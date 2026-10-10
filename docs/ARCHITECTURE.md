# 架构与请求链路详解

本文档说明项目的整体架构、核心组件职责与端到端请求链路。

---

## 1. 分层架构

```text
┌──────────────────────────────────────────────────────────────┐
│                      前端 (Vue 3 + Vite)                      │
│              ChatWindow.vue  ──axios stream──►  /api/*        │
└───────────────────────────┬──────────────────────────────────┘
                            │  Vite 代理 (去掉 /api 前缀)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                    控制层  AiController                       │
│         POST /medical/chat          POST /medical/rag         │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  应用服务层                                                   │
│    RagService / RagServiceImpl   ← 编排 RAG 全链路            │
│    QueryRewriter                 ← 查询重写                   │
│    MultiQueryEnhancer            ← 多查询扩展                 │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  AI 服务层 (LangChain4j AiService 声明式接口)                 │
│    Assistant            : 记忆 + 检索 + 工具 + 流式           │
│    ToolsCallingAssistant: 记忆无关 + 工具 + 流式              │
└───────────┬───────────────────────────┬──────────────────────┘
            ▼                           ▼
┌───────────────────────┐   ┌──────────────────────────────────┐
│  ChatMemoryProvider   │   │   ContentRetriever               │
│  MessageWindow(20)    │   │   EmbeddingStoreContentRetriever │
│  + MongoChatMemoryStore│  │   maxResults=1, minScore=0.5     │
└───────────┬───────────┘   └──────────────┬───────────────────┘
            ▼                              ▼
┌───────────────────────┐   ┌──────────────────────────────────┐
│      MongoDB          │   │   Pinecone 向量库                │
│  chat_memory_db       │   │   medical-index1                 │
│  .chat_message        │   │   (text-embedding-v3)            │
└───────────────────────┘   └──────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  工具层 (Function Calling)                                    │
│    AppointmentTools  : 号源查询 / 预约 / 取消                 │
│    PdfGenerationTool : 生成中文 PDF                           │
│    WebSearchTool     : SerpApi 谷歌搜索                       │
└───────────────────────────┬──────────────────────────────────┘
                            ▼
                  MySQL (appointment 表)
```

---

## 2. 核心组件职责

| 组件 | 文件 | 职责 |
| :--- | :--- | :--- |
| `Assistant` | `assistant/Assistant.java` | 声明式 AiService：绑定流式模型、记忆、检索器与工具集 |
| `ToolsCallingAssistant` | `assistant/ToolsCallingAssistant.java` | 仅绑定工具与流式模型，用于 RAG 链路的最终生成 |
| `RagServiceImpl` | `service/impl/RagServiceImpl.java` | RAG 全链路编排：重写 → 检索 → 摘要 → 组装 → 流式 → 回写 |
| `AiController` | `controller/AiController.java` | 对外暴露 `/medical/chat` 与 `/medical/rag` 两个流式接口 |
| `QueryRewriter` | `query/QueryRewriter.java` | 调用 qwen 修正错别字、补全信息、口语转书面语 |
| `MultiQueryEnhancer` | `query/MultiQueryEnhancer.java` | 三种多查询策略：本地规则 / LangChain4j 转换器 / LLM 生成 |
| `ChatMemoryConfig` | `config/ChatMemoryConfig.java` | 滑动窗口记忆（20 条）+ MongoDB 持久化 + 按 ID 隔离 |
| `EmbeddingStoreConfig` | `config/EmbeddingStoreConfig.java` | 构建 Pinecone 无服务器索引连接 |
| `RetrieverConfig` | `config/RetrieverConfig.java` | 内容检索器，控制 `maxResults` 与 `minScore` |
| `ExpandingQueryTransformerConfig` | `config/ExpandingQueryTransformerConfig.java` | 查询扩展转换器，`n=3` |
| `OcrConfig` | `config/OcrConfig.java` | Tesseract 实例，读取 `tesseract.*` 配置 |
| `MongoChatMemoryStore` | `store/MongoChatMemoryStore.java` | `ChatMemoryStore` 的 MongoDB 实现 |
| `AppointmentTools` | `tools/AppointmentTools.java` | 三个 `@Tool`：查号源、预约、取消预约 |
| `PdfGenerationTool` | `tools/PdfGenerationTool.java` | `@Tool` 生成中文 PDF |
| `WebSearchTool` | `tools/WebSearchTool.java` | `@Tool` 联网搜索 |

---

## 3. 端到端链路：`POST /medical/rag`

```java
// AiController#rag
return ragService.executeRag(chatForm.getMemoryId(), chatForm.getMessage());
```

`RagServiceImpl#executeRag` 步骤：

### 步骤 1 —— 参数校验

```java
if (memoryId == null || StringUtils.isBlank(message)) return Flux.empty();
```

### 步骤 2 —— 查询重写

```java
String rewriteMessage = queryRewriter.rewrite(message);
```

将"为啥我电脑打开 Exceleee 特别卡"改写为书面查询，提升向量召回率。

### 步骤 3 —— 向量检索

```java
List<Content> contentList = pineconeRetriever.retrieve(new Query(rewriteMessage));
String retrieverContext = convertToString(contentList);
```

`pineconeRetriever` 配置为 `maxResults=1`、`minScore=0.5`，
低于阈值的匹配会被丢弃，避免无关知识污染上下文。

### 步骤 4 —— 对话历史摘要

```java
ChatMemory chatMemory = chatMemoryProvider.get(memoryId);
String chatHistory = ChatMessageSerializer.messagesToJson(chatMemory.messages());
chatMemory.add(UserMessage.userMessage(rewriteMessage));

// 压缩到 100 字以内，防止输入超长
String chatHistorySummary = qwenChatModel.chat("将这段对话进行总结，结果不得超过100字" + chatHistory);
```

这是**关键工程处理**：直接把全部历史拼进 Prompt 会迅速撑爆上下文窗口，
因此先用小模型把历史压缩成摘要。

### 步骤 5 —— 组装最终 Prompt

```java
String userMessage = String.format(RAG_PROMPT, rewriteMessage, retrieverContext, chatHistorySummary);
```

模板结构：`用户提示词 + 检索上下文 + 对话历史摘要`。

### 步骤 6 —— 流式生成 + 记忆回写

```java
Flux<String> aiMessageFlux = toolsCallingAssistant.chat(userMessage);
StringBuilder sb = new StringBuilder();
return aiMessageFlux
        .doOnNext(sb::append)                 // 边流式输出边累积
        .doOnComplete(() -> {                 // 流结束后回写完整回复
            if (chatMemory != null && !sb.isEmpty()) {
                chatMemory.add(AiMessage.from(sb.toString()));
            }
        });
```

这样既保证前端 SSE 实时渲染，又保证会话历史完整落库。

---

## 4. 工具调用（Function Calling）机制

LangChain4j 会自动读取 `@Tool` 注解的 `name` / `value` 生成函数描述，
连同 `@P` 参数说明一起发送给大模型。模型根据用户意图决定是否调用、
调用哪个工具、以及传什么参数。

以"挂号"为例：

```text
用户：帮我挂神经内科下周一上午的号，我叫张三，身份证 110101...
  │
  ├─ 模型决定调用 AppointmentTools.queryDepartment(科室, 日期, 时间, 医生)
  │     └─ 返回 true/false，模型据此告知用户是否有号
  │
  ├─ 模型向用户（或直接）确认预约信息
  │
  └─ 模型调用 AppointmentTools.bookAppointment(Appointment)
        └─ AppointmentServiceImpl 查重 → MyBatis-Plus 落库 MySQL
```

系统提示词 `medical-prompt-template.txt` 中明确约束了：
- 必填信息：姓名、身份证、科室、日期、时段（医生可选）
- 信息齐全时**直接完成挂号**，无需二次确认
- 联网搜索必须走 `WebSearchTool`，PDF 必须走 `PDFGenerationTool`

---

## 5. 会话记忆与隔离

```java
@Bean
public ChatMemoryProvider chatMemoryProvider() {
    return memoryId -> MessageWindowChatMemory.builder()
            .id(memoryId)
            .maxMessages(20)                        // 滑动窗口：只保留最近 20 条
            .chatMemoryStore(mongoChatMemoryStore)  // 持久化
            .build();
}
```

- **隔离**：前端为每个浏览器生成独立 `memoryId`（UUID 前 6 位转数字）并存入 `localStorage`
- **持久化**：`MongoChatMemoryStore` 实现 `getMessages` / `updateMessages` / `deleteMessages`
- **窗口**：超过 20 条时自动淘汰最早的消息，控制 Token 消耗

---

## 6. 知识库入库流程

```text
knowledge/*.md ─┐
                ├─► DocumentLoader ─► (可选) DocumentSplitter ─► EmbeddingModel ─► Pinecone
hospital.png ───┘        (Tesseract OCR 提取文字)
```

由 `PineconeEmbeddingStoreTest#testUploadKnowledge` 触发，
属于**离线阶段**操作，线上检索不再重复执行。

---

## 7. 配置与外部依赖

| 配置 | 所在类 | 说明 |
| :--- | :--- | :--- |
| `pinecone.index-name` / `namespace` / `cloud` / `region` | `EmbeddingStoreConfig` | 索引参数，可通过配置覆盖 |
| `PINECONE_API_KEY` | `EmbeddingStoreConfig` | 从环境变量读取，缺失时启动期抛错并给出提示 |
| `DASH_SCOPE_API_KEY` | `application.properties` | 对话与向量化模型共用 |
| `GOOGLE_SEARCH_API_KEY` | `WebSearchTool` | 联网搜索，从环境变量读取 |
| `pdf.output-dir` | `PdfGenerationTool` | 默认 `./output/pdf`，不依赖本机绝对路径 |
| `tesseract.*` | `OcrConfig` | 语言与语言包路径 |

> ⚠️ 注意：`EmbeddingStoreConfig` 是**启动期 Bean**，因此 Pinecone 不可用或密钥缺失时
> 应用会直接启动失败。这属于预期行为，便于尽早暴露配置问题。

---

## 8. 已知问题与改进方向

以下问题为保持原有行为**未做修改**，留作后续优化点：

| 问题 | 位置 | 影响 | 建议 |
| :--- | :--- | :--- | :--- |
| `@Id` 注解导错包 | `bean/Message.java` | 使用了 `nonapi.io.github.classgraph.json.Id` 而非 `org.springframework.data.annotation.Id`，ObjectId 映射可能不理想 | 改为 Spring Data 的 `@Id` |
| `messageId` 字段未被查询使用 | `bean/Message.java` / `MongoChatMemoryStore` | 查询条件用的是 `memoryId`，与字段名不一致 | 统一字段名 |
| `deleteMessages` 删除的实体类不匹配 | `MongoChatMemoryStore` | `mongoTemplate.remove(query, ChatMessage.class)` 应操作 `Message.class` | 修正实体类型 |
| `WebSearchTool` 静态初始化时机 | `tools/WebSearchTool.java` | `static final` 字段在类加载时读取环境变量，运行中改环境变量不生效 | 改为实例字段 + `@Value` |
| `WebSearchTool` 异常处理不完整 | `tools/WebSearchTool.java` | `parseResult` 未判空，接口异常时可能抛 NPE | 增加空值判断 |
| `MultiQueryEnhancer` 未被调用 | `query/MultiQueryEnhancer.java` | 已实现三种策略，但 `RagServiceImpl` 未接入 | 按需接入检索前扩展 |
| 无单元测试覆盖 | 全局 | 现有测试均为依赖外部服务的集成测试，CI 难以执行 | 补充 Mock 化的单元测试 |
| 前端 `v-html` 存在 XSS 风险 | `frontend/src/components/ChatWindow.vue` | 直接使用 `v-html`，已定义的 `convertStreamOutput` 转义函数未被调用 | 启用转义或改用 `textContent` |

---

## 9. 本地验证清单

由于外部依赖较重，建议按此顺序验证：

1. `mvn clean install -DskipTests` —— 编译通过
2. 启动 MongoDB 与 MySQL，执行 `sql/schema.sql`
3. 设置 `DASH_SCOPE_API_KEY`，运行 `RagServiceTest` 前先确认模型连通
4. 设置 `PINECONE_API_KEY`，运行 `PineconeEmbeddingStoreTest#testUploadKnowledge` 初始化向量库
5. `mvn spring-boot:run` 启动，`curl` 调用 `/medical/rag` 验证 RAG 链路
6. `cd frontend && npm run dev`，验证流式渲染效果
