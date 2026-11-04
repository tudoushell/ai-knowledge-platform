import { ref, reactive } from "vue";
import { streamSse } from "../services/api.js";
import { applyChatEvent, createChatState } from "../services/chat.js";

// 每个请求只更新自己的问答记录；切换知识库或停止后，迟到的事件会被忽略。
export function useChatSession(stream = streamSse) {
  const turns = ref([]);
  const conversationId = ref(null);
  const loading = ref(false);
  let active;
  let sequence = 0;

  function stop() {
    if (!active) return;
    const { controller, turn } = active;
    active = null;
    if (!turn.done) turn.cancelled = true;
    loading.value = false;
    controller.abort();
  }

  function reset() {
    stop();
    turns.value = [];
    conversationId.value = null;
  }

  async function send(payload) {
    if (loading.value) return;
    const turn = reactive({
      ...createChatState(),
      id: ++sequence,
      question: payload.question,
      modelCode: payload.modelCode,
    });
    const request = { controller: new AbortController(), turn };
    active = request;
    loading.value = true;
    turns.value.push(turn);
    try {
      await stream("/api/rag/chat/stream", {
        method: "POST",
        signal: request.controller.signal,
        headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
        body: JSON.stringify({ ...payload, conversationId: conversationId.value }),
      }, (event) => {
        if (active !== request) return;
        applyChatEvent(turn, event);
        if (turn.conversationId) conversationId.value = turn.conversationId;
      });
      if (active === request && !turn.done && !turn.error)
        turn.error = "连接已中断，回答可能不完整。可编辑问题后重新发送。";
    } catch (error) {
      if (active === request) {
        if (error.name === "AbortError") turn.cancelled = true;
        else turn.error = error.message || "请求失败，请稍后重试。";
      }
    } finally {
      if (active === request) {
        active = null;
        loading.value = false;
      }
    }
  }

  return { turns, conversationId, loading, send, stop, reset };
}
