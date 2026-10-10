# AI 医疗助手 · ai-medical

> 基于 **Spring Boot 3 + LangChain4j + 通义千问 + Pinecone** 的智能医疗问答与预约挂号系统。
> 实现完整的 RAG 链路：查询重写 → 向量检索 → 历史压缩 → 工具调用 → 流式输出 → 记忆持久化。

<p align="left">
  <img alt="Java" src="https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.2.6-6DB33F?logo=springboot&logoColor=white">
  <img alt="LangChain4j" src="https://img.shields.io/badge/LangChain4j-1.0.0--beta3-blue">
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-green">
</p>

---

## 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 接口](#api-接口)
- [核心流程解析](#核心流程解析)
- [数据库](#数据库)
- [常见问题](#常见问题)
- [许可证](#许可证)

---

## 项目简介

本项目模拟"北京协和医院"的智能客服与医疗伴诊助手，面向两类真实场景：

1. **AI 医疗问答** —— 基于医院私有知识库（医院信息、科室信息、神经内科等）做检索增强生成（RAG），
   回答就医流程、科室选择、疾病科普等问题。
2. **AI 挂号助手** —— 通过 Function Calling 让大模型自主调用后端工具，
   完成 **查询号源 → 预约挂号 → 取消预约** 的完整闭环。

与传统"把知识硬塞进 Prompt"的做法不同，本项目实现了贴近生产环境的 RAG 链路，
并对**上下文长度控制**、**会话隔离与持久化**、**工具自主决策**等工程问题给出了具体处理。

---

## 功能特性

| 特性 | 说明 |
| :--- | :--- |
| 🔍 **完整 RAG 链路** | 查询重写（Query Rewriting）+ 向量检索（Pinecone）+ 多查询扩展（Multi-Query）|
| 💬 **多轮会话记忆** | `MessageWindowChatMemory` 滑动窗口 + MongoDB 持久化，按 `memoryId` 隔离用户 |
| 🧠 **上下文压缩** | 用小模型将历史对话摘要至 100 字内，避免超出上下文窗口 |
| 🛠️ **Function Calling** | 三个内置工具：号源查询/预约、PDF 生成、联网搜索，由模型自主决策调用 |
| ⚡ **流式输出** | 基于 `Flux<String>` + WebFlux 的增量返回，前端实时渲染 |
| 🖼️ **OCR 知识入库** | 用 Tesseract 识别医院图片文字，与 Markdown 一起向量化入库 |
| 📄 **PDF 生成** | iText 生成支持中文字体的 PDF（内置 STSongStd-Light）|
| 🎨 **前端会话界面** | Vue 3 + Element Plus，支持流式打字效果、响应式布局、多会话切换 |
| 📚 **接口文档** | 集成 Knife4j，启动后访问 `/doc.html` |

---

## 技术栈

**后端**

| 组件 | 版本 | 用途 |
| :--- | :--- | :--- |
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.6 | 应用框架 |
| LangChain4j | 1.0.0-beta3 | LLM 应用开发框架 |
| 通义千问 `qwen-plus` | — | 对话模型（阿里云百炼 DashScope）|
| 通义千问 `text-embedding-v3` | — | 向量化模型 |
| Pinecone | — | 向量数据库（RAG 检索）|
| MongoDB | — | 会话记忆持久化 |
| MySQL + MyBatis-Plus | 3.5.11 | 预约挂号数据 |
| Tess4J | 5.8.0 | OCR 图像文字识别 |
| iText | 9.1.0 | PDF 生成 |
| Hutool | 5.8.37 | 工具库 |
| Knife4j | 4.3.0 | 接口文档 |

**前端**：Vue 3.5 · Vite 5 · Element Plus 2.8 · Axios · UUID

---

## 项目结构

```text
ai-medical/
├── pom.xml                      # Maven 配置（独立项目，无父 POM 依赖）
├── README.md
├── LICENSE
├── CHANGELOG.md
├── CONTRIBUTING.md
├── .gitignore
├── .gitattributes
├── .env.example                 # 环境变量模板
│
├── sql/
│   └── schema.sql               # MySQL 建库建表脚本
│
├── docs/
│   ├── ARCHITECTURE.md          # 架构与请求链路详解
│   └── TESSDATA.md              # Tesseract 语言包获取说明
│
├── src/main/java/org/example/
│   ├── AiMedical.java                    # Spring Boot 启动类
│   ├── assistant/                        # AiService 声明式接口
│   │   ├── Assistant.java                #   记忆 + 检索 + 工具 + 流式
│   │   └── ToolsCallingAssistant.java    #   仅工具 + 流式
│   ├── config/                           # Bean 装配
│   │   ├── ChatMemoryConfig.java         #   会话记忆窗口
│   │   ├── EmbeddingStoreConfig.java     #   Pinecone 向量库
│   │   ├── RetrieverConfig.java          #   内容检索器（topK / minScore）
│   │   ├── ExpandingQueryTransformerConfig.java
│   │   └── OcrConfig.java                #   Tesseract
│   ├── controller/
│   │   └── AiController.java             #   /medical/chat、/medical/rag
│   ├── query/
│   │   ├── QueryRewriter.java            #   查询重写
│   │   └── MultiQueryEnhancer.java       #   多查询扩展
│   ├── service/
│   │   ├── RagService.java
│   │   ├── AppointmentService.java
│   │   └── impl/
│   │       ├── RagServiceImpl.java       #   RAG 全链路编排
│   │       └── AppointmentServiceImpl.java
│   ├── store/
│   │   └── MongoChatMemoryStore.java     #   记忆持久化
│   ├── tools/                            # Function Calling 工具集
│   │   ├── AppointmentTools.java         #   号源查询 / 预约 / 取消
│   │   ├── PdfGenerationTool.java        #   PDF 生成
│   │   └── WebSearchTool.java            #   联网搜索
│   ├── mapper/AppointmentMapper.java
│   └── bean/                             # 实体与传输对象
│       ├── Appointment.java
│       ├── ChatForm.java
│       └── Message.java
│
├── src/main/resources/
│   ├── application.properties
│   ├── medical-prompt-template.txt       # 系统提示词
│   ├── knowledge/                        # 知识库（Markdown + 图片）
│   │   ├── 医院信息.md
│   │   ├── 科室信息.md
│   │   ├── 神经内科.md
│   │   └── hospital.png
│   ├── mapper/AppointmentMapper.xml
│   └── tessdata/                         # Tesseract 语言包（需自行下载）
│
├── src/test/java/org/example/
│   ├── PineconeEmbeddingStoreTest.java   # 知识库入库 / 向量检索
│   └── RagServiceTest.java               # RAG 链路联调
│
└── frontend/                             # Vue 3 前端
    ├── index.html
    ├── vite.config.js                    # 含后端代理配置
    ├── package.json
    └── src/
        ├── main.js
        ├── App.vue
        ├── assets/logo.png
        └── components/ChatWindow.vue
```

---

## 环境要求

| 组件 | 版本 | 用途 | 是否必需 |
| :--- | :--- | :--- | :---: |
| JDK | 17+ | 运行后端 | ✅ |
| Maven | 3.9+ | 构建项目 | ✅ |
| Node.js | 18+ | 运行前端 | 前端需要 |
| MySQL | 8.0+ | 预约挂号数据 | ✅ |
| MongoDB | 5.0+ | 会话记忆持久化 | ✅ |
| Tesseract 语言包 | `chi_sim` + `eng` | OCR 识别 | 用到 OCR 时 |

> **需要的外部 API 密钥**（均可免费注册获取）：
> - `DASH_SCOPE_API_KEY` — [阿里云百炼](https://bailian.console.aliyun.com/) 控制台申请
> - `PINECONE_API_KEY` — [Pinecone](https://app.pinecone.io/) 控制台申请
> - `GOOGLE_SEARCH_API_KEY` — [SerpApi](https://serpapi.com/) 申请（仅联网搜索需要）

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/<your-name>/ai-medical.git
cd ai-medical
```

### 2. 准备数据库

```bash
# MySQL：建库建表
mysql -u root -p < sql/schema.sql

# MongoDB：启动服务即可，集合会自动创建
mongod --dbpath /your/data/path
```

### 3. 配置环境变量

```bash
cp .env.example .env      # Windows: copy .env.example .env
```

**Linux / macOS**

```bash
export DASH_SCOPE_API_KEY="sk-xxxxxxxx"
export PINECONE_API_KEY="xxxxxxxx-xxxx-xxxx"
export GOOGLE_SEARCH_API_KEY="xxxxxxxx"
export MYSQL_PASSWORD="你的数据库密码"
```

**Windows PowerShell**

```powershell
$env:DASH_SCOPE_API_KEY="sk-xxxxxxxx"
$env:PINECONE_API_KEY="xxxxxxxx-xxxx-xxxx"
$env:GOOGLE_SEARCH_API_KEY="xxxxxxxx"
$env:MYSQL_PASSWORD="你的数据库密码"
```

> 在 IntelliJ IDEA 中，也可通过 `Run → Edit Configurations → Environment variables` 统一配置。

### 4. 下载 Tesseract 语言包

`tessdata/*.traineddata` **未纳入版本控制**（体积过大），需手动放置：

```bash
cd src/main/resources/tessdata

# tessdata_fast 版本（推荐，速度快、体积小）
curl -LO https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata
curl -LO https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata
```

预期得到 `chi_sim.traineddata`（约 42 MB）与 `eng.traineddata`（约 22 MB）。
更多方式（国内 CDN、apt 安装）见 [`docs/TESSDATA.md`](docs/TESSDATA.md)。

> 若不需要 OCR，可跳过此步，其余功能不受影响。

### 5. 构建并启动后端

```bash
# 编译（跳过依赖外部服务的测试）
mvn clean install -DskipTests

# 启动
mvn spring-boot:run
```

启动成功后：

- 服务地址：<http://localhost:8081>
- 接口文档：<http://localhost:8081/doc.html>

### 6. 初始化向量知识库（首次必做）

RAG 依赖 Pinecone 中的知识向量，需先执行入库：

```bash
mvn test -Dtest=PineconeEmbeddingStoreTest#testUploadKnowledge
```

该测试会读取 `src/main/resources/knowledge/` 下的 Markdown 文档，
经 OCR（如有图片）→ 向量化 → 写入 Pinecone。

### 7. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 <http://localhost:5173> 即可开始对话。

---

## 配置说明

所有配置项均已**外部化**，支持通过环境变量覆盖，避免密钥进入代码仓库。

| 配置项 | 环境变量 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `langchain4j...chat-model.api-key` | `DASH_SCOPE_API_KEY` | — | 通义千问 API Key（**必填**）|
| `spring.datasource.url` | `MYSQL_URL` | `jdbc:mysql://localhost:3306/medical?...` | MySQL 连接串 |
| `spring.datasource.username` | `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `spring.datasource.password` | `MYSQL_PASSWORD` | `123456` | MySQL 密码 |
| `spring.data.mongodb.uri` | `MONGODB_URI` | `mongodb://localhost:27017/chat_memory_db` | MongoDB 连接串 |
| `pinecone.index-name` | — | `medical-index1` | Pinecone 索引名 |
| `pinecone.namespace` | — | `medical-namespace1` | Pinecone 命名空间 |
| `pinecone.cloud` / `pinecone.region` | — | `AWS` / `us-east-1` | 索引部署位置 |
| `pdf.output-dir` | `PDF_OUTPUT_DIR` | `./output/pdf` | PDF 输出目录 |
| `tesseract.language` | — | `chi_sim+eng` | OCR 识别语言 |
| `tesseract.datapath` | — | `classpath:tessdata/` | OCR 语言包路径 |
| `server.port` | — | `8081` | 服务端口 |

> `PINECONE_API_KEY` 与 `GOOGLE_SEARCH_API_KEY` 直接通过 `System.getenv()` 读取，不经过配置文件。

---

## API 接口

两个核心接口均为**流式返回**（`text/stream`）。

### 1. 智能问答（带工具调用）

```http
POST /medical/chat
Content-Type: application/json

{
  "memoryId": 1001,
  "message": "帮我挂一个神经内科下周一上午的号，我叫张三，身份证 110101199001011234"
}
```

- `memoryId`：会话 ID，相同 ID 共享上下文记忆
- 响应：`Flux<String>` 流式文本
- 能力：自动调用 `AppointmentTools` 完成查询号源 → 预约 → 返回结果

### 2. RAG 检索增强问答

```http
POST /medical/rag
Content-Type: application/json

{
  "memoryId": 1001,
  "message": "协和医院神经内科有哪些专家？"
}
```

- 响应：`Flux<String>` 流式文本
- 链路：查询重写 → Pinecone 检索 → 历史摘要 → 组装 Prompt → 流式生成

### 命令行验证

```bash
curl -N -X POST http://localhost:8081/medical/rag \
  -H "Content-Type: application/json" \
  -d "{\"memoryId\":1001,\"message\":\"协和医院门诊时间是几点？\"}"
```

---

## 核心流程解析

`/medical/rag` 是项目的技术核心，完整链路见 `RagServiceImpl#executeRag`：

```text
用户提问
   │
   ├─ 1. 查询重写 QueryRewriter.rewrite()
   │     修正错别字、补全信息、口语转书面语
   │
   ├─ 2. 向量检索 pineconeRetriever.retrieve()
   │     maxResults=1, minScore=0.5
   │
   ├─ 3. 对话历史摘要
   │     取出 MongoDB 中的历史 → qwen 压缩到 100 字以内，防止上下文超长
   │
   ├─ 4. 组装 Prompt
   │     用户提示词 + 检索上下文 + 对话历史摘要
   │
   ├─ 5. 交给 ToolsCallingAssistant 流式生成
   │     模型可自主调用：号源查询 / 预约 / 取消 / PDF / 联网搜索
   │
   └─ 6. doOnComplete 回写记忆
         将用户消息与完整 AI 回复写回 MongoChatMemoryStore
```

### 工具调用（Function Calling）

LangChain4j 会自动读取 `@Tool` 注解生成函数描述，连同 `@P` 参数说明发送给模型，
由模型决定是否调用、调用哪个工具、传什么参数。以挂号为例：

```text
模型调用 AppointmentTools.queryDepartment(科室, 日期, 时间, 医生)
  └─ 返回是否有号源

模型调用 AppointmentTools.bookAppointment(Appointment)
  └─ AppointmentServiceImpl 查重 → MyBatis-Plus 落库 MySQL
```

系统提示词 `medical-prompt-template.txt` 中约束了必填信息（姓名、身份证、科室、日期、时段），
并规定信息齐全时**直接完成挂号**，无需二次确认。

更详细的设计说明见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)。

---

## 数据库

### MySQL — 预约挂号

表结构定义见 [`sql/schema.sql`](sql/schema.sql)，核心表为 `appointment`：

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | BIGINT | 主键，自增 |
| `username` | VARCHAR(50) | 就诊人姓名 |
| `id_card` | VARCHAR(18) | 身份证号 |
| `department` | VARCHAR(50) | 预约科室 |
| `date` | VARCHAR(20) | 预约日期（`2025-04-14`）|
| `time` | VARCHAR(10) | 时段（`上午` / `下午`）|
| `doctor_name` | VARCHAR(50) | 预约医生（可空）|

### MongoDB — 会话记忆

由 `MongoChatMemoryStore` 自动维护，无需手工建集合：

- 数据库：`chat_memory_db`
- 集合：`chat_message`（由 `@Document("chat_message")` 指定）
- 文档：`{ memoryId, content }`，`content` 为 LangChain4j 序列化后的消息 JSON

---

## 常见问题

<details>
<summary><b>启动报错：找不到 <code>tessdata</code> 或 <code>chi_sim.traineddata</code></b></summary>

语言包未下载。请按 [`docs/TESSDATA.md`](docs/TESSDATA.md) 放入 `src/main/resources/tessdata/`。
若不需要 OCR，可忽略该错误（仅影响 OCR 相关调用）。
</details>

<details>
<summary><b>启动报错：<code>环境变量 PINECONE_API_KEY 未设置</code></b></summary>

未配置 Pinecone 密钥。请在 [Pinecone 控制台](https://app.pinecone.io/) 注册获取后设置：

```bash
export PINECONE_API_KEY="your-key"
```
</details>

<details>
<summary><b>接口返回空内容 / 检索不到知识</b></summary>

通常是向量库尚未初始化。请先执行入库：

```bash
mvn test -Dtest=PineconeEmbeddingStoreTest#testUploadKnowledge
```

另 `RetrieverConfig` 中 `minScore=0.5` 是相似度门槛，过低匹配会被过滤，可按知识库质量调整。
</details>

<details>
<summary><b>启动失败 / 大量依赖注入报错</b></summary>

本应用启动时即需要连接 MongoDB、MySQL 与 Pinecone（`EmbeddingStoreConfig` 为启动期 Bean）。
请确认三者均已就绪、`DASH_SCOPE_API_KEY` 与 `PINECONE_API_KEY` 已设置。
</details>

<details>
<summary><b>前端请求跨域 / 连接失败</b></summary>

`frontend/vite.config.js` 中代理目标为 `http://localhost:8081`。
请确认后端已在 **8081** 端口启动（对应 `server.port=8081`）。
</details>

<details>
<summary><b>MySQL 连接失败 / 时区报错</b></summary>

确认 MySQL 已启动且执行过 `sql/schema.sql`。连接串已内置
`serverTimezone=Asia/Shanghai&useSSL=false`，若使用 MySQL 5.7 请适当调整驱动参数。
</details>

---

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

> ⚠️ **免责声明**：本项目仅用于技术学习与研究，所提供的医疗建议**不构成任何专业医疗诊断或治疗意见**。
> 任何实际就医决策请咨询执业医师。请勿将本项目直接部署为面向公众的医疗服务。

---

## 致谢

- [LangChain4j](https://github.com/langchain4j/langchain4j) — Java 生态 LLM 应用开发框架
- [阿里云百炼 DashScope](https://bailian.console.aliyun.com/) — 通义千问模型服务
- [Pinecone](https://www.pinecone.io/) — 向量数据库
- [Tess4J](http://tess4j.sourceforge.net/) — Java OCR 封装
