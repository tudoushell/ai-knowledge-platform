<script setup>
import { onBeforeUnmount, reactive, ref, watch } from "vue";
import { post } from "../services/api.js";
import HitCard from "./HitCard.vue";
const props = defineProps({
  keyword: Boolean,
  workspace: { type: Object, required: true },
  notify: { type: Function, required: true },
});
const form = reactive({ query: "", topK: 5, similarityThreshold: 0.5 });
const result = ref(null);
const loading = ref(false);
let controller;
function reset() {
  controller?.abort();
  controller = null;
  loading.value = false;
  result.value = null;
}
watch(() => props.workspace.activeLibrary?.id, reset);
onBeforeUnmount(reset);
async function search() {
  if (loading.value || !props.workspace.requireLibrary()) return;
  const query = form.query.trim();
  if (query.length < (props.keyword ? 2 : 1))
    return props.notify(
      props.keyword ? "搜索关键词至少需要 2 个字符" : "请输入检索问题",
      "error",
    );
  const request = new AbortController();
  controller = request;
  loading.value = true;
  try {
    const payload = {
      knowledgeBaseId: props.workspace.activeLibrary.id,
      query,
      topK: form.topK,
    };
    if (!props.keyword) payload.similarityThreshold = form.similarityThreshold;
    const data = await post(
      `/api/retrieval/${props.keyword ? "keyword-search" : "search"}`,
      payload,
      { signal: request.signal },
    );
    if (controller === request) result.value = data;
  } catch (error) {
    if (error.name !== "AbortError") props.notify(error.message, "error");
  } finally {
    if (controller === request) {
      loading.value = false;
      controller = null;
    }
  }
}
</script>

<template>
  <section
    :id="keyword ? 'view-keyword-search' : 'view-retrieval'"
    class="view active"
  >
    <div class="section-intro">
      <div>
        <p class="section-kicker">
          {{ keyword ? "LEXICAL SEARCH" : "VECTOR SEARCH" }}
        </p>
        <h2>{{ keyword ? "定位关键词出现在哪里" : "观察模型召回了什么" }}</h2>
      </div>
      <p>
        {{
          keyword
            ? "直接匹配正文与章节标题，不生成向量。适合查找术语、类名、配置项和明确出现过的短语。"
            : "纯检索不会调用聊天模型，适合验证 Chunk、相似度阈值和 TopK 是否合理。"
        }}
      </p>
    </div>
    <form
      :id="keyword ? 'keyword-search-form' : 'retrieval-form'"
      class="query-composer"
      :class="{ 'keyword-composer': keyword }"
      @submit.prevent="search"
    >
      <label :for="keyword ? 'keyword-search-query' : 'retrieval-query'">{{
        keyword ? "搜索关键词" : "检索问题"
      }}</label>
      <input
        v-if="keyword"
        id="keyword-search-query"
        v-model="form.query"
        class="keyword-query-input"
        type="search"
        required
        minlength="2"
        maxlength="500"
        autocomplete="off"
        placeholder="例如：spring.ai.vectorstore.pgvector"
      />
      <textarea
        v-else
        id="retrieval-query"
        v-model="form.query"
        required
        maxlength="1000"
        placeholder="输入要查找的概念、问题或关键词"
      />
      <div class="query-options" :class="{ 'keyword-query-options': keyword }">
        <label
          >返回数量<input
            v-model.number="form.topK"
            type="number"
            min="1"
            max="20"
            required
        /></label>
        <label v-if="!keyword"
          >相似度阈值<input
            v-model.number="form.similarityThreshold"
            type="number"
            min="0"
            max="1"
            step="0.05"
            required
        /></label>
        <p v-else class="keyword-search-hint">
          正文与章节标题中的匹配内容会在结果中标出。
        </p>
        <button class="button button-primary" type="submit" :disabled="loading">
          {{ loading ? "检索中…" : keyword ? "查找关键词" : "开始检索" }}
        </button>
      </div>
    </form>
    <div
      class="output-panel"
      :class="{ 'empty-output': !result, 'keyword-output': keyword && result }"
      :aria-busy="loading"
    >
      <template v-if="!result"
        ><div class="empty-symbol">{{ keyword ? "“”" : "⌁" }}</div>
        <h3>{{ keyword ? "等待关键词" : "等待检索" }}</h3>
        <p>命中的文档片段、分数与来源会显示在这里。</p></template
      >
      <template v-else>
        <div class="result-head">
          <h3>{{ keyword ? `“${result.query || ""}”` : "检索结果" }}</h3>
          <span
            >{{
              result.resultCount ??
              (keyword ? result.results?.length : result.hits?.length) ??
              0
            }}
            条命中 · TopK {{ result.topK
            }}{{ keyword ? "" : ` · 阈值 ${result.similarityThreshold}` }}</span
          >
        </div>
        <div class="result-list">
          <HitCard
            v-for="(hit, index) in keyword ? result.results : result.hits"
            :key="hit.chunkId || index"
            :hit="hit"
            :query="keyword ? result.query : ''"
          />
          <div
            v-if="!(keyword ? result.results : result.hits)?.length"
            class="notice"
          >
            {{
              keyword
                ? "没有找到包含该关键词的片段。可以尝试更短的词组，或检查文档是否已完成索引。"
                : "没有达到相似度阈值的片段。可以降低阈值，或检查文档是否已完成索引。"
            }}
          </div>
        </div>
      </template>
    </div>
  </section>
</template>
