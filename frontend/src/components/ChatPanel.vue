<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { api } from "../services/api.js";
import { chatTitle } from "../services/chat.js";
import { useChatSession } from "../composables/useChatSession.js";
import HitCard from "./HitCard.vue";
import MarkdownContent from "./MarkdownContent.vue";

const props = defineProps({
  workspace: { type: Object, required: true },
  notify: { type: Function, required: true },
});
const models = ref([]);
const modelsLoading = ref(false);
const modelsError = ref("");
const formElement = ref(null);
const questionInput = ref(null);
const threadElement = ref(null);
const followLatest = ref(true);
const settingsOpen = ref(false);
const form = reactive({ question: "", modelCode: "", topK: 5, similarityThreshold: 0.5 });
const { turns, conversationId, loading, send, stop, reset } = useChatSession();
const canSend = computed(() => !!props.workspace.activeLibrary &&
  models.value.includes(form.modelCode) && !!form.question.trim() && !loading.value);
let modelsController;

function resetConversation() {
  reset();
  form.question = "";
  followLatest.value = true;
}
function newConversation() {
  resetConversation();
  nextTick(() => questionInput.value?.focus());
}
watch(() => props.workspace.activeLibrary?.id, resetConversation, { flush: "sync" });
watch(() => form.modelCode, (code) => {
  if (code) localStorage.setItem("atlas-chat-model", code);
});
function onScroll() {
  const el = threadElement.value;
  if (el) followLatest.value = el.scrollHeight - el.scrollTop - el.clientHeight < 64;
}
async function scrollToLatest() {
  await nextTick();
  const el = threadElement.value;
  if (el) el.scrollTop = el.scrollHeight;
  followLatest.value = true;
}
watch(() => [turns.value.length, turns.value.at(-1)?.answer,
  turns.value.at(-1)?.error, turns.value.at(-1)?.done, loading.value], async () => {
  if (followLatest.value) await scrollToLatest();
}, { flush: "post" });

async function loadModels() {
  if (modelsLoading.value) return;
  modelsController = new AbortController();
  modelsLoading.value = true;
  modelsError.value = "";
  try {
    const data = await api("/api/models", { signal: modelsController.signal });
    if (!Array.isArray(data) || !data.length) throw new Error("当前没有可用的聊天模型");
    models.value = data;
    const saved = localStorage.getItem("atlas-chat-model");
    form.modelCode = data.includes(saved) ? saved : data[0];
  } catch (error) {
    if (error.name !== "AbortError") modelsError.value = error.message;
  } finally {
    modelsLoading.value = false;
  }
}
onMounted(loadModels);
onBeforeUnmount(() => {
  reset();
  modelsController?.abort();
});

async function submit() {
  if (loading.value || !props.workspace.requireLibrary()) return;
  if (!models.value.includes(form.modelCode)) return props.notify("请选择一个可用的生成模型", "error");
  if (!form.question.trim()) return;
  // 快捷键与按钮共用校验，避免折叠字段无效时浏览器无法定位。
  if (!Number.isInteger(form.topK) || form.topK < 1 || form.topK > 20 ||
      !Number.isFinite(form.similarityThreshold) || form.similarityThreshold < 0 || form.similarityThreshold > 1) {
    settingsOpen.value = true;
    return props.notify("候选数需为 1–20 的整数，相似度阈值需在 0–1 之间", "error");
  }
  const payload = { ...form, question: form.question.trim(), knowledgeBaseId: props.workspace.activeLibrary.id };
  form.question = "";
  followLatest.value = true;
  await send(payload);
}
function shortcut(event) {
  if (!event.isComposing && event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
    event.preventDefault();
    if (canSend.value) formElement.value.requestSubmit();
  }
}
async function editQuestion(turn) {
  form.question = turn.question;
  await nextTick();
  questionInput.value?.focus();
}
</script>

<template>
  <section id="view-chat" class="view active chat-workspace">
    <header class="chat-toolbar">
      <div>
        <p class="section-kicker">知识问答</p>
        <h2>{{ conversationId ? "继续当前会话" : "开始一段新对话" }}</h2>
        <p class="chat-context">{{ workspace.activeLibrary?.name || "请先选择知识库" }} · {{ turns.length }} 轮提问</p>
      </div>
      <button class="button button-quiet" type="button" :disabled="loading || !turns.length" @click="newConversation">
        ＋ 新建会话
      </button>
    </header>
    <p class="chat-session-hint">追问会结合当前会话；新建会话后不携带此前上下文。页面刷新后不保留本地问答记录。</p>

    <div id="chat-output" ref="threadElement" class="chat-thread" :aria-busy="loading" @scroll="onScroll">
      <div v-if="!turns.length" class="chat-welcome">
        <span class="chat-welcome-mark" aria-hidden="true">问</span>
        <h3>从资料里找答案，也可以直接查数量</h3>
        <p>选择模型并发送问题，回答和引用会显示在这里。</p>
        <button class="button button-quiet" type="button" :disabled="!workspace.activeLibrary" @click="editQuestion({ question: '当前知识库有多少份文档？' })">
          试着问：当前知识库有多少份文档？
        </button>
      </div>
      <article v-for="(turn, index) in turns" :key="turn.id" class="chat-turn" :aria-label="'第 ' + (index + 1) + ' 轮问答'">
        <div class="chat-question">
          <span class="chat-role">你 · {{ index + 1 }}</span>
          <p>{{ turn.question }}</p>
        </div>
        <div class="chat-answer">
          <div class="chat-answer-head">
            <h3>{{ chatTitle(turn) }}</h3>
            <span :class="{ 'stream-status': loading && index === turns.length - 1 }">
              {{ turn.modelCode }} · {{ turn.cancelled ? "已停止" : turn.error ? "失败" : turn.done ? "已完成" : "处理中" }}
            </span>
          </div>
          <MarkdownContent v-if="turn.answer" class="answer-body" :content="turn.answer" />
          <p v-else-if="!turn.done && !turn.error && !turn.cancelled" class="stream-placeholder">
            {{ turn.mode === "TOOL" ? "正在查询知识库信息…" : "正在准备上下文并查找相关资料…" }}
          </p>
          <p v-if="turn.cancelled" class="chat-context">已停止接收回答；服务端可能已保存本轮消息。</p>
          <div v-if="turn.error" class="stream-error" role="alert">{{ turn.error }}</div>
          <button v-if="turn.error || turn.cancelled" class="button button-quiet chat-edit" type="button" :disabled="loading" @click="editQuestion(turn)">
            编辑问题后重发
          </button>
          <details v-if="turn.sources.length" class="chat-details">
            <summary>引用资料 · {{ turn.sources.length }} 条</summary>
            <div class="source-list">
              <HitCard v-for="(source, sourceIndex) in turn.sources" :key="source.referenceId || sourceIndex" :hit="source" :label="source.referenceId" />
            </div>
          </details>
          <details v-if="turn.trace" class="chat-details">
            <summary>会话追踪</summary>
            <p v-if="turn.conversationId">会话 ID：{{ turn.conversationId }}</p>
            <template v-if="turn.trace.conversation">
              <p>独立问题：{{ turn.trace.conversation.standaloneQuery }}</p>
              <p>问题改写：{{ turn.trace.conversation.rewritten ? "是" : "否" }} · 使用历史：{{ turn.trace.conversation.historyUsed ? "是" : "否" }}</p>
            </template>
          </details>
          <div v-if="turn.usage" class="usage">
            Token：输入 {{ turn.usage.promptTokens ?? "-" }} / 输出 {{ turn.usage.completionTokens ?? "-" }} / 总计 {{ turn.usage.totalTokens ?? "-" }}
          </div>
        </div>
      </article>
    </div>
    <button v-if="!followLatest && turns.length" class="button button-quiet chat-jump" type="button" @click="scrollToLatest">↓ 回到最新回答</button>

    <form id="chat-form" ref="formElement" class="chat-input-panel" novalidate @submit.prevent="submit">
      <div class="chat-input-head">
        <label for="chat-question">{{ conversationId ? "继续追问" : "你的问题" }}</label>
        <span>{{ form.question.length }} / 1000</span>
      </div>
      <textarea id="chat-question" ref="questionInput" v-model="form.question" maxlength="1000" rows="3" :disabled="!workspace.activeLibrary"
        :placeholder="conversationId ? '继续提问，将结合当前会话回答…' : '询问资料内容，或查询当前知识库的文档数量…'" @keydown="shortcut" />
      <div class="chat-compose-actions">
        <label class="chat-model-label" for="chat-model">生成模型
          <select id="chat-model" v-model="form.modelCode" :disabled="!models.length || loading">
            <option v-if="!models.length" value="">{{ modelsLoading ? "正在加载模型…" : "暂无可用模型" }}</option>
            <option v-for="model in models" :key="model" :value="model">{{ model }}</option>
          </select>
        </label>
        <button class="button button-quiet" type="button" :aria-expanded="settingsOpen" aria-controls="chat-settings" @click="settingsOpen = !settingsOpen">检索设置 {{ settingsOpen ? '−' : '＋' }}</button>
        <div class="chat-send-actions">
          <button v-if="loading" class="button button-stop" type="button" @click="stop">停止生成</button>
          <button v-else id="chat-submit" class="button button-primary" type="submit" :disabled="!canSend">{{ conversationId ? "发送追问 ↑" : "发送问题 ↑" }}</button>
        </div>
      </div>
      <div v-if="modelsError" class="chat-model-error" role="alert">
        <span>{{ modelsError }}</span>
        <button class="button button-quiet" type="button" :disabled="modelsLoading" @click="loadModels">重新加载模型</button>
      </div>
      <div v-show="settingsOpen" id="chat-settings" class="chat-settings">
        <label>最终候选数<input v-model.number="form.topK" type="number" min="1" max="20" step="1" :disabled="loading" /></label>
        <label>向量相似度阈值<input v-model.number="form.similarityThreshold" type="number" min="0" max="1" step="0.05" :disabled="loading" /></label>
        <p>仅影响基于资料的检索回答，文档数量查询不使用这些参数。</p>
      </div>
      <p class="chat-composer-hint">{{ loading ? "正在生成回答，你可以先编辑下一条问题。" : "⌘ / Ctrl + Enter 发送，Enter 换行" }}</p>
    </form>
  </section>
</template>

<style scoped>
.chat-workspace { min-width: 0; }
.chat-toolbar, .chat-input-head, .chat-answer-head, .chat-compose-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.chat-toolbar h2 { margin: 0; font: 600 27px Georgia, "Songti SC", serif; }
.chat-context, .chat-session-hint, .chat-composer-hint { color: var(--slate); font-size: 12px; line-height: 1.65; }
.chat-context { margin: 8px 0 0; }
.chat-session-hint { margin: 12px 0 18px; }
.chat-thread { min-height: 300px; max-height: 58vh; overflow-y: auto; overscroll-behavior: contain; border: 1px solid var(--line); border-radius: 10px 10px 0 0; background: var(--paper); scrollbar-gutter: stable; }
.chat-welcome { display: grid; justify-items: center; align-content: center; min-height: 300px; padding: 28px; text-align: center; gap: 12px; }
.chat-welcome-mark { display: grid; place-items: center; width: 48px; height: 48px; border-radius: 14px 14px 3px 14px; color: var(--teal); background: var(--teal-soft); font: 24px "Songti SC", serif; }
.chat-welcome h3 { margin: 0; font-size: 18px; }
.chat-welcome p { margin: 0; color: var(--slate); font-size: 13px; }
.chat-turn { padding: 24px; border-bottom: 1px solid var(--line); }
.chat-turn:last-child { border-bottom: 0; }
.chat-question { margin: 0 0 22px 12%; border-right: 3px solid var(--teal); padding: 12px 16px; border-radius: 8px 0 0 8px; background: var(--fog); }
.chat-role { color: var(--teal); font-size: 11px; font-weight: 700; }
.chat-question p { margin: 6px 0 0; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
.chat-answer-head { gap: 8px; margin-bottom: 12px; }
.chat-answer-head h3 { margin: 0; font-size: 14px; }
.chat-answer-head > span { color: var(--muted); font-size: 11px; overflow-wrap: anywhere; }
.chat-answer { min-width: 0; }
.chat-details { margin-top: 16px; font-size: 12px; color: var(--slate); overflow-wrap: anywhere; }
.chat-details summary { cursor: pointer; padding: 8px 0; }
.chat-details p { line-height: 1.6; }
.chat-edit { margin-top: 12px; }
.chat-jump { display: block; margin: 8px auto; }
.chat-input-panel { padding: 18px 20px 12px; background: var(--paper); border: 1px solid var(--line); border-top: 2px solid var(--teal); border-radius: 0 0 10px 10px; box-shadow: var(--shadow); }
.chat-input-head { font-size: 12px; margin-bottom: 8px; }
.chat-input-head label { font-weight: 700; }
.chat-input-head span { font: 11px ui-monospace, monospace; color: var(--muted); }
.chat-input-panel textarea { min-height: 86px; max-height: 240px; resize: vertical; }
.chat-compose-actions { justify-content: flex-start; margin-top: 12px; gap: 10px; }
.chat-model-label { display: flex; align-items: center; gap: 8px; min-width: 0; color: var(--slate); font-size: 12px; }
.chat-model-label select { width: auto; max-width: 240px; min-width: 0; }
.chat-send-actions { margin-left: auto; }
.chat-compose-actions .button { min-width: 0; white-space: nowrap; }
.button-stop { border: 1px solid var(--red); color: var(--red); background: var(--paper); }
.chat-settings { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding-top: 16px; margin-top: 12px; border-top: 1px solid var(--line); }
.chat-settings label { font-size: 12px; color: var(--slate); }
.chat-settings input { margin-top: 6px; }
.chat-settings p { grid-column: 1 / -1; margin: 0; color: var(--muted); font-size: 12px; }
.chat-composer-hint { margin: 10px 0 0; }
.chat-model-error { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 12px; color: var(--red); font-size: 12px; }
@media (max-width: 960px) {
  .chat-model-label { flex: 1 1 100%; }
  .chat-model-label select { flex: 1; max-width: none; }
}
@media (max-width: 720px) {
  .chat-toolbar { gap: 12px; }
  .chat-toolbar h2 { font-size: 23px; }
  .chat-turn { padding: 16px; }
  .chat-question { margin-left: 4%; }
  .chat-input-panel { padding: 14px; }
  .chat-thread { max-height: 60vh; }
  .chat-settings { grid-template-columns: 1fr; }
}
</style>
