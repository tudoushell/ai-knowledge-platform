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

页面会先尝试恢复登录状态；没有有效会话时显示登录页。使用数据库中已有且已启用的用户账号登录，当前页面不提供注册。Flyway 的 V11 迁移会初始化 `admin` 管理员，初始密码使用初始化时提供的值；SQL 仅保存 BCrypt 哈希。已有同名账号时不会重置密码或提升权限。

本地使用 HTTP 时，为后端设置 `REFRESH_COOKIE_SECURE=false`；生产环境使用 HTTPS 并保持 `true`。Refresh Token 通过 HttpOnly Cookie 保存，浏览器自动收发，前端不会读取或存储它。Access Token 只放在内存，不写入 localStorage 或 sessionStorage。

登录、刷新、退出请求携带 `X-Requested-With: XMLHttpRequest` 和 Cookie；其他 API、文件上传及 SSE 请求自动携带 `Authorization: Bearer …`。Access Token 即将过期或接口返回 401 时，同一页面共享一次刷新；只在流开始之前重试认证失败的 SSE 请求，不重放已接收的回答。退出成功或刷新令牌失效时卸载工作台，停止轮询与流式请求，清理页面内的数据。

默认使用同源 `/api` 代理。若改为浏览器直接跨源访问，后端需要配置明确的可信 CORS 来源、允许凭据及保护请求头；不要为携带凭据的请求开放任意来源。

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
├── App.vue                       # 登录状态、会话恢复和页面切换
├── Workspace.vue                 # 工作台、知识库侧栏、通知和退出入口
├── components/
│   ├── LoginPage.vue             # 登录表单、密码显示和错误反馈
│   ├── DocumentsPanel.vue        # 上传、处理、文档列表
│   ├── DocumentInspector.vue     # 文件详情、Chunk 和任务历史分页
│   ├── SearchPanel.vue           # 向量检索、关键词检索
│   ├── ChatPanel.vue             # 模型选择、SSE 问答、引用与用量
│   ├── HitCard.vue               # 检索命中、引用上下文
│   ├── HighlightedText.vue        # 安全的关键词高亮
│   └── MarkdownContent.vue       # 回答 Markdown 展示
├── composables/useWorkspace.js   # 知识库、文档状态和任务轮询
├── services/api.js               # REST 请求、SSE 流解析
├── services/auth.js              # Cookie 认证、内存令牌和共享刷新请求
├── utils/                        # 格式化、原有 Markdown 子集渲染器
└── assets/app.css                # 原工作台样式
```

保留 `atlas-active-library`、`atlas-chat-model` 和 `atlas-process-task:*` 的本地存储键。浏览器存储按访问源隔离，从后端端口切换至 `5173` 后需要重新选择知识库和模型。

文档上传后仍需点击“开始完整处理”。删除和重新处理保留原有确认提示。切换知识库时会清空检索及回答，并中断当前前端请求；后端已创建的文档任务仍会继续执行。

知识问答按轮次保留当前页面的提问和回答，后续发送会携带服务端返回的会话 ID。“新建会话”会清空本地记录，并在下次发送时创建新会话，不删除数据库中的历史消息。刷新页面后不会自动恢复会话。

生成期间可编辑下一条问题，但不能重复发送；可点击“停止生成”中断接收并保留已显示内容。停止不保证服务端撤销本轮处理或消息保存。发生错误后可将原问题填回输入框编辑再发送，不自动重试，避免重复写入消息。检索设置仅用于 RAG 回答，工具查询不使用这些参数。
