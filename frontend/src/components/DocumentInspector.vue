<script setup>
import { computed } from "vue";
import {
  formatBytes,
  formatDateTime,
  statusLabel,
  taskStatusLabel,
  taskStepLabel,
} from "../utils/format.js";
const props = defineProps({ workspace: { type: Object, required: true } });
const fields = computed(() => {
  const d = props.workspace.detail;
  if (!d) return [];
  return [
    ["状态", statusLabel(d.status)],
    ["文件大小", formatBytes(d.sizeBytes)],
    ["解析格式", d.parsedFormat],
    ["文本字符", d.parsedCharCount],
    ["结构块", d.parsedBlockCount],
    ["页数", d.pageCount],
    ["切分策略", d.chunkStrategy],
    [
      "Chunk 配置",
      d.chunkSize == null
        ? null
        : `${d.chunkSize} / overlap ${d.chunkOverlap ?? 0}`,
    ],
    ["向量模型", d.embeddingModel],
    ["最近索引", formatDateTime(d.indexedAt)],
    ["创建时间", formatDateTime(d.createdAt)],
    ["更新时间", formatDateTime(d.updatedAt)],
  ];
});
const pages = computed(() => [
  {
    type: "chunks",
    title: "文本片段",
    unit: "段",
    data: props.workspace.chunks,
  },
  {
    type: "process-tasks",
    title: "处理历史",
    unit: "次",
    data: props.workspace.history,
  },
]);
</script>

<template>
  <section class="card document-inspector-card">
    <div class="document-list-head inspector-head">
      <div>
        <div class="card-label">文件检查台</div>
        <h3 id="inspector-title">
          {{ workspace.selected?.originalName || "选择文档查看详情" }}
        </h3>
      </div>
      <div class="document-list-tools">
        <span id="inspector-status">{{
          workspace.selected
            ? statusLabel(workspace.detail?.status || workspace.selected.status)
            : "未选择文档"
        }}</span
        ><button
          id="reprocess-document"
          class="button button-secondary button-compact"
          type="button"
          :disabled="workspace.actionsDisabled"
          @click="workspace.processDocument('reprocess')"
        >
          {{ workspace.taskActive ? "任务处理中…" : "重新处理" }}
        </button>
      </div>
    </div>
    <div class="inspector-grid">
      <section class="inspector-panel detail-panel">
        <div class="inspector-panel-head">
          <h4>文件详情</h4>
          <span>解析与索引信息</span>
        </div>
        <div v-if="workspace.detail" id="document-detail" class="detail-grid">
          <div
            v-for="[label, value] in fields"
            :key="label"
            class="detail-item"
          >
            <span>{{ label }}</span
            ><strong>{{ value == null || value === "" ? "—" : value }}</strong>
          </div>
          <div v-if="workspace.detail.parsedPreview" class="detail-preview">
            <span>解析预览</span>
            <p>{{ workspace.detail.parsedPreview }}</p>
          </div>
          <div v-if="workspace.detail.errorMessage" class="detail-error">
            处理错误：{{ workspace.detail.errorMessage }}
          </div>
        </div>
        <div v-else class="inspector-empty">
          {{
            workspace.busy.has("inspector")
              ? "正在读取文件详情…"
              : workspace.selected
                ? "文件详情加载失败。"
                : "选择文档后加载文件详情。"
          }}
        </div>
      </section>
      <section
        v-for="panel in pages"
        :key="panel.type"
        class="inspector-panel"
        :class="panel.type === 'chunks' ? 'chunks-panel' : 'history-panel'"
      >
        <div class="inspector-panel-head">
          <h4>{{ panel.title }}</h4>
          <div class="pager-tools">
            <button
              class="pager-button"
              type="button"
              :aria-label="`上一页${panel.title}`"
              :disabled="
                !workspace.selected ||
                panel.data.page <= 1 ||
                workspace.busy.has(panel.type)
              "
              @click="
                workspace.run(panel.type, () =>
                  workspace.loadPage(panel.type, panel.data.page - 1),
                )
              "
            >
              ←
            </button>
            <span>{{
              panel.data.total
                ? `${panel.data.page} / ${Math.ceil(panel.data.total / panel.data.size)} · ${panel.data.total} ${panel.unit}`
                : `0 ${panel.unit}`
            }}</span>
            <button
              class="pager-button"
              type="button"
              :aria-label="`下一页${panel.title}`"
              :disabled="
                !workspace.selected ||
                panel.data.page * panel.data.size >= panel.data.total ||
                workspace.busy.has(panel.type)
              "
              @click="
                workspace.run(panel.type, () =>
                  workspace.loadPage(panel.type, panel.data.page + 1),
                )
              "
            >
              →
            </button>
          </div>
        </div>
        <div v-if="!panel.data.records?.length" class="inspector-empty">
          {{
            workspace.busy.has("inspector")
              ? "正在读取…"
              : panel.type === "chunks"
                ? "暂无可展示的 Chunk。"
                : "暂无处理任务历史。"
          }}
        </div>
        <div
          v-else
          :class="
            panel.type === 'chunks'
              ? 'chunk-inspector-list'
              : 'task-history-list'
          "
        >
          <template
            v-for="(item, index) in panel.data.records"
            :key="item.id || index"
          >
            <article
              v-if="panel.type === 'chunks'"
              class="chunk-inspector-item"
            >
              <div class="chunk-inspector-head">
                <strong>Chunk {{ item.chunkIndex ?? "—" }}</strong
                ><span
                  >{{ item.sectionTitle }}
                  {{
                    item.pageNumber != null
                      ? `第 ${item.pageNumber} 页 · `
                      : ""
                  }}{{ item.charCount ?? 0 }} 字符</span
                >
              </div>
              <p>{{ item.content || "该 Chunk 没有可展示的文本。" }}</p>
            </article>
            <article
              v-else
              class="history-item"
              :class="item.status?.toLowerCase()"
            >
              <div class="history-item-head">
                <strong
                  >{{ taskStepLabel(item.currentStep) }} ·
                  {{ taskStatusLabel(item.status) }}</strong
                ><span>{{ item.progress ?? 0 }}%</span>
              </div>
              <p>
                {{ formatDateTime(item.createdAt) }} · 重试
                {{ item.retryCount ?? 0 }} 次
              </p>
              <small v-if="item.errorMessage">{{ item.errorMessage }}</small>
            </article>
          </template>
        </div>
      </section>
    </div>
  </section>
</template>
