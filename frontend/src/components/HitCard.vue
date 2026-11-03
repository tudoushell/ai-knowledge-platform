<script setup>
import { computed } from "vue";
import HighlightedText from "./HighlightedText.vue";

const props = defineProps({
  hit: { type: Object, required: true },
  label: { default: "" },
  query: { default: "" },
});
const metadata = computed(() => {
  const hit = props.hit;
  const details = [];
  if (hit.sectionTitle) details.push(hit.sectionTitle);
  if (hit.pageNumber != null) details.push(`第 ${hit.pageNumber} 页`);
  if (hit.chunkIndex != null) details.push(`Chunk ${hit.chunkIndex}`);
  if (hit.contextStartIndex != null && hit.contextEndIndex != null) {
    details.push(
      `上下文 Chunk ${hit.contextStartIndex}–${hit.contextEndIndex}`,
    );
  }
  if (hit.matchedChunkIndex != null)
    details.push(`命中 Chunk ${hit.matchedChunkIndex}`);
  return details.join(" · ") || `文档 ID：${hit.documentId || "-"}`;
});
</script>

<template>
  <article class="hit" :class="{ 'keyword-hit': query }">
    <div class="hit-top">
      <div class="hit-title">
        {{ label || `#${hit.rank ?? "—"}` }} ·
        <HighlightedText
          :text="hit.documentName || '未命名文档'"
          :query="query"
        />
      </div>
      <span class="hit-score" :class="{ 'keyword-score': query }">{{
        hit.score == null
          ? "NO SCORE"
          : `${query ? "匹配 " : ""}${Number(hit.score).toFixed(4)}`
      }}</span>
    </div>
    <div class="hit-meta">
      <HighlightedText :text="metadata" :query="query" />
    </div>
    <details v-if="hit.chunks?.length" class="source-details" open>
      <summary>上下文详情 · {{ hit.chunks.length }} 个 Chunk</summary>
      <div class="source-chunks">
        <section
          v-for="(chunk, index) in hit.chunks"
          :key="chunk.id || index"
          class="source-chunk"
          :class="{ matched: chunk.matched }"
        >
          <div class="source-chunk-head">
            <strong>Chunk {{ chunk.chunkIndex ?? "" }}</strong
            ><span class="chunk-badge" :class="{ matched: chunk.matched }">{{
              chunk.matched ? "核心检索命中" : "相邻上下文"
            }}</span>
          </div>
          <div class="source-chunk-meta">
            {{ chunk.sectionTitle || chunk.selectTitle }}
            {{ chunk.pageNumber != null ? `第 ${chunk.pageNumber} 页` : "" }}
          </div>
          <p class="source-chunk-content">
            {{ chunk.content || "该 Chunk 没有可展示的文本。" }}
          </p>
        </section>
      </div>
    </details>
    <p v-else class="hit-content">
      <HighlightedText :text="hit.content" :query="query" />
    </p>
  </article>
</template>
