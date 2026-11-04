import test from "node:test";
import assert from "node:assert/strict";
import { applyChatEvent, chatTitle, createChatState } from "../src/services/chat.js";
import { streamSse } from "../src/services/api.js";

test("工具回答解析新事件结构，不会因空引用被标记为未找到资料", async (t) => {
  const trace = { conversation: { standaloneQuery: "知识库有多少份文档？" }, retrieval: null };
  const usage = { promptTokens: 10, completionTokens: 5, totalTokens: 15 };
  const payloads = [
    { type: "conversation", conversationId: "conversation-1" },
    { type: "mode", content: "TOOL" },
    { type: "trace", trace },
    { type: "sources", sources: [] },
    { type: "delta", content: "共有" },
    { type: "delta", content: " " },
    { type: "delta", content: "3 份文档。" },
    { type: "done", usage },
  ];
  t.mock.method(globalThis, "fetch", async () => new Response(
    payloads.map((data) => `event: ${data.type}\ndata: ${JSON.stringify(data)}\n\n`).join(""),
    { headers: { "Content-Type": "text/event-stream" } },
  ));
  const chat = createChatState();
  await streamSse("/api/rag/chat/stream", {}, (event) => applyChatEvent(chat, event));
  assert.equal(chat.conversationId, "conversation-1");
  assert.equal(chat.mode, "TOOL");
  assert.deepEqual(chat.trace, trace);
  assert.equal(chat.answer, "共有 3 份文档。");
  assert.deepEqual(chat.usage, usage);
  assert.equal(chat.done, true);
  assert.equal(chatTitle(chat), "基于工具查询的回答");
});

test("RAG 有引用、无引用及缺少 mode 的响应均可显示", () => {
  const chat = createChatState();
  applyChatEvent(chat, { event: "mode", data: { content: "RAG" } });
  applyChatEvent(chat, { data: { type: "sources", sources: [{ referenceId: "S1" }] } });
  applyChatEvent(chat, { data: { type: "done", usage: null } });
  assert.equal(chatTitle(chat), "基于知识库的回答");
  applyChatEvent(chat, { data: { type: "sources", sources: [] } });
  assert.equal(chatTitle(chat), "未找到相关资料");
  const legacy = createChatState();
  applyChatEvent(legacy, { event: "done", data: {} });
  assert.equal(chatTitle(legacy), "未找到相关资料");
});

test("工具失败保留部分回答并显示错误，新请求不继承旧结果", () => {
  const chat = createChatState();
  applyChatEvent(chat, { data: { type: "mode", content: "TOOL" } });
  assert.equal(chatTitle(chat), "正在查询知识库信息");
  applyChatEvent(chat, { data: { type: "delta", content: "部分回答" } });
  applyChatEvent(chat, { data: { type: "error", content: "查询失败" } });
  applyChatEvent(chat, { data: { type: "done" } });
  assert.equal(chatTitle(chat), "回答生成失败");
  assert.equal(chat.answer, "部分回答");
  assert.equal(chat.error, "查询失败");
  const next = createChatState();
  assert.equal(next.mode, null);
  assert.equal(next.trace, null);
  assert.equal(next.answer, "");
  assert.equal(next.conversationId, null);
});
