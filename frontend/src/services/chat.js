export function createChatState() {
  return {
    answer: "",
    sources: [],
    usage: null,
    conversationId: null,
    mode: null,
    trace: null,
    error: "",
    done: false,
    cancelled: false,
  };
}

// SSE 的事件名和 data.type 均可标识事件，优先使用后端载荷中的类型。
export function applyChatEvent(chat, { event, data }) {
  if (!data || typeof data !== "object") return;
  switch (data.type || event) {
    case "conversation":
      chat.conversationId = data.conversationId || null;
      break;
    case "mode":
      chat.mode = data.content;
      break;
    case "trace":
      chat.trace = data.trace || null;
      break;
    case "source":
    case "sources":
      chat.sources = Array.isArray(data.sources) ? data.sources : [];
      break;
    case "delta":
      chat.answer += data.content || "";
      break;
    case "error":
      chat.error = data.content || "回答生成失败，请稍后重试。";
      break;
    case "done":
      chat.usage = data.usage || null;
      chat.done = true;
      break;
  }
}

export function chatTitle(chat) {
  if (chat?.error) return "回答生成失败";
  if (chat?.cancelled) return "已停止生成";
  if (chat?.mode === "TOOL")
    return chat.done ? "基于工具查询的回答" : "正在查询知识库信息";
  if (chat?.done)
    return chat.sources.length ? "基于知识库的回答" : "未找到相关资料";
  if (chat?.answer) return "正在生成回答";
  return "正在准备回答";
}
