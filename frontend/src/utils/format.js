export function formatDate(value) {
  if (!value) return "时间未知";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
  }).format(new Date(value));
}

export function formatBytes(value) {
  const size = Number(value);
  if (!Number.isFinite(size) || size < 0) return "大小未知";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

export function statusLabel(status) {
  return (
    {
      UPLOADED: "已上传",
      QUEUED: "等待处理",
      PARSING: "解析中",
      PARSED: "已解析",
      CHUNKING: "切分中",
      CHUNKED: "已切分",
      INDEXING: "索引中",
      INDEXED: "已索引",
      INDEX_FAILED: "索引失败",
      PROCESS_FAILED: "处理失败",
      FAILED: "处理失败",
    }[status] ||
    status ||
    "未知状态"
  );
}

export function taskStatusLabel(status) {
  return (
    {
      PENDING: "等待领取",
      RUNNING: "处理中",
      RETRY_WAIT: "等待重试",
      SUCCEEDED: "已完成",
      FAILED: "处理失败",
      CANCELLED: "已取消",
    }[status] ||
    status ||
    "未知状态"
  );
}

export function taskStepLabel(step) {
  return (
    {
      PARSE: "解析文档",
      CHUNK: "切分文本",
      INDEX: "生成向量",
      COMPLETE: "完成处理",
    }[step] ||
    step ||
    "等待开始"
  );
}

export function formatDateTime(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
