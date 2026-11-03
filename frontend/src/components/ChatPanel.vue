<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { api, streamSse } from "../services/api.js";
import HitCard from "./HitCard.vue";
import MarkdownContent from "./MarkdownContent.vue";
const props = defineProps({
  workspace: { type: Object, required: true },
  notify: { type: Function, required: true },
});
const models = ref([]);
const modelsLoading = ref(true);
const formElement = ref(null);
const form = reactive({
  question: "",
  modelCode: "",
  topK: 5,
  similarityThreshold: 0.5,
});
const chat = ref(null);
const loading = ref(false);
let controller;
const modelsController = new AbortController();
const title = computed(() =>
  chat.value?.error
    ? "回答生成失败"
    : chat.value?.done
      ? chat.value.sources.length
        ? "基于知识库的回答"
        : "未找到相关资料"
      : chat.value?.answer
        ? "正在生成回答"
        : "正在进行混合检索",
);
function reset() {
  controller?.abort();
  controller = null;
  chat.value = null;
  loading.value = false;
}
watch(() => props.workspace.activeLibrary?.id, reset);
watch(
  () => form.modelCode,
  (code) => {
    if (code) localStorage.setItem("atlas-chat-model", code);
  },
);
onBeforeUnmount(() => {
  reset();
  modelsController.abort();
});
onMounted(async () => {
  try {
    const data = await api("/api/models", { signal: modelsController.signal });
    if (!Array.isArray(data) || !data.length)
      throw new Error("当前没有可用的聊天模型");
    models.value = data;
    const saved = localStorage.getItem("atlas-chat-model");
    form.modelCode = data.includes(saved) ? saved : data[0];
  } catch (error) {
    if (error.name !== "AbortError") props.notify(error.message, "error");
  } finally {
    modelsLoading.value = false;
  }
});
async function submit() {
  if (loading.value || !props.workspace.requireLibrary()) return;
  if (!models.value.includes(form.modelCode))
    return props.notify("请选择一个可用的生成模型", "error");
  if (!form.question.trim()) return props.notify("请输入你的问题", "error");
  const request = new AbortController();
  controller = request;
  loading.value = true;
  const view = reactive({
    answer: "",
    sources: [],
    usage: null,
    error: "",
    done: false,
  });
  chat.value = view;
  try {
    await streamSse(
      "/api/rag/chat/stream",
      {
        method: "POST",
        signal: request.signal,
        headers: {
          "Content-Type": "application/json",
          Accept: "text/event-stream",
        },
        body: JSON.stringify({
          ...form,
          question: form.question.trim(),
          knowledgeBaseId: props.workspace.activeLibrary.id,
        }),
      },
      ({ event, data }) => {
        if (controller !== request || !data || typeof data !== "object") return;
        const type = data.type || event;
        if (type === "sources" || type === "source")
          view.sources = Array.isArray(data.sources) ? data.sources : [];
        if (type === "delta") view.answer += data.content || "";
        if (type === "error")
          view.error = data.content || "回答生成失败，请稍后重试。";
        if (type === "done") {
          view.usage = data.usage || null;
          view.done = true;
        }
      },
    );
  } catch (error) {
    if (controller === request && error.name !== "AbortError") {
      view.error = error.message;
      props.notify(error.message, "error");
    }
  } finally {
    if (controller === request) {
      view.done = true;
      loading.value = false;
      controller = null;
    }
  }
}
function shortcut(event) {
  if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
    event.preventDefault();
    if (!loading.value && models.value.length)
      formElement.value.requestSubmit();
  }
}
</script>

<template>
  <section id="view-chat" class="view active">
    <div class="section-intro">
      <div>
        <p class="section-kicker">HYBRID RAG · STREAMING</p>
        <h2>检索资料后实时生成回答</h2>
      </div>
      <p>
        系统先在当前知识库中进行向量与关键词混合检索，经 RRF
        排序并扩展命中上下文；找到资料后，所选模型才会逐段生成回答。完成后可查看引用资料和
        Token 用量。
      </p>
    </div>
    <form
      id="chat-form"
      ref="formElement"
      class="query-composer chat-composer"
      @submit.prevent="submit"
    >
      <label for="chat-question">你的问题</label
      ><textarea
        id="chat-question"
        v-model="form.question"
        required
        maxlength="1000"
        placeholder="例如：文档中推荐的部署方式是什么？"
        @keydown="shortcut"
      />
      <div class="query-options">
        <label for="chat-model"
          >生成模型<select
            id="chat-model"
            v-model="form.modelCode"
            required
            :disabled="!models.length || loading"
          >
            <option v-if="!models.length" value="">
              {{ modelsLoading ? "正在加载模型…" : "模型加载失败" }}
            </option>
            <option v-for="model in models" :key="model" :value="model">
              {{ model }}
            </option>
          </select></label
        >
        <label
          >最终候选数<input
            v-model.number="form.topK"
            type="number"
            min="1"
            max="20"
            required
        /></label>
        <label
          >向量相似度阈值<input
            v-model.number="form.similarityThreshold"
            type="number"
            min="0"
            max="1"
            step="0.05"
            required
        /></label>
        <button
          id="chat-submit"
          class="button button-primary"
          type="submit"
          :disabled="!models.length || loading"
        >
          {{ loading ? "正在生成…" : "检索并实时回答" }}
        </button>
      </div>
      <small class="shortcut">⌘ / Ctrl + Enter 快速发送</small>
    </form>
    <div
      id="chat-output"
      class="output-panel"
      :class="{ 'empty-output': !chat, 'is-streaming': loading }"
      :aria-busy="loading"
    >
      <template v-if="!chat"
        ><div class="empty-symbol">?</div>
        <h3>等待提问</h3>
        <p>
          发送后将先执行混合检索；找到资料会显示引用并实时生成回答，未找到资料时不会调用模型。
        </p></template
      >
      <template v-else>
        <div class="result-head">
          <h3>{{ title }}</h3>
          <span :class="{ 'stream-status': loading }"
            >{{ chat.sources.length }} 条引用 ·
            {{ loading ? "正在生成" : "已结束" }}</span
          >
        </div>
        <MarkdownContent
          v-if="chat.answer"
          class="answer-body"
          :content="chat.answer"
        />
        <p v-else-if="loading && !chat.error" class="stream-placeholder">
          正在通过向量与关键词检索查找资料…
        </p>
        <div v-if="chat.error" :class="chat.answer ? 'stream-error' : 'notice'">
          {{ chat.error }}
        </div>
        <template v-if="chat.sources.length"
          ><h4 class="source-heading">引用来源</h4>
          <div class="source-list">
            <HitCard
              v-for="(source, index) in chat.sources"
              :key="source.referenceId || index"
              :hit="source"
              :label="source.referenceId"
            /></div
        ></template>
        <div v-if="chat.usage" class="usage">
          Token：输入 {{ chat.usage.promptTokens ?? "-" }} / 输出
          {{ chat.usage.completionTokens ?? "-" }} / 总计
          {{ chat.usage.totalTokens ?? "-" }}
        </div>
      </template>
    </div>
  </section>
</template>
