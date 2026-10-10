# 贡献指南

感谢你对本项目的关注！本文档说明如何参与贡献。

---

## 开始之前

提交改动时请留意：**不要把密钥、本机绝对路径、构建产物提交上来**。

---

## 开发环境准备

| 组件 | 版本 |
| :--- | :--- |
| JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 18+（前端） |
| MySQL | 8.0+ |
| MongoDB | 5.0+ |

配置密钥请参考 [`.env.example`](.env.example) 与 README 的「快速开始」章节。

---

## 分支与提交规范

### 分支命名

```text
feature/简短描述     新功能
fix/简短描述         缺陷修复
docs/简短描述        文档改动
refactor/简短描述    重构
```

### 提交信息

采用 [Conventional Commits](https://www.conventionalcommits.org/) 风格：

```text
<type>(<scope>): <简短描述>

<可选正文>
```

常用 `type`：`feat`、`fix`、`docs`、`style`、`refactor`、`test`、`chore`。

示例：

```text
feat(rag): 新增多查询扩展的 LLM 生成策略
fix(appointment): 修正预约查重条件漏掉日期字段的问题
docs: 补充 Tesseract 语言包下载说明
```

`scope` 建议使用对应层次，如 `rag`、`chat`、`memory`、`appointment`、`ocr`、`pdf`、`frontend`、`docs`。

---

## 提交前检查清单

- [ ] `mvn clean install -DskipTests` 编译通过
- [ ] 前端改动已通过 `npm run build`
- [ ] 未提交任何 API Key、密码、令牌
- [ ] 未提交本机绝对路径（如 `D:/file/...`、`C:/Users/...`）
- [ ] 未提交 `target/`、`node_modules/`、`dist/`、`*.traineddata`
- [ ] 新增配置项已加入 `.env.example` 并在 README 配置表中说明
- [ ] 新增或修改的代码带有必要的中文注释（与本项目风格保持一致）

> 快速自查是否误提交大文件：
>
> ```bash
> git ls-files -s | awk '$4 ~ /traineddata|\.jar$|\.zip$/ {print}'
> ```

---

## 代码风格

### 后端

- 遵循现有分层目录：`assistant` / `config` / `controller` / `query` / `service` / `store` / `tools` / `bean`
- 使用 Lombok（`@Data`、`@Slf4j`）减少样板代码
- 配置项一律外部化，通过 `@Value("${key:default}")` 或环境变量读取，
  **禁止硬编码**绝对路径与密钥
- 新增 Function Calling 工具时，`@Tool` 的 `name` 与 `value` 需写清用途与参数含义，
  这直接影响模型的调用准确率
- 公开方法补充 Javadoc 或中文行内注释

### 前端

- Vue 3 `<script setup>` 组合式 API
- 组件放在 `src/components/`，样式使用 `<style scoped>`
- 保持现有响应式断点（768px）与 Element Plus 组件用法

---

## 提交 Pull Request

1. Fork 本仓库并从 `main` 切出特性分支
2. 完成改动，确保通过上述检查清单
3. 提交 PR，描述中说明：
   - **改动目的**：解决什么问题
   - **实现方式**：关键设计取舍
   - **验证方式**：如何测试、测试了什么
4. 若 PR 涉及界面改动，请附上截图

---

## 报告问题

提交 Issue 时请包含：

- 环境信息（JDK / Maven / 操作系统 / MySQL / MongoDB 版本）
- 复现步骤
- 期望行为与实际行为
- 关键日志或异常堆栈（**请先脱敏，去掉密钥**）

> 提示：`EmbeddingStoreConfig` 是启动期 Bean，缺少 `PINECONE_API_KEY`
> 时应用会直接启动失败，这属于预期行为，报错时请先确认环境变量。

---

## 许可证

贡献的代码将按本项目的 [MIT License](LICENSE) 授权。
