// 所有请求使用同源 /api，开发时由 Vite 代理，生产时由反向代理转发。
import { auth } from "./auth.js";

export function createApiClient(transport = (...args) => auth.fetch(...args)) {
  async function api(path, options = {}) {
    const response = await transport(path, options);
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || body.code !== 200) {
      throw new Error(body?.message || `请求失败（HTTP ${response.status}）`);
    }
    return body.data;
  }

  function post(path, data, options = {}) {
    return api(path, {
      ...options,
      method: "POST",
      headers: { "Content-Type": "application/json", ...options.headers },
      body: JSON.stringify(data),
    });
  }

  return {
    api,
    post,
    streamSse: (path, options, onEvent) =>
      readSse(transport, path, options, onEvent),
  };
}

export const { api, post, streamSse } = createApiClient();

export function parseSseMessage(message) {
  let eventName = "message";
  const dataLines = [];

  for (const line of message.split(/\r?\n/)) {
    if (!line || line.startsWith(":")) continue;
    const separator = line.indexOf(":");
    const field = separator === -1 ? line : line.slice(0, separator);
    let value = separator === -1 ? "" : line.slice(separator + 1);
    if (value.startsWith(" ")) value = value.slice(1);

    if (field === "event") eventName = value;
    if (field === "data") dataLines.push(value);
  }

  if (!dataLines.length) return null;
  const rawData = dataLines.join("\n");
  try {
    return { event: eventName, data: JSON.parse(rawData) };
  } catch {
    return { event: eventName, data: rawData };
  }
}

async function readSse(transport, path, options, onEvent) {
  const response = await transport(path, options);
  const contentType = response.headers.get("content-type") || "";

  if (
    !response.ok ||
    !response.body ||
    !contentType.includes("text/event-stream")
  ) {
    const text = await response.text();
    let body = null;
    try {
      body = JSON.parse(text);
    } catch {
      // 非 JSON 错误响应直接使用原始文本。
    }
    throw new Error(
      body?.message || text || `请求失败（HTTP ${response.status}）`,
    );
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value, { stream: !done });

      let separator;
      while ((separator = buffer.match(/\r?\n\r?\n/))) {
        const message = buffer.slice(0, separator.index);
        buffer = buffer.slice(separator.index + separator[0].length);
        const parsed = parseSseMessage(message);
        if (parsed) onEvent(parsed);
      }

      if (done) break;
    }

    if (buffer.trim()) {
      const parsed = parseSseMessage(buffer);
      if (parsed) onEvent(parsed);
    }
  } finally {
    await reader.cancel().catch(() => {});
    reader.releaseLock();
  }
}
