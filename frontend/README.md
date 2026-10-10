# AI 医疗助手 · 前端 (frontend)

后端配套的 Vue 3 单页应用，提供流式对话界面。

---

## 技术栈

| 依赖 | 版本 | 说明 |
| :--- | :--- | :--- |
| Vue | ^3.5.13 | 组合式 API（`<script setup>`）|
| Vite | ^5.4.8 | 构建工具与开发服务器 |
| Element Plus | ^2.8.4 | UI 组件库 |
| Axios | ^1.7.7 | HTTP 客户端（流式请求）|
| uuid | ^10.0.0 | 生成会话 ID |

界面图标通过 CDN 引入 Font Awesome 6.7.2（见 `index.html`）。

---

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 生产构建，产物输出到 dist/
npm run build

# 本地预览构建产物
npm run preview
```

> ⚠️ **启动前端前请先启动后端**，且后端必须监听 **8081** 端口
> （`src/main/resources/application.properties` 中 `server.port=8081`）。

---

## 与后端的对接

### 代理配置

`vite.config.js` 中已配置开发代理，避免跨域问题：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, ''),   // 去掉 /api 前缀
    },
  },
}
```

因此前端请求 `/api/medical/rag` 会被转发到 `http://localhost:8081/medical/rag`。

### 调用的接口

| 前端调用 | 实际后端接口 | 说明 |
| :--- | :--- | :--- |
| `POST /api/medical/rag` | `POST /medical/rag` | 当前启用（`ChatWindow.vue`）|
| `POST /api/medical/chat` | `POST /medical/chat` | 已注释，可切换为带工具调用的对话 |

请求体：

```json
{ "memoryId": 123456, "message": "协和医院门诊时间是几点？" }
```

响应为流式文本，前端通过 Axios 的 `onDownloadProgress` 增量拼接渲染。

---

## 会话标识（memoryId）

`ChatWindow.vue` 中的 `initMemoryId()` 逻辑：

1. 从 `localStorage` 读取键 `key`；
2. 不存在时生成一个 UUID，取其**前 6 个十六进制字符**转换为十进制数字（`uuidToMemoryId`），
   确保落在 6 位数以内；
3. 写回 `localStorage`，保证刷新页面后仍延续同一会话；
4. 点击「新会话」会清除该键并刷新页面，从而开启全新上下文。

> 说明：该设计是为了让 `memoryId` 保持数值型（后端 `ChatForm.memoryId` 为 `Long`）。
> 理论上存在极小概率的 ID 碰撞，生产环境建议改用数据库自增 ID 或雪花算法。

---

## 目录结构

```text
frontend/
├── index.html                  # 入口 HTML
├── vite.config.js              # Vite 配置（含后端代理）
├── package.json
├── .gitignore
├── .vscode/
│   └── extensions.json         # 推荐插件：Vue.volar
└── src/
    ├── main.js                 # 应用入口，注册 Element Plus
    ├── App.vue                 # 根组件
    ├── assets/
    │   └── logo.png            # 侧边栏 Logo
    └── components/
        └── ChatWindow.vue      # 对话窗口（核心组件）
```

---

## 界面说明

- **左侧栏**：Logo、「新会话」按钮；窄屏（≤768px）下自动变为顶部横向布局
- **消息区**：用户消息右对齐（浅蓝）、机器人消息左对齐（浅绿），
  加载中显示三点脉冲动画
- **输入区**：回车或点击「发送」提交；请求进行中禁用发送按钮
- **流式渲染**：机器人气泡内容随 `onDownloadProgress` 回调实时增长，
  并自动滚动到底部

---

## 已知问题

| 问题 | 位置 | 说明 |
| :--- | :--- | :--- |
| `v-html` 渲染存在 XSS 风险 | `ChatWindow.vue` 消息渲染 | 当前直接使用 `v-html`，虽定义了 `convertStreamOutput` 做转义但**未被调用**；接入不可信内容前应启用转义或改用 `textContent` |
| `convertStreamOutput` 为死代码 | `ChatWindow.vue` | 定义后未使用，换行符未做 `<br>` 转换 |

---

## 相关文档

- 项目总览：[`../README.md`](../README.md)
- 架构与请求链路：[`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md)
