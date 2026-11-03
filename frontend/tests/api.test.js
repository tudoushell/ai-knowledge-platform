import test from "node:test";
import assert from "node:assert/strict";
import { api, parseSseMessage, streamSse } from "../src/services/api.js";

test("SSE 支持注释、多行数据与 CRLF", () => {
  assert.equal(parseSseMessage(": heartbeat"), null);
  assert.deepEqual(
    parseSseMessage('event: delta\r\ndata: {"content":\r\ndata: "你好"}'),
    {
      event: "delta",
      data: { content: "你好" },
    },
  );
});

test("SSE 正确拼接跨网络分片的中文、事件边界和末尾事件", async (t) => {
  const events = [];
  const bytes = new TextEncoder().encode(
    'event: delta\r\ndata: {"content":"知识库"}\r\n\r\nevent: done\ndata: {"type":"done"}',
  );
  t.mock.method(
    globalThis,
    "fetch",
    async () =>
      new Response(
        new ReadableStream({
          start(controller) {
            // 逐字节发送，覆盖 UTF-8 字符和 CRLF 被分到不同块的情况。
            for (const byte of bytes)
              controller.enqueue(new Uint8Array([byte]));
            controller.close();
          },
        }),
        { headers: { "Content-Type": "text/event-stream;charset=UTF-8" } },
      ),
  );
  await streamSse("/api/rag/chat/stream", {}, (event) => events.push(event));
  assert.deepEqual(events, [
    { event: "delta", data: { content: "知识库" } },
    { event: "done", data: { type: "done" } },
  ]);
});

test("API 与流式请求保留后端业务错误", async (t) => {
  t.mock.method(globalThis, "fetch", async () =>
    Response.json({ code: 500, message: "模型不可用" }),
  );
  await assert.rejects(api("/api/models"), /模型不可用/);
  await assert.rejects(
    streamSse("/api/rag/chat/stream", {}, () => {}),
    /模型不可用/,
  );
});

test("中断流式请求会释放 reader", async (t) => {
  let cancelled = false;
  const stream = new ReadableStream({
    start(controller) {
      controller.enqueue(
        new TextEncoder().encode('data: {"type":"delta"}\n\n'),
      );
    },
    cancel() {
      cancelled = true;
    },
  });
  t.mock.method(
    globalThis,
    "fetch",
    async () =>
      new Response(stream, {
        headers: { "Content-Type": "text/event-stream" },
      }),
  );
  await assert.rejects(
    streamSse("/stream", {}, () => {
      throw new Error("停止读取");
    }),
    /停止读取/,
  );
  assert.equal(cancelled, true);
  assert.equal(stream.locked, false);
});
