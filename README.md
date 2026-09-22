# AI Knowledge Platform

基于 Java 17、Spring Boot 4.1.1、Spring AI 2.0.1、PostgreSQL / PGVector 和 Redis 的 RAG 知识平台。

支持文档入库、多查询混合检索、带会话历史的问题改写，以及同步和 SSE 流式知识问答。

## 当前功能

- 知识库：创建、列表和详情查询。
- 文档：上传 TXT、Markdown、PDF、DOCX，同一知识库内按文件内容去重；单次上传一个文件，默认最大 10 MB。
- 文档处理：异步执行解析、切块和向量索引，支持进度查询、任务历史、失败退避重试、超时任务恢复和重新处理。
- 文档管理：查询文档详情、分页查看 Chunk、手动切块与索引，以及删除文档和关联派生数据。
- 检索：向量召回与 PostgreSQL 关键词检索，单查询 RRF 融合、多查询按 Chunk ID 合并并再次 RRF 融合，最后进行 Rerank。
- 问答：支持查询改写、查询扩展、相邻 Chunk 上下文扩展、引用来源和模型返回的 Token 用量。
- 会话记忆：PostgreSQL 持久化用户与助手消息，Redis 缓存近期消息；按消息数量和字符数裁剪历史，并用于将追问改写为独立问题。
- 追踪信息：返回历史来源、缓存状态、历史窗口统计、问题改写结果和检索各阶段候选数量。

当前会话删除已在服务层实现，会同时清理历史消息并尝试使缓存失效；尚未提供独立的会话管理 HTTP 接口。

## 项目结构

```text
ai-knowledge-platform
├── ai-common                # 通用响应、异常处理、提示词和 JSON 工具
├── rag-service              # 可执行的 RAG 服务、REST/SSE 接口及数据库迁移
└── compose
    └── server-compose.yml   # PostgreSQL / PGVector 与 Redis
```

## 环境要求

- JDK 17+
- Docker 与 Docker Compose（使用项目提供的数据库、缓存配置时）
- PostgreSQL + `pgvector`；Compose 当前使用 PostgreSQL 18 / pgvector 0.8.2
- Redis；Compose 当前使用 `redis:7.4-alpine`
- Ollama，默认地址 `http://localhost:11434`
- 使用默认云端模型和 Rerank 配置时，需要阿里云模型服务与 Cohere 的 API Key

项目自带 Maven Wrapper，无需单独安装 Maven。以下命令均在项目根目录执行。

## 启动

### 1. 启动数据库与缓存

在同一个终端设置 Redis 凭据，供 Compose 和后续启动的 Java 进程共同读取：

```bash
export REDIS_USERNAME=admin
export REDIS_PASSWORD='替换为你的Redis密码'
docker compose -f compose/server-compose.yml up -d
```

PostgreSQL 默认连接为 `localhost:5432/ai_db`，用户名和密码均为 `postgres`。Redis 默认端口为 `6379`，Compose 创建 ACL 用户 `admin` 并禁用 `default` 用户。

数据库连接配置位于 [application.yml](rag-service/src/main/resources/application.yml)，也可通过 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 覆盖。Flyway 在应用启动时创建业务表，PGVector 表及向量索引由 Spring AI 初始化。

Compose 只启动数据库和 Redis，不包含 Java 服务或 Ollama。如果使用 `.env` 向 Compose 传参，还需要将对应变量传给 Java 进程或 IDE 运行配置，Spring Boot 不会自动读取 Compose 的 `.env`。

### 2. 准备模型

启动 Ollama 并拉取模型：

```bash
ollama pull nomic-embed-text
ollama pull qwen3-vl:2b
```

当前模型配置：

| 用途 | 模型 / 调用编码 | 配置位置 |
| --- | --- | --- |
| Embedding | `nomic-embed-text`，768 维 | `EmbeddingConfig.java` |
| 本地聊天 | `modelCode: local` → `qwen3-vl:2b` | `ChatConfig.java` |
| 云端聊天及默认问题改写、扩展 | `modelCode: qwen3.8-flash` | `ChatConfig.java` |
| Rerank | `rerank-v4.0-pro` | `app.rag.rerank` |

模型服务地址和聊天模型目前在 Java 配置类中定义，接入自己的服务时需检查 [ChatConfig.java](rag-service/src/main/java/com/elliot/ai/rag/config/ChatConfig.java) 与 [EmbeddingConfig.java](rag-service/src/main/java/com/elliot/ai/rag/config/EmbeddingConfig.java)。更换 Embedding 模型时，需同时匹配向量维度并重新索引文档。

按当前默认配置设置密钥，变量名区分大小写：

```bash
export alibaba_key='你的阿里云模型API Key'
export cohere_api_key='你的Cohere API Key'
```

`RERANK_API_KEY` 不是当前 YAML 使用的变量名。Rerank 可通过 `APP_RAG_RERANK_ENABLED=false` 关闭；默认 `fallback-enabled=true`，调用失败时回退到融合排序。当前云端 `ChatClient` Bean 无条件创建，Rerank 密钥也使用无默认值的占位符，关闭调用开关不等于自动移除相应启动配置要求。

### 3. 构建并运行

```bash
./mvnw clean package -DskipTests
java -jar rag-service/target/rag-service-0.0.1-SNAPSHOT.jar
```

此构建命令跳过测试执行。仓库中的集成测试及检索评测可能依赖真实数据库、文档或模型服务，应在准备相应环境后单独运行。

服务默认地址：`http://localhost:8090`。Swagger UI：`http://localhost:8090/swagger-ui.html`；OpenAPI JSON：`http://localhost:8090/v3/api-docs`。

## 从上传文档到问答

先创建知识库：

```bash
curl -X POST http://localhost:8090/api/knowledge-bases \
  -H 'Content-Type: application/json' \
  -d '{"name":"示例知识库","description":"项目文档"}'
curl http://localhost:8090/api/knowledge-bases
```

创建接口返回成功状态，知识库 ID 可从列表接口取得。将下面的变量替换为实际 UUID：

```bash
KB_ID='知识库UUID'
curl -X POST "http://localhost:8090/api/knowledge-bases/$KB_ID/documents" \
  -F 'file=@/absolute/path/to/document.pdf'
```

上传只保存原始文件和文档记录，之后需显式创建处理任务：

```bash
DOC_ID='上传响应中的文档UUID'
curl -X POST "http://localhost:8090/api/knowledge-bases/$KB_ID/documents/$DOC_ID/process-tasks"

TASK_ID='创建任务响应中的任务UUID'
curl "http://localhost:8090/api/knowledge-bases/$KB_ID/documents/$DOC_ID/process-tasks/$TASK_ID"
```

任务依次进行解析、切块和索引。状态包括 `PENDING`、`RUNNING`、`RETRY_WAIT`、`SUCCEEDED`、`FAILED` 和 `CANCELLED`；等待 `SUCCEEDED` 后再进行知识问答。重复创建任务会返回已有活动任务；重新处理接口会清理已有派生数据后重建，存在活动任务时不允许重新处理。

发起流式问答：

```bash
curl -N -X POST http://localhost:8090/api/rag/chat/stream \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d "{\"knowledgeBaseId\":\"$KB_ID\",\"question\":\"这份文档介绍了什么？\",\"modelCode\":\"local\",\"topK\":5}"
```

首次请求省略 `conversationId`，服务自动创建会话。后续请求携带返回的 `conversationId` 和相同 `knowledgeBaseId`，即可使用历史消息进行追问改写。`modelCode` 必填，可通过 `GET /api/models` 查询；`topK` 可选，范围为 1～20；`similarityThreshold` 可选，范围为 0～1。

调用 `/api/rag/chat` 可一次性获取完整回答，返回结果包含 `conversationId`、`answer`、`knowledgeFound`、`sources`、`tokenUsage` 和 `trace`。普通接口使用 `Result` 包装，流式接口直接返回 SSE。

## RAG 流程与流式事件

```text
创建或校验会话 → 读取历史窗口 → 会话问题改写为独立问题
    → 检索查询改写 → 查询扩展
    → 每个查询执行向量召回 + 关键词召回 → 第一层 RRF
    → 跨查询按 Chunk ID 合并 + 第二层 RRF → Rerank → 最终 Top K
    → 相邻 Chunk 扩展与上下文拼接 → 模型回答
```

历史消息当前用于问题改写；最终回答提示词使用独立问题和检索上下文。没有召回资料时，服务直接返回兜底提示，不调用回答模型。

SSE 正常顺序为 `conversation → trace → sources → delta... → done`：

| 事件 | 内容 |
| --- | --- |
| `conversation` | 本次会话 ID，供后续追问使用 |
| `trace` | 会话历史来源、缓存状态、窗口统计及检索追踪信息 |
| `sources` | 引用来源和关联 Chunk 信息 |
| `delta` | 一段回答文本 |
| `done` | 结束标记，以及模型提供的 Token 用量（可能为空） |
| `error` | 执行异常时的错误提示，随后发送 `done` |

`trace.conversation` 包含 `memorySource`（`REDIS` / `DATABASE`）、`cacheStatus`（`HIT` / `MISS` / `ERROR`）、`rawMessageCount`、`windowMessageCount`、`historyChars` 和会话改写信息。字符数按 Java `String.length()` 统计，不等同于 Token 数。

`trace.retrieval` 包含检索查询改写结果、实际参与检索的查询列表、多查询候选数及 Rerank 输入、输出数量。

## 主要配置

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 地址 |
| `REDIS_USERNAME` / `REDIS_PASSWORD` | `admin` / `admin123` | Redis ACL 凭据，应用与 Compose 保持一致 |
| `REDIS_DATABASE` | `0` | Redis 逻辑数据库 |
| `REDIS_CONNECT_TIMEOUT` / `REDIS_TIMEOUT` | `3s` / `3s` | Redis 连接 / 命令超时 |
| `CONVERSATION_MEMORY_MAX_MESSAGE` | `10` | 近期历史消息数量上限 |
| `CONVERSATION_MEMORY_MAX_HISTORY_CHARS` | `12000` | 历史消息内容字符数上限 |
| `CONVERSATION_MEMORY_TTL` | `30m` | 会话缓存过期时间 |
| `CONVERSATION_QUERY_REWRITE_ENABLED` | `true` | 基于历史改写追问 |
| `CONVERSATION_QUERY_REWRITE_MODEL` | `qwen3.8-flash` | 会话问题改写模型 |
| `QUERY_REWRITE_ENABLED` / `QUERY_REWRITE_MODEL` | `true` / `qwen3.8-flash` | 检索问题改写 |
| `QUERY_EXPANSION_ENABLED` / `QUERY_EXPANSION_MODEL` | `true` / `qwen3.8-flash` | 检索问题扩展 |
| `QUERY_EXPANSION_COUNT` | `2` | 额外扩展查询数量 |
| `MULTI_QUERY_RRK` | `60` | 第二层 RRF 平滑常数 |
| `RERANK_BASE_URL` / `RERANK_PATH` | `https://api.cohere.com` / `/v2/rerank` | Rerank 服务地址 |
| `RERANK_MODEL` | `rerank-v4.0-pro` | Rerank 模型 |
| `cohere_api_key` / `alibaba_key` | 无 | Cohere / 阿里云模型密钥 |

选择 `modelCode: local` 只改变回答模型；查询改写和扩展仍使用各自配置的模型。需要统一使用本地模型时，将三个改写 / 扩展模型变量也设为 `local`。

更多默认值见 [application.yml](rag-service/src/main/resources/application.yml)：最终 `top-k=5`、每路召回 `recall-top-k=20`、Rerank 候选上限 `10`、相邻 Chunk 半径 `1`、上下文字符上限 `20000`。原始文件和解析产物分别保存在工作目录下的 `data/uploads`、`data/parsed`。

Redis 历史缓存使用 `rag:conversation:history:{conversationId}` 作为 Key。读取返回 `MISS` 或 `ERROR` 时查询数据库并尝试回填缓存；命中时直接使用缓存内容。当前缓存多步写入没有事务封装，也没有持久化的失效重试机制；读取降级不代表 Redis 故障下所有写入路径都能成功，缓存恢复后也可能暂时命中旧数据。
