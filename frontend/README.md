# Atlas Vue 前端

从 `rag-service/src/main/resources/static` 迁移的独立 Vue 3 + Vite 项目，保留原工作台的界面与功能。前端依赖和构建由 npm 管理，后端继续由 Maven 管理。

## 本地运行

需要 Node.js `^20.19.0 || >=22.12.0` 和 npm。先按根目录 README 启动后端（默认 `http://localhost:8090`），再运行：

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`。所有 `/api` 请求（含上传、任务轮询和 SSE）通过 Vite 代理到后端，无需为开发环境开放 CORS。

后端端口或地址不同时：

```bash
cp .env.example .env
# 将 .env 内的 API_PROXY_TARGET 改为实际后端地址
npm run dev
```

修改代理配置后需重启 Vite。`API_PROXY_TARGET` 只供开发和本地预览服务器使用，不会写入浏览器产物。不要在前端环境变量中存放模型 API Key。

## 构建与验证

```bash
npm test
npm run build
npm run preview
```

构建产物为 `dist/`，本地预览地址为 `http://localhost:4173`，同样代理到配置的后端。`preview` 用于检查构建产物。

生产部署时，将 `dist/` 部署到静态服务器，并把同源 `/api/` 转发到 RAG 服务；SSE 路径需关闭响应缓冲。可参考 [nginx.conf.example](nginx.conf.example)。前端构建不会自动写回 Spring Boot 的静态资源目录，也不参与 Maven 打包。

## 代码结构

```text
src/
├── App.vue                       # 工作台、知识库侧栏、通知
├── components/
│   ├── DocumentsPanel.vue        # 上传、处理、文档列表
│   ├── DocumentInspector.vue     # 文件详情、Chunk 和任务历史分页
│   ├── SearchPanel.vue           # 向量检索、关键词检索
│   ├── ChatPanel.vue             # 模型选择、SSE 问答、引用与用量
│   ├── HitCard.vue               # 检索命中、引用上下文
│   ├── HighlightedText.vue        # 安全的关键词高亮
│   └── MarkdownContent.vue       # 回答 Markdown 展示
├── composables/useWorkspace.js   # 知识库、文档状态和任务轮询
├── services/api.js               # REST 请求、SSE 流解析
├── utils/                        # 格式化、原有 Markdown 子集渲染器
└── assets/app.css                # 原工作台样式
```

保留 `atlas-active-library`、`atlas-chat-model` 和 `atlas-process-task:*` 的本地存储键。浏览器存储按访问源隔离，从后端端口切换至 `5173` 后需要重新选择知识库和模型。

文档上传后仍需点击“开始完整处理”。删除和重新处理保留原有确认提示。切换知识库时会清空检索及回答，并中断当前前端请求；后端已创建的文档任务仍会继续执行。
