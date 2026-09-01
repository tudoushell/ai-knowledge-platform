const state = {
    libraries: [],
    activeLibrary: null,
    models: [],
    documents: [],
    processTasks: new Map(),
    documentDetail: null,
    chunkPage: { page: 1, size: 5, total: 0, records: [] },
    taskHistoryPage: { page: 1, size: 5, total: 0, records: [] }
};

const elements = {
    libraryList: document.querySelector('#library-list'),
    activeName: document.querySelector('#active-name'),
    activeDescription: document.querySelector('#active-description'),
    activeBadge: document.querySelector('#active-badge'),
    apiStatus: document.querySelector('#api-status'),
    createForm: document.querySelector('#create-form'),
    createToggle: document.querySelector('#create-toggle'),
    uploadForm: document.querySelector('#upload-form'),
    fileInput: document.querySelector('#document-file'),
    fileName: document.querySelector('#file-name'),
    documentId: document.querySelector('#document-id'),
    documentSummary: document.querySelector('#document-summary'),
    documentList: document.querySelector('#document-list'),
    documentCount: document.querySelector('#document-count'),
    refreshDocuments: document.querySelector('#refresh-documents'),
    processButton: document.querySelector('#process-document'),
    processTaskStatus: document.querySelector('#process-task-status'),
    reprocessButton: document.querySelector('#reprocess-document'),
    inspectorTitle: document.querySelector('#inspector-title'),
    inspectorStatus: document.querySelector('#inspector-status'),
    documentDetail: document.querySelector('#document-detail'),
    chunkList: document.querySelector('#chunk-list'),
    chunksPrev: document.querySelector('#chunks-prev'),
    chunksNext: document.querySelector('#chunks-next'),
    chunksPage: document.querySelector('#chunks-page'),
    taskHistory: document.querySelector('#task-history'),
    historyPrev: document.querySelector('#history-prev'),
    historyNext: document.querySelector('#history-next'),
    historyPage: document.querySelector('#history-page'),
    chunkButton: document.querySelector('#chunk-document'),
    indexButton: document.querySelector('#index-document'),
    retrievalOutput: document.querySelector('#retrieval-output'),
    keywordSearchOutput: document.querySelector('#keyword-search-output'),
    chatOutput: document.querySelector('#chat-output'),
    chatModel: document.querySelector('#chat-model'),
    chatSubmit: document.querySelector('#chat-submit'),
    toast: document.querySelector('#toast')
};

let toastTimer;
let taskPollTimer;

const ACTIVE_TASK_STATUSES = new Set(['PENDING', 'RUNNING', 'RETRY_WAIT']);

async function api(path, options = {}) {
    const response = await fetch(path, options);
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || body.code !== 200) {
        throw new Error(body?.message || `请求失败（HTTP ${response.status}）`);
    }
    return body.data;
}

function parseSseMessage(message) {
    let eventName = 'message';
    const dataLines = [];

    for (const line of message.split(/\r?\n/)) {
        if (!line || line.startsWith(':')) continue;
        const separator = line.indexOf(':');
        const field = separator === -1 ? line : line.slice(0, separator);
        let value = separator === -1 ? '' : line.slice(separator + 1);
        if (value.startsWith(' ')) value = value.slice(1);

        if (field === 'event') eventName = value;
        if (field === 'data') dataLines.push(value);
    }

    if (!dataLines.length) return null;
    const rawData = dataLines.join('\n');
    try {
        return { event: eventName, data: JSON.parse(rawData) };
    } catch {
        return { event: eventName, data: rawData };
    }
}

async function streamSse(path, options, onEvent) {
    const response = await fetch(path, options);
    const contentType = response.headers.get('content-type') || '';

    if (!response.ok || !response.body || !contentType.includes('text/event-stream')) {
        const text = await response.text();
        let body = null;
        try {
            body = JSON.parse(text);
        } catch {
            // 非 JSON 错误响应直接使用原始文本。
        }
        throw new Error(body?.message || text || `请求失败（HTTP ${response.status}）`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

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
}

function notify(message, type = 'success') {
    clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.className = `toast visible${type === 'error' ? ' error' : ''}`;
    toastTimer = setTimeout(() => elements.toast.className = 'toast', 3200);
}

async function withBusy(button, task) {
    button.classList.add('busy');
    button.disabled = true;
    try {
        return await task();
    } finally {
        button.classList.remove('busy');
        button.disabled = false;
        updateDocumentActions();
    }
}

function requireLibrary() {
    if (!state.activeLibrary) {
        notify('请先从左侧选择知识库', 'error');
        return false;
    }
    return true;
}

function formatDate(value) {
    if (!value) return '时间未知';
    return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(new Date(value));
}

function clearFileSelection() {
    // 直接清空 file input，避免部分浏览器仅调用 form.reset() 后仍保留已选文件名。
    elements.fileInput.value = '';
    elements.fileName.textContent = '选择 TXT、Markdown、PDF 或 Word 文件';
}

async function loadModels() {
    elements.chatModel.disabled = true;
    elements.chatSubmit.disabled = true;
    elements.chatModel.replaceChildren(new Option('正在加载模型…', ''));

    try {
        const models = await api('/api/models');
        if (!Array.isArray(models) || !models.length) {
            throw new Error('当前没有可用的聊天模型');
        }

        state.models = models;
        elements.chatModel.replaceChildren();
        models.forEach(modelCode => {
            elements.chatModel.append(new Option(modelCode, modelCode));
        });

        const savedModel = localStorage.getItem('atlas-chat-model');
        elements.chatModel.value = models.includes(savedModel) ? savedModel : models[0];
        localStorage.setItem('atlas-chat-model', elements.chatModel.value);
        elements.chatModel.disabled = false;
        elements.chatSubmit.disabled = false;
    } catch (error) {
        state.models = [];
        elements.chatModel.replaceChildren(new Option('模型加载失败', ''));
        notify(error.message, 'error');
    }
}

async function loadLibraries(preferredId) {
    elements.libraryList.innerHTML = '<div class="library-empty">正在读取知识库…</div>';
    try {
        state.libraries = await api('/api/knowledge-bases');
        elements.apiStatus.textContent = '已连接';
        elements.apiStatus.classList.remove('error');
        renderLibraries();
        const target = preferredId || state.activeLibrary?.id || localStorage.getItem('atlas-active-library');
        const library = state.libraries.find(item => item.id === target) || state.libraries[0];
        if (library) await selectLibrary(library.id);
        else clearActiveLibrary();
    } catch (error) {
        elements.apiStatus.textContent = '连接失败';
        elements.apiStatus.classList.add('error');
        elements.libraryList.innerHTML = '<div class="library-empty">无法读取知识库，请确认服务已启动。</div>';
        notify(error.message, 'error');
    }
}

function renderLibraries() {
    elements.libraryList.replaceChildren();
    if (!state.libraries.length) {
        const empty = document.createElement('div');
        empty.className = 'library-empty';
        empty.textContent = '还没有知识库，先创建一个。';
        elements.libraryList.append(empty);
        return;
    }
    state.libraries.forEach(library => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = `library-item${state.activeLibrary?.id === library.id ? ' active' : ''}`;
        const name = document.createElement('strong');
        name.textContent = library.name;
        const meta = document.createElement('small');
        meta.textContent = `${library.status || 'UNKNOWN'} · ${formatDate(library.updatedAt || library.createdAt)}`;
        button.append(name, meta);
        button.addEventListener('click', () => selectLibrary(library.id));
        elements.libraryList.append(button);
    });
}

async function selectLibrary(id) {
    try {
        state.activeLibrary = await api(`/api/knowledge-bases/${encodeURIComponent(id)}`);
        localStorage.setItem('atlas-active-library', id);
        renderLibraries();
        renderActiveLibrary();
        await loadDocuments();
    } catch (error) {
        notify(error.message, 'error');
    }
}

function clearActiveLibrary() {
    stopTaskPolling();
    state.activeLibrary = null;
    state.documents = [];
    clearInspector();
    elements.activeName.textContent = '选择一个知识库';
    elements.activeDescription.textContent = '从左侧选择知识库，开始整理与检索资料。';
    elements.activeBadge.textContent = '未选择';
    elements.activeBadge.className = 'active-badge';
    elements.documentId.value = '';
    renderDocument(null);
    renderDocumentList();
}

function renderActiveLibrary() {
    const library = state.activeLibrary;
    elements.activeName.textContent = library.name;
    elements.activeDescription.textContent = library.description || `知识库 ID：${library.id}`;
    elements.activeBadge.textContent = library.status || 'UNKNOWN';
    elements.activeBadge.className = `active-badge${library.status === 'ENABLED' ? ' enabled' : ''}`;
}

async function loadDocuments(preferredId) {
    if (!state.activeLibrary) {
        state.documents = [];
        renderDocumentList();
        return;
    }
    const libraryId = state.activeLibrary.id;
    elements.documentList.innerHTML = '<div class="document-list-empty">正在读取文档…</div>';
    const documents = await api(`/api/knowledge-bases/${encodeURIComponent(libraryId)}/documents`);
    if (state.activeLibrary?.id !== libraryId) return;

    state.documents = Array.isArray(documents) ? documents : [];
    const selected = state.documents.find(item => item.id === preferredId)
        || state.documents.find(item => item.id === elements.documentId.value)
        || state.documents[0]
        || null;
    elements.documentId.value = selected?.id || '';
    renderDocument(selected);
    renderDocumentList();
    restoreTaskProgress(selected);
    if (selected) loadInspector(selected).catch(error => notify(error.message, 'error'));
    else clearInspector();
}

function currentDocument() {
    const id = elements.documentId.value.trim();
    if (!id || !state.activeLibrary) return null;
    return state.documents.find(item => item.id === id) || null;
}

function selectDocument(id) {
    const selected = state.documents.find(item => item.id === id) || null;
    stopTaskPolling();
    elements.documentId.value = selected?.id || '';
    renderDocument(selected);
    renderDocumentList();
    restoreTaskProgress(selected);
    if (selected) loadInspector(selected).catch(error => notify(error.message, 'error'));
    else clearInspector();
}

function formatBytes(value) {
    const size = Number(value);
    if (!Number.isFinite(size) || size < 0) return '大小未知';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function statusLabel(status) {
    return {
        UPLOADED: '已上传',
        QUEUED: '等待处理',
        PARSING: '解析中',
        PARSED: '已解析',
        CHUNKING: '切分中',
        CHUNKED: '已切分',
        INDEXING: '索引中',
        INDEXED: '已索引',
        INDEX_FAILED: '索引失败',
        PROCESS_FAILED: '处理失败',
        FAILED: '处理失败'
    }[status] || status || '未知状态';
}

function taskStorageKey(documentId) {
    return `atlas-process-task:${documentId}`;
}

function processTaskPath(documentId, taskId) {
    return `/api/knowledge-bases/${encodeURIComponent(state.activeLibrary.id)}`
        + `/documents/${encodeURIComponent(documentId)}/process-tasks/${encodeURIComponent(taskId)}`;
}

function currentProcessTask(documentInfo = currentDocument()) {
    return documentInfo ? state.processTasks.get(documentInfo.id) || null : null;
}

function isTaskActive(task) {
    return Boolean(task && ACTIVE_TASK_STATUSES.has(task.status));
}

function taskStatusLabel(status) {
    return {
        PENDING: '等待领取',
        RUNNING: '处理中',
        RETRY_WAIT: '等待重试',
        SUCCEEDED: '已完成',
        FAILED: '处理失败',
        CANCELLED: '已取消'
    }[status] || status || '未知状态';
}

function taskStepLabel(step) {
    return {
        PARSE: '解析文档',
        CHUNK: '切分文本',
        INDEX: '生成向量',
        COMPLETE: '完成处理'
    }[step] || step || '等待开始';
}

function rememberProcessTask(task) {
    if (!task?.documentId || !task?.id) return;
    state.processTasks.set(task.documentId, task);
    localStorage.setItem(taskStorageKey(task.documentId), task.id);
}

function renderTaskProgress(documentInfo = currentDocument()) {
    const panel = elements.processTaskStatus;
    panel.replaceChildren();
    const task = currentProcessTask(documentInfo);
    if (!documentInfo) {
        panel.className = 'task-progress empty';
        panel.textContent = '尚未选择文档';
        return;
    }
    if (!task) {
        panel.className = 'task-progress empty';
        panel.textContent = '尚未创建完整处理任务。';
        return;
    }

    const normalizedStatus = String(task.status || '').toLowerCase();
    panel.className = `task-progress ${normalizedStatus}`;
    if (task.status === 'FAILED' || task.status === 'CANCELLED') panel.classList.add('failed');
    if (task.status === 'SUCCEEDED') panel.classList.add('succeeded');

    const head = document.createElement('div');
    head.className = 'task-progress-head';
    const title = document.createElement('strong');
    title.textContent = taskStepLabel(task.currentStep);
    const stateLabel = document.createElement('span');
    stateLabel.textContent = taskStatusLabel(task.status);
    head.append(title, stateLabel);

    const progress = Math.min(100, Math.max(0, Number(task.progress) || 0));
    const progressBar = document.createElement('div');
    progressBar.className = 'task-progress-bar';
    const progressValue = document.createElement('span');
    progressValue.style.width = `${progress}%`;
    progressBar.append(progressValue);

    const meta = document.createElement('div');
    meta.className = 'task-progress-meta';
    meta.textContent = `进度 ${progress}% · 已重试 ${Number(task.retryCount) || 0} 次`;
    panel.append(head, progressBar, meta);

    if (task.errorMessage) {
        const error = document.createElement('div');
        error.className = 'task-progress-error';
        error.textContent = task.errorMessage;
        panel.append(error);
    }
}

function stopTaskPolling() {
    if (taskPollTimer) window.clearTimeout(taskPollTimer);
    taskPollTimer = null;
}

async function fetchTaskProgress(documentInfo, taskId) {
    if (!state.activeLibrary || !documentInfo || !taskId) return null;
    const libraryId = state.activeLibrary.id;
    const task = await api(processTaskPath(documentInfo.id, taskId));
    if (state.activeLibrary?.id !== libraryId) return null;
    rememberProcessTask(task);
    return task;
}

function scheduleTaskPolling(documentInfo, taskId, delay = 1200) {
    stopTaskPolling();
    const libraryId = state.activeLibrary?.id;
    taskPollTimer = window.setTimeout(async () => {
        if (!libraryId || state.activeLibrary?.id !== libraryId || currentDocument()?.id !== documentInfo.id) return;
        try {
            const task = await fetchTaskProgress(documentInfo, taskId);
            if (!task) return;
            renderTaskProgress(documentInfo);
            updateDocumentActions();
            updateInspectorActions();
            if (isTaskActive(task)) {
                scheduleTaskPolling(documentInfo, taskId);
                return;
            }
            await loadDocuments(documentInfo.id);
            notify(task.status === 'SUCCEEDED' ? '文档处理已完成' : `文档任务${taskStatusLabel(task.status)}`, task.status === 'SUCCEEDED' ? 'success' : 'error');
        } catch (error) {
            renderTaskProgress(documentInfo);
            notify(`获取处理进度失败：${error.message}`, 'error');
        }
    }, delay);
}

async function restoreTaskProgress(documentInfo) {
    if (!documentInfo || !state.activeLibrary) {
        renderTaskProgress(documentInfo);
        return;
    }
    const cachedTask = currentProcessTask(documentInfo);
    const taskId = cachedTask?.id || localStorage.getItem(taskStorageKey(documentInfo.id));
    if (!taskId) {
        renderTaskProgress(documentInfo);
        return;
    }
    try {
        const task = await fetchTaskProgress(documentInfo, taskId);
        if (!task || currentDocument()?.id !== documentInfo.id) return;
        renderTaskProgress(documentInfo);
        updateDocumentActions();
        updateInspectorActions();
        if (isTaskActive(task)) scheduleTaskPolling(documentInfo, task.id);
    } catch {
        // 任务可能已被清理；不影响文档的正常选择和手动处理。
        state.processTasks.delete(documentInfo.id);
        localStorage.removeItem(taskStorageKey(documentInfo.id));
        if (currentDocument()?.id === documentInfo.id) renderTaskProgress(documentInfo);
    }
}

function documentApiPath(documentId) {
    return `/api/knowledge-bases/${encodeURIComponent(state.activeLibrary.id)}`
        + `/documents/${encodeURIComponent(documentId)}`;
}

function formatDateTime(value) {
    if (!value) return '—';
    return new Intl.DateTimeFormat('zh-CN', {
        month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit'
    }).format(new Date(value));
}

function clearInspector() {
    state.documentDetail = null;
    state.chunkPage = { page: 1, size: 5, total: 0, records: [] };
    state.taskHistoryPage = { page: 1, size: 5, total: 0, records: [] };
    elements.inspectorTitle.textContent = '选择文档查看详情';
    elements.inspectorStatus.textContent = '未选择文档';
    elements.documentDetail.className = 'inspector-empty';
    elements.documentDetail.textContent = '选择文档后加载文件详情。';
    elements.chunkList.className = 'inspector-empty';
    elements.chunkList.textContent = '暂无可展示的 Chunk。';
    elements.taskHistory.className = 'inspector-empty';
    elements.taskHistory.textContent = '暂无处理任务历史。';
    elements.chunksPage.textContent = '—';
    elements.historyPage.textContent = '—';
    elements.reprocessButton.disabled = true;
    elements.chunksPrev.disabled = true;
    elements.chunksNext.disabled = true;
    elements.historyPrev.disabled = true;
    elements.historyNext.disabled = true;
}

async function loadInspector(documentInfo) {
    if (!state.activeLibrary || !documentInfo) return clearInspector();
    const libraryId = state.activeLibrary.id;
    const documentId = documentInfo.id;
    state.documentDetail = null;
    state.chunkPage = { page: 1, size: 5, total: 0, records: [] };
    state.taskHistoryPage = { page: 1, size: 5, total: 0, records: [] };
    elements.inspectorTitle.textContent = documentInfo.originalName || '未命名文档';
    elements.inspectorStatus.textContent = '正在加载详情…';
    elements.documentDetail.className = 'inspector-empty';
    elements.documentDetail.textContent = '正在读取文件详情…';
    elements.chunkList.className = 'inspector-empty';
    elements.chunkList.textContent = '正在读取文本片段…';
    elements.taskHistory.className = 'inspector-empty';
    elements.taskHistory.textContent = '正在读取处理历史…';
    updateInspectorActions();

    const [detail, chunks, history] = await Promise.all([
        api(documentApiPath(documentId)),
        api(`${documentApiPath(documentId)}/chunks?page=1&size=${state.chunkPage.size}`),
        api(`${documentApiPath(documentId)}/process-tasks?page=1&size=${state.taskHistoryPage.size}`)
    ]);
    if (state.activeLibrary?.id !== libraryId || currentDocument()?.id !== documentId) return;
    state.documentDetail = detail;
    state.chunkPage = chunks;
    state.taskHistoryPage = history;
    renderInspector();
}

async function loadChunkPage(page) {
    const documentInfo = currentDocument();
    if (!documentInfo || !state.activeLibrary) return;
    elements.chunkList.className = 'inspector-empty';
    elements.chunkList.textContent = '正在读取文本片段…';
    const result = await api(`${documentApiPath(documentInfo.id)}/chunks?page=${page}&size=${state.chunkPage.size}`);
    if (currentDocument()?.id !== documentInfo.id) return;
    state.chunkPage = result;
    renderInspector();
}

async function loadTaskHistoryPage(page) {
    const documentInfo = currentDocument();
    if (!documentInfo || !state.activeLibrary) return;
    elements.taskHistory.className = 'inspector-empty';
    elements.taskHistory.textContent = '正在读取处理历史…';
    const result = await api(`${documentApiPath(documentInfo.id)}/process-tasks?page=${page}&size=${state.taskHistoryPage.size}`);
    if (currentDocument()?.id !== documentInfo.id) return;
    state.taskHistoryPage = result;
    renderInspector();
}

function detailItem(label, value) {
    const item = document.createElement('div');
    item.className = 'detail-item';
    const key = document.createElement('span');
    key.textContent = label;
    const content = document.createElement('strong');
    content.textContent = value == null || value === '' ? '—' : value;
    item.append(key, content);
    return item;
}

function renderInspector() {
    const detail = state.documentDetail;
    const documentInfo = currentDocument();
    if (!documentInfo) return clearInspector();
    elements.inspectorTitle.textContent = detail?.originalName || documentInfo.originalName || '未命名文档';
    elements.inspectorStatus.textContent = statusLabel(detail?.status || documentInfo.status);

    elements.documentDetail.replaceChildren();
    if (!detail) {
        elements.documentDetail.className = 'inspector-empty';
        elements.documentDetail.textContent = '文件详情加载失败。';
    } else {
        elements.documentDetail.className = 'detail-grid';
        const values = [
            ['状态', statusLabel(detail.status)],
            ['文件大小', formatBytes(detail.sizeBytes)],
            ['解析格式', detail.parsedFormat],
            ['文本字符', detail.parsedCharCount == null ? null : String(detail.parsedCharCount)],
            ['结构块', detail.parsedBlockCount == null ? null : String(detail.parsedBlockCount)],
            ['页数', detail.pageCount == null ? null : String(detail.pageCount)],
            ['切分策略', detail.chunkStrategy],
            ['Chunk 配置', detail.chunkSize == null ? null : `${detail.chunkSize} / overlap ${detail.chunkOverlap ?? 0}`],
            ['向量模型', detail.embeddingModel],
            ['最近索引', formatDateTime(detail.indexedAt)],
            ['创建时间', formatDateTime(detail.createdAt)],
            ['更新时间', formatDateTime(detail.updatedAt)]
        ];
        values.forEach(([label, value]) => elements.documentDetail.append(detailItem(label, value)));
        if (detail.parsedPreview) {
            const preview = document.createElement('div');
            preview.className = 'detail-preview';
            const label = document.createElement('span');
            label.textContent = '解析预览';
            const text = document.createElement('p');
            text.textContent = detail.parsedPreview;
            preview.append(label, text);
            elements.documentDetail.append(preview);
        }
        if (detail.errorMessage) {
            const error = document.createElement('div');
            error.className = 'detail-error';
            error.textContent = `处理错误：${detail.errorMessage}`;
            elements.documentDetail.append(error);
        }
    }

    renderChunkPage();
    renderTaskHistory();
    updateInspectorActions();
}

function renderChunkPage() {
    const data = state.chunkPage;
    elements.chunkList.replaceChildren();
    const totalPages = Math.max(1, Math.ceil((Number(data.total) || 0) / data.size));
    elements.chunksPage.textContent = data.total ? `${data.page} / ${totalPages} · ${data.total} 段` : '0 段';
    if (!data.records?.length) {
        elements.chunkList.className = 'inspector-empty';
        elements.chunkList.textContent = '该文档尚未生成 Chunk。';
        return;
    }
    elements.chunkList.className = 'chunk-inspector-list';
    data.records.forEach(chunk => {
        const item = document.createElement('article');
        item.className = 'chunk-inspector-item';
        const head = document.createElement('div');
        head.className = 'chunk-inspector-head';
        const title = document.createElement('strong');
        title.textContent = `Chunk ${chunk.chunkIndex ?? '—'}`;
        const meta = document.createElement('span');
        const values = [];
        if (chunk.sectionTitle) values.push(chunk.sectionTitle);
        if (chunk.pageNumber != null) values.push(`第 ${chunk.pageNumber} 页`);
        values.push(`${chunk.charCount ?? 0} 字符`);
        meta.textContent = values.join(' · ');
        const content = document.createElement('p');
        content.textContent = chunk.content || '该 Chunk 没有可展示的文本。';
        head.append(title, meta);
        item.append(head, content);
        elements.chunkList.append(item);
    });
}

function renderTaskHistory() {
    const data = state.taskHistoryPage;
    elements.taskHistory.replaceChildren();
    const totalPages = Math.max(1, Math.ceil((Number(data.total) || 0) / data.size));
    elements.historyPage.textContent = data.total ? `${data.page} / ${totalPages} · ${data.total} 次` : '0 次';
    if (!data.records?.length) {
        elements.taskHistory.className = 'inspector-empty';
        elements.taskHistory.textContent = '尚未创建处理任务。';
        return;
    }
    elements.taskHistory.className = 'task-history-list';
    data.records.forEach(task => {
        const item = document.createElement('article');
        item.className = `history-item ${String(task.status || '').toLowerCase()}`;
        const head = document.createElement('div');
        head.className = 'history-item-head';
        const title = document.createElement('strong');
        title.textContent = `${taskStepLabel(task.currentStep)} · ${taskStatusLabel(task.status)}`;
        const percent = document.createElement('span');
        percent.textContent = `${task.progress ?? 0}%`;
        head.append(title, percent);
        const meta = document.createElement('p');
        meta.textContent = `${formatDateTime(task.createdAt)} · 重试 ${task.retryCount ?? 0} 次`;
        item.append(head, meta);
        if (task.errorMessage) {
            const error = document.createElement('small');
            error.textContent = task.errorMessage;
            item.append(error);
        }
        elements.taskHistory.append(item);
    });
}

function updateInspectorActions() {
    const documentInfo = currentDocument();
    const active = isTaskActive(currentProcessTask(documentInfo));
    const chunkData = state.chunkPage;
    const historyData = state.taskHistoryPage;
    elements.reprocessButton.disabled = !documentInfo || active;
    elements.reprocessButton.textContent = active ? '任务处理中…' : '重新处理';
    elements.chunksPrev.disabled = !documentInfo || chunkData.page <= 1;
    elements.chunksNext.disabled = !documentInfo || !chunkData.total
        || chunkData.page >= Math.ceil(chunkData.total / chunkData.size);
    elements.historyPrev.disabled = !documentInfo || historyData.page <= 1;
    elements.historyNext.disabled = !documentInfo || !historyData.total
        || historyData.page >= Math.ceil(historyData.total / historyData.size);
}

function renderDocumentList() {
    elements.documentList.replaceChildren();
    elements.documentCount.textContent = `${state.documents.length} 个文档`;
    elements.refreshDocuments.disabled = !state.activeLibrary;

    if (!state.activeLibrary || !state.documents.length) {
        const empty = document.createElement('div');
        empty.className = 'document-list-empty';
        empty.textContent = state.activeLibrary
            ? '这个知识库还没有文档，从上方上传第一份资料。'
            : '选择知识库后显示已上传文档。';
        elements.documentList.append(empty);
        return;
    }

    const selectedId = elements.documentId.value;
    state.documents.forEach(documentInfo => {
        const row = document.createElement('article');
        row.className = `document-row${documentInfo.id === selectedId ? ' selected' : ''}`;

        const selectButton = document.createElement('button');
        selectButton.type = 'button';
        selectButton.className = 'document-select';
        selectButton.setAttribute('aria-pressed', String(documentInfo.id === selectedId));

        const type = document.createElement('span');
        type.className = 'document-type';
        type.textContent = (documentInfo.fileExtension || 'FILE').toUpperCase();

        const body = document.createElement('span');
        body.className = 'document-file-body';
        const name = document.createElement('strong');
        name.textContent = documentInfo.originalName || '未命名文档';
        const meta = document.createElement('small');
        const details = [
            formatBytes(documentInfo.sizeBytes),
            formatDate(documentInfo.createdAt)
        ];
        if (documentInfo.chunkCount != null) details.push(`${documentInfo.chunkCount} Chunk`);
        if (documentInfo.vectorCount != null) details.push(`${documentInfo.vectorCount} 向量`);
        meta.textContent = details.join(' · ');
        body.append(name, meta);

        const status = document.createElement('span');
        const normalizedStatus = String(documentInfo.status || '').toLowerCase();
        status.className = `document-status ${normalizedStatus}`;
        status.textContent = statusLabel(documentInfo.status);
        selectButton.append(type, body, status);
        selectButton.addEventListener('click', () => selectDocument(documentInfo.id));

        const deleteButton = document.createElement('button');
        deleteButton.type = 'button';
        deleteButton.className = 'document-delete';
        deleteButton.textContent = '删除';
        deleteButton.setAttribute('aria-label', `删除文档 ${documentInfo.originalName || documentInfo.id}`);
        deleteButton.addEventListener('click', () => deleteDocument(documentInfo, deleteButton));

        row.append(selectButton, deleteButton);
        elements.documentList.append(row);
    });
}

async function deleteDocument(documentInfo, button) {
    if (!state.activeLibrary) return;
    const confirmed = window.confirm(`确定删除“${documentInfo.originalName || documentInfo.id}”吗？\n关联的 Chunk 和向量也会一并删除。`);
    if (!confirmed) return;

    await withBusy(button, async () => {
        await api(
            `/api/knowledge-bases/${encodeURIComponent(state.activeLibrary.id)}/documents/${encodeURIComponent(documentInfo.id)}`,
            { method: 'DELETE' }
        );
        state.processTasks.delete(documentInfo.id);
        localStorage.removeItem(taskStorageKey(documentInfo.id));
        if (elements.documentId.value === documentInfo.id) stopTaskPolling();
        await loadDocuments();
        notify(`“${documentInfo.originalName || '文档'}”已删除`);
    }).catch(error => notify(error.message, 'error'));
}

function renderDocument(documentInfo) {
    elements.documentSummary.replaceChildren();
    if (!documentInfo) {
        elements.documentSummary.className = 'document-summary empty';
        elements.documentSummary.textContent = '尚未选择文档';
        setPipeline('upload');
        renderTaskProgress(null);
        updateDocumentActions();
        return;
    }
    elements.documentSummary.className = 'document-summary';
    const title = document.createElement('strong');
    title.textContent = documentInfo.originalName || '已有文档';
    const idLine = document.createElement('span');
    idLine.textContent = `ID：${documentInfo.id}`;
    const stateLine = document.createElement('span');
    const extras = [];
    if (documentInfo.chunkCount != null) extras.push(`${documentInfo.chunkCount} 个 Chunk`);
    if (documentInfo.vectorCount != null) extras.push(`${documentInfo.vectorCount} 条向量`);
    stateLine.textContent = `状态：${documentInfo.status || 'UNKNOWN'}${extras.length ? ` · ${extras.join(' · ')}` : ''}`;
    elements.documentSummary.append(title, idLine, document.createElement('br'), stateLine);
    setPipeline(documentInfo.status);
    renderTaskProgress(documentInfo);
    updateDocumentActions();
}

function setPipeline(status) {
    const normalized = String(status || '').toUpperCase();
    const level = ['INDEXED', 'INDEXING', 'INDEX_FAILED'].includes(normalized) ? 3
        : normalized === 'CHUNKED' ? 2
        : ['PARSED', 'UPLOADED', 'UNKNOWN', 'UPLOAD'].includes(normalized) ? 1 : 1;
    document.querySelectorAll('.pipeline-step').forEach((step, index) => {
        step.classList.toggle('done', index + 1 < level);
        step.classList.toggle('current', index + 1 === level);
    });
    document.querySelectorAll('.pipeline-link').forEach((link, index) => link.classList.toggle('done', index + 1 < level));
}

function updateDocumentActions() {
    const enabled = Boolean(state.activeLibrary && currentDocument());
    const taskActive = isTaskActive(currentProcessTask());
    elements.processButton.disabled = !enabled || taskActive;
    elements.processButton.textContent = taskActive ? '后台处理中…' : '开始完整处理';
    elements.chunkButton.disabled = !enabled || taskActive;
    elements.indexButton.disabled = !enabled || taskActive;
}

function switchView(name) {
    document.querySelectorAll('.view-tab').forEach(tab => tab.classList.toggle('active', tab.dataset.view === name));
    document.querySelectorAll('.view').forEach(view => view.classList.toggle('active', view.id === `view-${name}`));
}

function sourceChunkDetails(chunks) {
    const details = document.createElement('details');
    details.className = 'source-details';
    details.open = true;

    const summary = document.createElement('summary');
    summary.textContent = `上下文详情 · ${chunks.length} 个 Chunk`;
    details.append(summary);

    const chunkList = document.createElement('div');
    chunkList.className = 'source-chunks';
    chunks.forEach(chunk => {
        const item = document.createElement('section');
        item.className = `source-chunk${chunk.matched ? ' matched' : ''}`;

        const head = document.createElement('div');
        head.className = 'source-chunk-head';
        const index = document.createElement('strong');
        index.textContent = chunk.chunkIndex == null ? 'Chunk' : `Chunk ${chunk.chunkIndex}`;
        const badge = document.createElement('span');
        badge.className = `chunk-badge${chunk.matched ? ' matched' : ''}`;
        // matched 表示该 Chunk 是混合检索与排序后选出的核心片段，不限定为向量检索命中。
        badge.textContent = chunk.matched ? '核心检索命中' : '相邻上下文';
        head.append(index, badge);

        const metadata = [];
        if (chunk.selectTitle) metadata.push(chunk.selectTitle);
        if (chunk.pageNumber != null) metadata.push(`第 ${chunk.pageNumber} 页`);
        if (metadata.length) {
            const meta = document.createElement('div');
            meta.className = 'source-chunk-meta';
            meta.textContent = metadata.join(' · ');
            item.append(head, meta);
        } else {
            item.append(head);
        }

        const content = document.createElement('p');
        content.className = 'source-chunk-content';
        content.textContent = chunk.content || '该 Chunk 没有可展示的文本。';
        item.append(content);
        chunkList.append(item);
    });

    details.append(chunkList);
    return details;
}

function hitCard(hit, label) {
    const card = document.createElement('article');
    card.className = 'hit';
    const top = document.createElement('div');
    top.className = 'hit-top';
    const title = document.createElement('div');
    title.className = 'hit-title';
    title.textContent = `${label || `#${hit.rank}`} · ${hit.documentName || '未命名文档'}`;
    const score = document.createElement('span');
    score.className = 'hit-score';
    score.textContent = hit.score == null ? 'NO SCORE' : Number(hit.score).toFixed(4);
    top.append(title, score);
    const meta = document.createElement('div');
    meta.className = 'hit-meta';
    const details = [];
    if (hit.sectionTitle) details.push(hit.sectionTitle);
    if (hit.pageNumber != null) details.push(`第 ${hit.pageNumber} 页`);
    if (hit.chunkIndex != null) details.push(`Chunk ${hit.chunkIndex}`);
    if (hit.contextStartIndex != null && hit.contextEndIndex != null) {
        details.push(hit.contextStartIndex === hit.contextEndIndex
            ? `上下文 Chunk ${hit.contextStartIndex}`
            : `上下文 Chunk ${hit.contextStartIndex}–${hit.contextEndIndex}`);
    }
    if (hit.matchedChunkIndex != null) details.push(`命中 Chunk ${hit.matchedChunkIndex}`);
    meta.textContent = details.join(' · ') || `文档 ID：${hit.documentId || '-'}`;
    card.append(top, meta);

    if (Array.isArray(hit.chunks) && hit.chunks.length) {
        card.append(sourceChunkDetails(hit.chunks));
    } else {
        const content = document.createElement('p');
        content.className = 'hit-content';
        content.textContent = hit.content || '';
        card.append(content);
    }
    return card;
}

function renderRetrieval(data) {
    elements.retrievalOutput.className = 'output-panel';
    elements.retrievalOutput.replaceChildren();
    const head = document.createElement('div');
    head.className = 'result-head';
    const title = document.createElement('h3');
    title.textContent = '检索结果';
    const meta = document.createElement('span');
    meta.textContent = `${data.resultCount} 条命中 · TopK ${data.topK} · 阈值 ${data.similarityThreshold}`;
    head.append(title, meta);
    const list = document.createElement('div');
    list.className = 'result-list';
    if (!data.hits?.length) {
        const notice = document.createElement('div');
        notice.className = 'notice';
        notice.textContent = '没有达到相似度阈值的片段。可以降低阈值，或检查文档是否已完成索引。';
        list.append(notice);
    } else {
        data.hits.forEach(hit => list.append(hitCard(hit)));
    }
    elements.retrievalOutput.append(head, list);
}

function appendHighlightedText(parent, value, query) {
    const text = String(value || '');
    const keyword = String(query || '').trim();
    if (!keyword) {
        parent.textContent = text;
        return;
    }

    const normalizedText = text.toLocaleLowerCase();
    const normalizedKeyword = keyword.toLocaleLowerCase();
    let cursor = 0;
    let matchIndex = normalizedText.indexOf(normalizedKeyword);
    while (matchIndex !== -1) {
        if (matchIndex > cursor) parent.append(document.createTextNode(text.slice(cursor, matchIndex)));
        const mark = document.createElement('mark');
        mark.className = 'keyword-highlight';
        mark.textContent = text.slice(matchIndex, matchIndex + keyword.length);
        parent.append(mark);
        cursor = matchIndex + keyword.length;
        matchIndex = normalizedText.indexOf(normalizedKeyword, cursor);
    }
    if (cursor < text.length) parent.append(document.createTextNode(text.slice(cursor)));
}

function keywordHitCard(hit, query) {
    const card = document.createElement('article');
    card.className = 'hit keyword-hit';

    const top = document.createElement('div');
    top.className = 'hit-top';
    const title = document.createElement('div');
    title.className = 'hit-title';
    title.append(document.createTextNode(`#${hit.rank ?? '—'} · `));
    appendHighlightedText(title, hit.documentName || '未命名文档', query);
    const score = document.createElement('span');
    score.className = 'hit-score keyword-score';
    score.textContent = hit.score == null ? '无匹配分' : `匹配 ${Number(hit.score).toFixed(4)}`;
    top.append(title, score);

    const meta = document.createElement('div');
    meta.className = 'hit-meta keyword-hit-meta';
    const location = [];
    if (hit.sectionTitle) location.push(hit.sectionTitle);
    if (hit.pageNumber != null) location.push(`第 ${hit.pageNumber} 页`);
    if (hit.chunkIndex != null) location.push(`Chunk ${hit.chunkIndex}`);
    if (location.length) {
        appendHighlightedText(meta, location.join(' · '), query);
    } else {
        meta.textContent = `文档 ID：${hit.documentId || '-'}`;
    }

    const content = document.createElement('p');
    content.className = 'hit-content keyword-hit-content';
    appendHighlightedText(content, hit.content || '', query);
    card.append(top, meta, content);
    return card;
}

function renderKeywordSearch(data) {
    elements.keywordSearchOutput.className = 'output-panel keyword-output';
    elements.keywordSearchOutput.replaceChildren();

    const head = document.createElement('div');
    head.className = 'result-head keyword-result-head';
    const heading = document.createElement('div');
    const label = document.createElement('span');
    label.className = 'result-eyebrow';
    label.textContent = 'LITERAL MATCHES';
    const title = document.createElement('h3');
    title.textContent = `“${data.query || ''}”`;
    heading.append(label, title);
    const meta = document.createElement('span');
    meta.textContent = `${data.resultCount ?? data.results?.length ?? 0} 条命中 · TopK ${data.topK}`;
    head.append(heading, meta);

    const list = document.createElement('div');
    list.className = 'result-list keyword-result-list';
    if (!data.results?.length) {
        const notice = document.createElement('div');
        notice.className = 'notice';
        notice.textContent = '没有找到包含该关键词的片段。可以尝试更短的词组，或检查文档是否已完成索引。';
        list.append(notice);
    } else {
        data.results.forEach(hit => list.append(keywordHitCard(hit, data.query)));
    }
    elements.keywordSearchOutput.append(head, list);
}

function appendInlineMarkdown(parent, source) {
    const pattern = /(`[^`]+`|\*\*[^*]+\*\*|~~[^~]+~~|\*[^*]+\*|\[[^\]]+\]\([^\s)]+\))/g;
    let cursor = 0;
    for (const match of source.matchAll(pattern)) {
        if (match.index > cursor) {
            parent.append(document.createTextNode(source.slice(cursor, match.index)));
        }
        const token = match[0];
        if (token.startsWith('`')) {
            const code = document.createElement('code');
            code.textContent = token.slice(1, -1);
            parent.append(code);
        } else if (token.startsWith('**')) {
            const strong = document.createElement('strong');
            strong.textContent = token.slice(2, -2);
            parent.append(strong);
        } else if (token.startsWith('~~')) {
            const deleted = document.createElement('del');
            deleted.textContent = token.slice(2, -2);
            parent.append(deleted);
        } else if (token.startsWith('*')) {
            const emphasis = document.createElement('em');
            emphasis.textContent = token.slice(1, -1);
            parent.append(emphasis);
        } else {
            const linkMatch = token.match(/^\[([^\]]+)]\(([^\s)]+)\)$/);
            const url = linkMatch?.[2] || '';
            if (/^(https?:|mailto:)/i.test(url)) {
                const link = document.createElement('a');
                link.href = url;
                link.target = '_blank';
                link.rel = 'noopener noreferrer';
                link.textContent = linkMatch[1];
                parent.append(link);
            } else {
                parent.append(document.createTextNode(token));
            }
        }
        cursor = match.index + token.length;
    }
    if (cursor < source.length) {
        parent.append(document.createTextNode(source.slice(cursor)));
    }
}

function isTableDivider(line) {
    const cells = line.trim().replace(/^\||\|$/g, '').split('|');
    return cells.length > 1 && cells.every(cell => /^\s*:?-{3,}:?\s*$/.test(cell));
}

function tableCells(line) {
    return line.trim().replace(/^\||\|$/g, '').split('|').map(cell => cell.trim());
}

function renderMarkdown(markdown) {
    const fragment = document.createDocumentFragment();
    const lines = String(markdown || '').replace(/\r\n?/g, '\n').split('\n');
    let index = 0;

    while (index < lines.length) {
        const line = lines[index];
        if (!line.trim()) {
            index++;
            continue;
        }

        const fence = line.match(/^\s*```\s*([\w.+-]*)\s*$/);
        if (fence) {
            const block = document.createElement('div');
            block.className = 'markdown-code-block';
            if (fence[1]) {
                const language = document.createElement('div');
                language.className = 'markdown-code-language';
                language.textContent = fence[1];
                block.append(language);
            }
            const pre = document.createElement('pre');
            const code = document.createElement('code');
            const content = [];
            index++;
            while (index < lines.length && !/^\s*```\s*$/.test(lines[index])) {
                content.push(lines[index++]);
            }
            if (index < lines.length) index++;
            code.textContent = content.join('\n');
            pre.append(code);
            block.append(pre);
            fragment.append(block);
            continue;
        }

        const heading = line.match(/^(#{1,6})\s+(.+)$/);
        if (heading) {
            const level = Math.min(heading[1].length + 1, 6);
            const element = document.createElement(`h${level}`);
            appendInlineMarkdown(element, heading[2].trim());
            fragment.append(element);
            index++;
            continue;
        }

        if (/^\s*(---+|___+|\*\*\*+)\s*$/.test(line)) {
            fragment.append(document.createElement('hr'));
            index++;
            continue;
        }

        if (index + 1 < lines.length && line.includes('|') && isTableDivider(lines[index + 1])) {
            const tableWrap = document.createElement('div');
            tableWrap.className = 'markdown-table-wrap';
            const table = document.createElement('table');
            const thead = document.createElement('thead');
            const headRow = document.createElement('tr');
            tableCells(line).forEach(value => {
                const cell = document.createElement('th');
                appendInlineMarkdown(cell, value);
                headRow.append(cell);
            });
            thead.append(headRow);
            table.append(thead);
            index += 2;
            const tbody = document.createElement('tbody');
            while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
                const row = document.createElement('tr');
                tableCells(lines[index]).forEach(value => {
                    const cell = document.createElement('td');
                    appendInlineMarkdown(cell, value);
                    row.append(cell);
                });
                tbody.append(row);
                index++;
            }
            table.append(tbody);
            tableWrap.append(table);
            fragment.append(tableWrap);
            continue;
        }

        if (/^\s*>\s?/.test(line)) {
            const quote = document.createElement('blockquote');
            while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
                if (quote.childNodes.length) quote.append(document.createElement('br'));
                appendInlineMarkdown(quote, lines[index].replace(/^\s*>\s?/, ''));
                index++;
            }
            fragment.append(quote);
            continue;
        }

        const unordered = line.match(/^\s*[-+*]\s+(.+)$/);
        const ordered = line.match(/^\s*\d+[.)]\s+(.+)$/);
        if (unordered || ordered) {
            const list = document.createElement(ordered ? 'ol' : 'ul');
            const matcher = ordered ? /^\s*\d+[.)]\s+(.+)$/ : /^\s*[-+*]\s+(.+)$/;
            while (index < lines.length) {
                const itemMatch = lines[index].match(matcher);
                if (!itemMatch) break;
                const item = document.createElement('li');
                appendInlineMarkdown(item, itemMatch[1]);
                list.append(item);
                index++;
            }
            fragment.append(list);
            continue;
        }

        const paragraph = document.createElement('p');
        while (index < lines.length && lines[index].trim()) {
            const nextLine = lines[index];
            if (paragraph.childNodes.length) paragraph.append(document.createElement('br'));
            appendInlineMarkdown(paragraph, nextLine.trim());
            index++;
            if (index < lines.length && (
                /^(#{1,6})\s+/.test(lines[index]) ||
                /^\s*```/.test(lines[index]) ||
                /^\s*>\s?/.test(lines[index]) ||
                /^\s*([-+*]|\d+[.)])\s+/.test(lines[index])
            )) break;
        }
        fragment.append(paragraph);
    }
    return fragment;
}

function renderChat(data) {
    elements.chatOutput.className = 'output-panel';
    elements.chatOutput.replaceChildren();
    const head = document.createElement('div');
    head.className = 'result-head';
    const title = document.createElement('h3');
    title.textContent = data.knowledgeFound ? '基于知识库的回答' : '未找到相关资料';
    const meta = document.createElement('span');
    meta.textContent = data.sources?.length ? `${data.sources.length} 条引用` : '无引用';
    head.append(title, meta);
    const body = document.createElement('div');
    body.className = data.knowledgeFound ? 'answer-body markdown-body' : 'notice';
    if (data.knowledgeFound) {
        body.append(renderMarkdown(data.answer || '模型未返回回答。'));
    } else {
        body.textContent = data.answer || '模型未返回回答。';
    }
    elements.chatOutput.append(head, body);
    if (data.sources?.length) {
        const heading = document.createElement('h4');
        heading.className = 'source-heading';
        heading.textContent = '引用来源';
        const list = document.createElement('div');
        list.className = 'source-list';
        data.sources.forEach(source => list.append(hitCard(source, source.referenceId)));
        elements.chatOutput.append(heading, list);
    }
    if (data.tokenUsage) {
        const usage = document.createElement('div');
        usage.className = 'usage';
        usage.textContent = `Token：输入 ${data.tokenUsage.promptTokens ?? '-'} / 输出 ${data.tokenUsage.completionTokens ?? '-'} / 总计 ${data.tokenUsage.totalTokens ?? '-'}`;
        elements.chatOutput.append(usage);
    }
}

function createStreamingChatView() {
    elements.chatOutput.className = 'output-panel is-streaming';
    elements.chatOutput.setAttribute('aria-busy', 'true');
    elements.chatOutput.replaceChildren();

    const head = document.createElement('div');
    head.className = 'result-head';
    const title = document.createElement('h3');
    title.textContent = '正在进行混合检索';
    const status = document.createElement('span');
    status.className = 'stream-status';
    status.textContent = '准备检索';
    head.append(title, status);

    const body = document.createElement('div');
    body.className = 'answer-body markdown-body streaming-answer';
    const placeholder = document.createElement('p');
    placeholder.className = 'stream-placeholder';
    placeholder.textContent = '正在通过向量与关键词检索查找资料…';
    body.append(placeholder);

    elements.chatOutput.append(head, body);
    return {
        title,
        status,
        body,
        answer: '',
        sources: [],
        usage: null,
        failed: false,
        done: false,
        renderFrame: null,
        sourceHeading: null,
        sourceList: null,
        usageElement: null,
        errorElement: null
    };
}

function renderStreamingAnswer(view, immediate = false) {
    const render = () => {
        view.renderFrame = null;
        const errorElement = view.errorElement;
        view.body.replaceChildren();
        if (view.answer) {
            view.body.append(renderMarkdown(view.answer));
        } else if (!view.failed) {
            const placeholder = document.createElement('p');
            placeholder.className = 'stream-placeholder';
            placeholder.textContent = '正在通过向量与关键词检索查找资料…';
            view.body.append(placeholder);
        }
        if (errorElement) view.body.append(errorElement);
    };

    if (immediate) {
        if (view.renderFrame != null) cancelAnimationFrame(view.renderFrame);
        render();
    } else if (view.renderFrame == null) {
        view.renderFrame = requestAnimationFrame(render);
    }
}

function renderStreamingSources(view) {
    if (view.sourceHeading) {
        view.sourceHeading.remove();
        view.sourceList.remove();
        view.sourceHeading = null;
        view.sourceList = null;
    }
    if (!view.sources.length) return;

    view.sourceHeading = document.createElement('h4');
    view.sourceHeading.className = 'source-heading';
    view.sourceHeading.textContent = '引用来源';
    view.sourceList = document.createElement('div');
    view.sourceList.className = 'source-list';
    view.sources.forEach(source => view.sourceList.append(hitCard(source, source.referenceId)));
    elements.chatOutput.append(view.sourceHeading, view.sourceList);
}

function renderStreamUsage(view) {
    if (!view.usage) return;
    if (!view.usageElement) {
        view.usageElement = document.createElement('div');
        view.usageElement.className = 'usage';
        elements.chatOutput.append(view.usageElement);
    }
    view.usageElement.textContent =
        `Token：输入 ${view.usage.promptTokens ?? '-'} / 输出 ${view.usage.completionTokens ?? '-'} / 总计 ${view.usage.totalTokens ?? '-'}`;
}

function finishStreamingChat(view) {
    if (view.done) return;
    view.done = true;
    renderStreamingAnswer(view, true);
    renderStreamUsage(view);
    elements.chatOutput.classList.remove('is-streaming');
    elements.chatOutput.setAttribute('aria-busy', 'false');
    view.status.classList.remove('stream-status');

    if (view.failed) {
        view.title.textContent = '回答生成失败';
        view.status.textContent = '已结束';
    } else {
        view.title.textContent = view.sources.length ? '基于知识库的回答' : '未找到相关资料';
        view.status.textContent = `${view.sources.length} 条引用 · 已完成`;
    }
}

function failStreamingChat(view, message) {
    view.failed = true;
    renderStreamingAnswer(view, true);
    if (!view.answer) view.body.className = 'notice';
    if (!view.errorElement) {
        view.errorElement = document.createElement('div');
        view.errorElement.className = view.answer ? 'stream-error' : '';
        view.body.append(view.errorElement);
    }
    view.errorElement.textContent = message || '回答生成失败，请稍后重试。';
}

function handleRagStreamEvent(view, message) {
    const data = message.data;
    if (!data || typeof data !== 'object') return;
    const type = data.type || message.event;

    if (type === 'sources' || type === 'source') {
        view.sources = Array.isArray(data.sources) ? data.sources : [];
        view.status.textContent = `${view.sources.length} 条引用 · 正在生成`;
        renderStreamingSources(view);
        return;
    }
    if (type === 'delta') {
        view.answer += data.content || '';
        view.title.textContent = view.sources.length ? '基于知识库的回答' : '正在生成回答';
        renderStreamingAnswer(view);
        return;
    }
    if (type === 'error') {
        failStreamingChat(view, data.content);
        return;
    }
    if (type === 'done') {
        view.usage = data.usage || null;
        finishStreamingChat(view);
    }
}

elements.createToggle.addEventListener('click', () => {
    const open = elements.createForm.hidden;
    elements.createForm.hidden = !open;
    elements.createToggle.setAttribute('aria-expanded', String(open));
    if (open) document.querySelector('#kb-name').focus();
});

document.querySelector('#create-cancel').addEventListener('click', () => {
    elements.createForm.hidden = true;
    elements.createToggle.setAttribute('aria-expanded', 'false');
});

elements.createForm.addEventListener('submit', async event => {
    event.preventDefault();
    const button = event.submitter;
    await withBusy(button, async () => {
        const form = new FormData(elements.createForm);
        await api('/api/knowledge-bases', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: form.get('name').trim(), description: form.get('description').trim() })
        });
        const name = form.get('name').trim();
        elements.createForm.reset();
        elements.createForm.hidden = true;
        elements.createToggle.setAttribute('aria-expanded', 'false');
        await loadLibraries();
        const created = state.libraries.find(item => item.name === name);
        if (created) await selectLibrary(created.id);
        notify('知识库已创建');
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#refresh-libraries').addEventListener('click', () => loadLibraries());
elements.refreshDocuments.addEventListener('click', () => {
    loadDocuments(currentDocument()?.id).catch(error => notify(error.message, 'error'));
});
document.querySelectorAll('.view-tab').forEach(tab => tab.addEventListener('click', () => switchView(tab.dataset.view)));

elements.fileInput.addEventListener('change', () => {
    elements.fileName.textContent = elements.fileInput.files[0]?.name
        || '选择 TXT、Markdown、PDF 或 Word 文件';
});

elements.uploadForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const file = elements.fileInput.files[0];
    if (!file) return notify('请选择要上传的文件', 'error');
    const button = event.submitter;
    await withBusy(button, async () => {
        const form = new FormData();
        form.append('file', file);
        const uploaded = await api(`/api/knowledge-bases/${state.activeLibrary.id}/documents`, { method: 'POST', body: form });
        elements.uploadForm.reset();
        clearFileSelection();
        await loadDocuments(uploaded.id);
        notify(`“${uploaded.originalName}”已上传并解析`);
    }).catch(error => notify(error.message, 'error'));
});

elements.processButton.addEventListener('click', async () => {
    if (!requireLibrary()) return;
    const documentInfo = currentDocument();
    if (!documentInfo) return notify('请从文件列表选择文档', 'error');

    await withBusy(elements.processButton, async () => {
        const task = await api(
            `/api/knowledge-bases/${encodeURIComponent(state.activeLibrary.id)}`
            + `/documents/${encodeURIComponent(documentInfo.id)}/process-tasks`,
            { method: 'POST' }
        );
        rememberProcessTask(task);
        renderTaskProgress(documentInfo);
        updateDocumentActions();
        updateInspectorActions();
        if (isTaskActive(task)) scheduleTaskPolling(documentInfo, task.id, 0);
        notify(task.status === 'PENDING' ? '处理任务已创建，等待后台执行' : `处理任务${taskStatusLabel(task.status)}`);
    }).catch(error => notify(error.message, 'error'));
});

elements.reprocessButton.addEventListener('click', async () => {
    if (!requireLibrary()) return;
    const documentInfo = currentDocument();
    if (!documentInfo) return notify('请从文件列表选择文档', 'error');
    const confirmed = window.confirm(`重新处理“${documentInfo.originalName || documentInfo.id}”会清除已有的解析结果、Chunk 和向量，是否继续？`);
    if (!confirmed) return;

    await withBusy(elements.reprocessButton, async () => {
        const task = await api(`${documentApiPath(documentInfo.id)}/process-tasks/reprocess`, { method: 'POST' });
        rememberProcessTask(task);
        renderTaskProgress(documentInfo);
        updateInspectorActions();
        if (isTaskActive(task)) scheduleTaskPolling(documentInfo, task.id, 0);
        await loadDocuments(documentInfo.id);
        notify('已创建重新处理任务');
    }).catch(error => notify(error.message, 'error'));
});

elements.chunksPrev.addEventListener('click', () => {
    loadChunkPage(state.chunkPage.page - 1).catch(error => notify(error.message, 'error'));
});
elements.chunksNext.addEventListener('click', () => {
    loadChunkPage(state.chunkPage.page + 1).catch(error => notify(error.message, 'error'));
});
elements.historyPrev.addEventListener('click', () => {
    loadTaskHistoryPage(state.taskHistoryPage.page - 1).catch(error => notify(error.message, 'error'));
});
elements.historyNext.addEventListener('click', () => {
    loadTaskHistoryPage(state.taskHistoryPage.page + 1).catch(error => notify(error.message, 'error'));
});

elements.chunkButton.addEventListener('click', async () => {
    if (!requireLibrary()) return;
    const document = currentDocument();
    if (!document) return notify('请从文件列表选择文档', 'error');
    await withBusy(elements.chunkButton, async () => {
        const count = await api(`/api/knowledge-bases/${state.activeLibrary.id}/documents/${encodeURIComponent(document.id)}/chunks`, { method: 'POST' });
        await loadDocuments(document.id);
        notify(`文本切分完成，共 ${count} 个 Chunk`);
    }).catch(error => notify(error.message, 'error'));
});

elements.indexButton.addEventListener('click', async () => {
    if (!requireLibrary()) return;
    const document = currentDocument();
    if (!document) return notify('请从文件列表选择文档', 'error');
    await withBusy(elements.indexButton, async () => {
        const indexed = await api(`/api/knowledge-bases/${state.activeLibrary.id}/documents/${encodeURIComponent(document.id)}/index`, { method: 'POST' });
        await loadDocuments(document.id);
        notify(`向量索引完成，共 ${indexed.vectorCount} 条`);
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#retrieval-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const form = new FormData(event.currentTarget);
    const button = event.submitter;
    await withBusy(button, async () => {
        const data = await api('/api/retrieval/search', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                knowledgeBaseId: state.activeLibrary.id,
                query: form.get('query').trim(),
                topK: Number(form.get('topK')),
                similarityThreshold: Number(form.get('similarityThreshold'))
            })
        });
        renderRetrieval(data);
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#keyword-search-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const form = new FormData(event.currentTarget);
    const query = String(form.get('query') || '').trim();
    if (query.length < 2) return notify('搜索关键词至少需要 2 个字符', 'error');
    const button = event.submitter;
    await withBusy(button, async () => {
        const data = await api('/api/retrieval/keyword-search', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                knowledgeBaseId: state.activeLibrary.id,
                query,
                topK: Number(form.get('topK'))
            })
        });
        renderKeywordSearch(data);
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#chat-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const form = new FormData(event.currentTarget);
    const modelCode = String(form.get('modelCode') || '').trim();
    if (!state.models.includes(modelCode)) {
        notify('请选择一个可用的生成模型', 'error');
        return;
    }
    const button = event.submitter;
    const streamView = createStreamingChatView();
    await withBusy(button, async () => {
        await streamSse('/api/rag/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            },
            body: JSON.stringify({
                knowledgeBaseId: state.activeLibrary.id,
                question: form.get('question').trim(),
                topK: Number(form.get('topK')),
                similarityThreshold: Number(form.get('similarityThreshold')),
                modelCode
            })
        }, message => handleRagStreamEvent(streamView, message));
        finishStreamingChat(streamView);
    }).catch(error => {
        failStreamingChat(streamView, error.message);
        finishStreamingChat(streamView);
        notify(error.message, 'error');
    });
});

elements.chatModel.addEventListener('change', () => {
    if (elements.chatModel.value) {
        localStorage.setItem('atlas-chat-model', elements.chatModel.value);
    }
});

document.querySelector('#chat-question').addEventListener('keydown', event => {
    if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
        document.querySelector('#chat-form').requestSubmit();
    }
});

updateDocumentActions();
loadModels();
loadLibraries();
