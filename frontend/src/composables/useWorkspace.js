import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { api, post } from "../services/api.js";

const activeStatuses = new Set(["PENDING", "RUNNING", "RETRY_WAIT"]);
const emptyPage = () => ({ page: 1, size: 5, total: 0, records: [] });

export function useWorkspace(notify) {
  const libraries = ref([]);
  const activeLibrary = ref(null);
  const documents = ref([]);
  const selectedId = ref("");
  const detail = ref(null);
  const chunks = ref(emptyPage());
  const history = ref(emptyPage());
  const processTasks = reactive(new Map());
  const connection = ref("连接中");
  const busy = reactive(new Set());
  const selected = computed(
    () => documents.value.find((item) => item.id === selectedId.value) || null,
  );
  const task = computed(() => processTasks.get(selectedId.value));
  const taskActive = computed(() => activeStatuses.has(task.value?.status));
  const actionsDisabled = computed(
    () => !selected.value || taskActive.value || busy.has("process"),
  );
  let pollTimer;
  let libraryVersion = 0;
  let documentVersion = 0;
  let inspectorVersion = 0;
  let disposed = false;

  async function run(key, action) {
    if (busy.has(key)) return;
    busy.add(key);
    try {
      return await action();
    } catch (error) {
      if (!disposed) notify(error.message, "error");
    } finally {
      busy.delete(key);
    }
  }

  function requireLibrary() {
    if (activeLibrary.value) return true;
    notify("请先从左侧选择知识库", "error");
    return false;
  }

  function libraryPath(id = activeLibrary.value?.id) {
    return `/api/knowledge-bases/${encodeURIComponent(id)}`;
  }
  function documentPath(
    id = selectedId.value,
    libraryId = activeLibrary.value?.id,
  ) {
    return `${libraryPath(libraryId)}/documents/${encodeURIComponent(id)}`;
  }
  function stopPolling() {
    clearTimeout(pollTimer);
  }
  function clearInspector() {
    inspectorVersion++;
    busy.delete("inspector");
    detail.value = null;
    chunks.value = emptyPage();
    history.value = emptyPage();
  }
  function rememberTask(value) {
    processTasks.set(value.documentId, value);
    localStorage.setItem(`atlas-process-task:${value.documentId}`, value.id);
  }

  async function loadLibraries(preferredId) {
    return run("libraries", async () => {
      try {
        libraries.value = await api("/api/knowledge-bases");
      } catch (error) {
        connection.value = "连接失败";
        throw error;
      }
      connection.value = "已连接";
      const target =
        preferredId ||
        activeLibrary.value?.id ||
        localStorage.getItem("atlas-active-library");
      const library =
        libraries.value.find((item) => item.id === target) ||
        libraries.value[0];
      if (library) await selectLibrary(library.id);
      else {
        libraryVersion++;
        documentVersion++;
        stopPolling();
        activeLibrary.value = null;
        documents.value = [];
        selectedId.value = "";
        clearInspector();
      }
    });
  }

  async function selectLibrary(id) {
    const version = ++libraryVersion;
    documentVersion++;
    stopPolling();
    activeLibrary.value = null;
    documents.value = [];
    selectedId.value = "";
    clearInspector();
    try {
      const library = await api(libraryPath(id));
      if (disposed || version !== libraryVersion) return;
      activeLibrary.value = library;
      localStorage.setItem("atlas-active-library", id);
      await loadDocuments();
    } catch (error) {
      if (version === libraryVersion) notify(error.message, "error");
    }
  }

  async function createLibrary(values) {
    return run("create", async () => {
      await post("/api/knowledge-bases", values);
      await loadLibraries();
      const created = libraries.value.find((item) => item.name === values.name);
      if (created) await selectLibrary(created.id);
      notify("知识库已创建");
      return true;
    });
  }

  async function loadDocuments(preferredId) {
    if (!activeLibrary.value) return;
    const libraryId = activeLibrary.value.id;
    const version = ++documentVersion;
    const result = await api(`${libraryPath(libraryId)}/documents`);
    if (
      disposed ||
      activeLibrary.value?.id !== libraryId ||
      version !== documentVersion
    )
      return;
    documents.value = Array.isArray(result) ? result : [];
    const next =
      documents.value.find(
        (item) => item.id === (preferredId || selectedId.value),
      ) || documents.value[0];
    await selectDocument(next?.id || "");
  }

  async function selectDocument(id) {
    stopPolling();
    selectedId.value = id;
    clearInspector();
    if (!id) return;
    await Promise.allSettled([
      loadInspector().catch((error) => notify(error.message, "error")),
      restoreTask(),
    ]);
  }

  async function loadInspector() {
    const path = documentPath();
    const version = ++inspectorVersion;
    busy.add("inspector");
    const result = await Promise.allSettled([
      api(path),
      api(`${path}/chunks?page=1&size=5`),
      api(`${path}/process-tasks?page=1&size=5`),
    ]);
    if (disposed || version !== inspectorVersion) return;
    busy.delete("inspector");
    const targets = [detail, chunks, history];
    result.forEach((item, index) => {
      if (item.status === "fulfilled") targets[index].value = item.value;
      else notify(item.reason.message, "error");
    });
  }

  async function loadPage(type, page) {
    const target = type === "chunks" ? chunks : history;
    const max = Math.max(1, Math.ceil(target.value.total / target.value.size));
    if (!selected.value || page < 1 || page > max) return;
    const version = inspectorVersion;
    const result = await api(
      `${documentPath()}/${type}?page=${page}&size=${target.value.size}`,
    );
    if (!disposed && version === inspectorVersion) target.value = result;
  }

  async function restoreTask() {
    const id = selectedId.value;
    const libraryId = activeLibrary.value?.id;
    const taskId =
      processTasks.get(id)?.id ||
      localStorage.getItem(`atlas-process-task:${id}`);
    if (!taskId) return;
    try {
      const value = await api(
        `${documentPath(id, libraryId)}/process-tasks/${encodeURIComponent(taskId)}`,
      );
      if (
        disposed ||
        selectedId.value !== id ||
        activeLibrary.value?.id !== libraryId
      )
        return;
      rememberTask(value);
      if (activeStatuses.has(value.status)) schedulePoll(id, libraryId, taskId);
    } catch {
      processTasks.delete(id);
      localStorage.removeItem(`atlas-process-task:${id}`);
    }
  }

  function schedulePoll(id, libraryId, taskId) {
    stopPolling();
    pollTimer = setTimeout(async () => {
      if (
        disposed ||
        selectedId.value !== id ||
        activeLibrary.value?.id !== libraryId
      )
        return;
      try {
        const value = await api(
          `${documentPath(id, libraryId)}/process-tasks/${encodeURIComponent(taskId)}`,
        );
        if (
          disposed ||
          selectedId.value !== id ||
          activeLibrary.value?.id !== libraryId
        )
          return;
        rememberTask(value);
        if (activeStatuses.has(value.status))
          schedulePoll(id, libraryId, taskId);
        else {
          await loadDocuments(id);
          notify(
            value.status === "SUCCEEDED" ? "文档处理已完成" : "文档处理失败",
            value.status === "SUCCEEDED" ? "success" : "error",
          );
        }
      } catch (error) {
        notify(`获取处理进度失败：${error.message}`, "error");
      }
    }, 1200);
  }

  async function upload(file) {
    if (!requireLibrary()) return;
    if (!file) return notify("请选择要上传的文件", "error");
    if (file.size > 10 * 1024 * 1024)
      return notify("单个文件最大 10 MB", "error");
    return run("upload", async () => {
      const libraryId = activeLibrary.value.id;
      const form = new FormData();
      form.append("file", file);
      const uploaded = await api(`${libraryPath(libraryId)}/documents`, {
        method: "POST",
        body: form,
      });
      if (activeLibrary.value?.id === libraryId)
        await loadDocuments(uploaded.id);
      notify(`“${uploaded.originalName}”已上传`);
      return true;
    });
  }

  async function processDocument(action = "process") {
    if (!selected.value || !requireLibrary() || taskActive.value) return;
    const info = selected.value;
    if (
      action === "reprocess" &&
      !window.confirm(
        `重新处理“${info.originalName || info.id}”会清除已有的解析结果、Chunk 和向量，是否继续？`,
      )
    )
      return;
    const libraryId = activeLibrary.value.id;
    const suffix = {
      process: "process-tasks",
      reprocess: "process-tasks/reprocess",
      chunk: "chunks",
      index: "index",
    }[action];
    await run("process", async () => {
      const result = await api(
        `${documentPath(info.id, libraryId)}/${suffix}`,
        { method: "POST" },
      );
      if (action === "process" || action === "reprocess") rememberTask(result);
      if (activeLibrary.value?.id === libraryId) await loadDocuments(info.id);
      notify(
        {
          process: "处理任务已创建",
          reprocess: "已创建重新处理任务",
          chunk: `文本切分完成，共 ${result} 个 Chunk`,
          index: `向量索引完成，共 ${result?.vectorCount} 条`,
        }[action],
      );
    });
  }

  async function deleteDocument(info) {
    if (
      !window.confirm(
        `确定删除“${info.originalName || info.id}”吗？\n关联的 Chunk 和向量也会一并删除。`,
      )
    )
      return;
    const libraryId = activeLibrary.value.id;
    await run(`delete:${info.id}`, async () => {
      await api(documentPath(info.id, libraryId), { method: "DELETE" });
      processTasks.delete(info.id);
      localStorage.removeItem(`atlas-process-task:${info.id}`);
      if (activeLibrary.value?.id === libraryId) await loadDocuments();
      notify("文档已删除");
    });
  }

  onBeforeUnmount(() => {
    disposed = true;
    libraryVersion++;
    documentVersion++;
    inspectorVersion++;
    stopPolling();
  });

  return {
    libraries,
    activeLibrary,
    documents,
    selectedId,
    selected,
    detail,
    chunks,
    history,
    task,
    taskActive,
    actionsDisabled,
    connection,
    busy,
    run,
    requireLibrary,
    loadLibraries,
    selectLibrary,
    createLibrary,
    loadDocuments,
    selectDocument,
    loadPage,
    upload,
    processDocument,
    deleteDocument,
  };
}
