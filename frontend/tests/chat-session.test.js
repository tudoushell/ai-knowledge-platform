import test from "node:test";
import assert from "node:assert/strict";
import { useChatSession } from "../src/composables/useChatSession.js";
import { chatTitle } from "../src/services/chat.js";

const payload = { question: "有多少份文档？", modelCode: "test-model", knowledgeBaseId: "kb-1" };
const event = (type, data = {}) => ({ data: { type, ...data } });

test("连续提问保留每轮答案，并在下一轮发送会话 ID", async () => {
  const bodies = [];
  const session = useChatSession(async (_path, options, emit) => {
    bodies.push(JSON.parse(options.body));
    emit(event("conversation", { conversationId: "session-1" }));
    emit(event("delta", { content: "回答 " + bodies.length }));
    emit(event("done"));
  });
  await session.send(payload);
  await session.send({ ...payload, question: "再说明一下" });
  assert.equal(bodies[0].conversationId, null);
  assert.equal(bodies[1].conversationId, "session-1");
  assert.equal(session.turns.value.length, 2);
  assert.equal(session.turns.value[0].answer, "回答 1");
  assert.equal(session.turns.value[1].question, "再说明一下");
  assert.equal(session.turns.value[1].answer, "回答 2");
});

test("生成期间阻止重复发送，停止后保留部分回答并忽略迟到事件", async () => {
  let emit;
  let finish;
  let signal;
  let calls = 0;
  const session = useChatSession(async (_path, options, callback) => {
    calls++;
    emit = callback;
    signal = options.signal;
    await new Promise((resolve) => { finish = resolve; });
  });
  const running = session.send(payload);
  emit(event("delta", { content: "半段回答" }));
  await session.send(payload);
  assert.equal(calls, 1);
  session.stop();
  emit(event("delta", { content: "迟到的内容" }));
  emit(event("done"));
  assert.equal(signal.aborted, true);
  assert.equal(session.loading.value, false);
  assert.equal(session.turns.value[0].answer, "半段回答");
  assert.equal(session.turns.value[0].done, false);
  assert.equal(chatTitle(session.turns.value[0]), "已停止生成");
  finish();
  await running;
});

test("重置会话后，旧请求的事件和 finally 不会污染新请求", async () => {
  const pending = [];
  const session = useChatSession(async (_path, _options, emit) => {
    await new Promise((finish) => pending.push({ emit, finish }));
  });
  const first = session.send(payload);
  pending[0].emit(event("conversation", { conversationId: "old-session" }));
  session.reset();
  assert.equal(session.turns.value.length, 0);
  assert.equal(session.conversationId.value, null);
  const second = session.send({ ...payload, knowledgeBaseId: "kb-2" });
  pending[0].emit(event("conversation", { conversationId: "old-session" }));
  pending[0].finish();
  await first;
  assert.equal(session.loading.value, true);
  assert.equal(session.conversationId.value, null);
  pending[1].emit(event("conversation", { conversationId: "new-session" }));
  pending[1].emit(event("done"));
  pending[1].finish();
  await second;
  assert.equal(session.conversationId.value, "new-session");
  assert.equal(session.turns.value.length, 1);
  assert.equal(session.loading.value, false);
});

test("网络错误或缺少 done 的断流不能标为完成，错误记录可保留", async () => {
  const interrupted = useChatSession(async (_path, _options, emit) => emit(event("delta", { content: "部分内容" })));
  await interrupted.send(payload);
  assert.match(interrupted.turns.value[0].error, /连接已中断/);
  assert.equal(interrupted.turns.value[0].done, false);
  const failed = useChatSession(async () => { throw new Error("无法连接服务"); });
  await failed.send(payload);
  assert.equal(failed.turns.value[0].error, "无法连接服务");
  assert.equal(failed.loading.value, false);
});
