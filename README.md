# AI Knowledge Platform

基于 Spring Boot、Spring AI、PostgreSQL / PGVector 的混合检索知识平台。

当前包含文档解析、文本切分、向量与关键词混合检索、RRF 融合、Rerank、引用上下文扩展，以及 SSE 流式知识问答。

```text
ai-knowledge-platform
├── ai-common                # 通用响应、异常处理、提示词和 JSON 工具
└── rag-service              # 可执行的 RAG 服务
```

## 环境要求

- JDK 17+
- PostgreSQL 16+，并安装 `pgvector` 扩展
- Ollama（默认使用 `nomic-embed-text` 作为 Embedding 模型；本地聊天模型为 `qwen3-vl:2b`）

创建数据库后，按需调整 `rag-service/src/main/resources/application.yml` 中的 PostgreSQL 连接信息。Flyway 会在应用启动时创建业务表；PGVector 表由 Spring AI 初始化。

项目内置 PostgreSQL / PGVector 的 Docker Compose 配置，可先启动数据库：

```bash
docker compose -f compose/pg-compose.yml up -d
```

## 启动

```bash
./mvnw clean package
java -jar rag-service/target/rag-service-0.0.1-SNAPSHOT.jar
```

服务默认运行在 `http://localhost:8090`，接口文档为 `http://localhost:8090/swagger-ui.html`。

## 可选环境变量

```bash
export RERANK_BASE_URL=https://api.cohere.com
export RERANK_API_KEY=your-api-key
```

未配置或不可用的 Rerank 服务可通过 `app.rag.rerank.enabled=false` 关闭；也可保留 `fallback-enabled=true`，在 Rerank 失败时回退到 RRF 排序。
