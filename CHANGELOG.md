# 更新日志

本项目的所有重要改动都会记录在此文件中。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.0.0] - 2026-10-10

首个正式发布版本。提供完整的智能医疗问答与预约挂号能力。

### 新增

#### 核心能力

- **RAG 检索增强问答**（`POST /medical/rag`）
  - 查询重写：修正错别字、补全信息、口语转书面语（`QueryRewriter`）
  - 向量检索：Pinecone + `text-embedding-v3`，`maxResults=1`、`minScore=0.5`
  - 历史压缩：将多轮对话摘要至 100 字内，避免超出上下文窗口
  - 流式输出：基于 `Flux<String>` 的增量返回

- **Function Calling 工具链**
  - `AppointmentTools`：查询号源、预约挂号、取消预约
  - `PdfGenerationTool`：生成支持中文字体的 PDF
  - `WebSearchTool`：SerpApi 联网搜索

- **多轮会话记忆**
  - `MessageWindowChatMemory` 滑动窗口（20 条）
  - `MongoChatMemoryStore` 持久化到 MongoDB
  - 按 `memoryId` 实现多用户会话隔离

- **知识库入库**
  - 支持 Markdown 文档向量化
  - 支持 Tesseract OCR 提取医院图片文字后入库
  - 提供 `PineconeEmbeddingStoreTest` 完成离线索引构建

#### 前端

- Vue 3 + Element Plus 流式对话界面（`frontend/`）
- 打字动画、自动滚动、响应式布局、多会话切换
- Vite 开发代理，规避跨域

#### 工程化

- 独立的 Maven 配置，克隆后可直接构建（无父 POM 依赖）
- `.env.example` 环境变量模板，明确各密钥申请入口
- `sql/schema.sql` MySQL 建库建表脚本
- `.gitignore` / `.gitattributes` 规范版本控制
- Knife4j 接口文档集成
- 完整文档：`README.md`、`docs/ARCHITECTURE.md`、`docs/TESSDATA.md`、`CONTRIBUTING.md`

### 说明

- 所有连接信息与密钥均已外部化，支持环境变量覆盖，默认值适配本地开发
- PDF 输出目录默认为 `./output/pdf`，不依赖开发者本机绝对路径
- Tesseract 语言包因体积较大未纳入版本控制，参见 `docs/TESSDATA.md`

### 已知问题

详见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) 第 8 节，主要包括：

- `bean/Message.java` 的 `@Id` 注解导入了 `nonapi.io.github.classgraph.json.Id`，
  应为 `org.springframework.data.annotation.Id`
- `Message.messageId` 字段与查询条件所使用的 `memoryId` 字段名不一致
- `MongoChatMemoryStore#deleteMessages` 删除时传入的实体类不匹配
- `WebSearchTool` 使用 `static final` 读取环境变量，不支持运行时变更
- `MultiQueryEnhancer` 已实现但未接入 RAG 链路
- 现有测试均为依赖外部服务的集成测试，缺少 Mock 化的单元测试
- 前端 `v-html` 渲染存在 XSS 风险，转义函数定义后未启用
