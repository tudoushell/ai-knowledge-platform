<script setup>
import { computed, ref } from "vue";
import {
  formatBytes,
  formatDate,
  statusLabel,
  taskStatusLabel,
  taskStepLabel,
} from "../utils/format.js";
import DocumentInspector from "./DocumentInspector.vue";
const props = defineProps({ workspace: { type: Object, required: true } });
const fileInput = ref(null);
const file = ref(null);
const pipeline = computed(() =>
  ["INDEXED", "INDEXING", "INDEX_FAILED"].includes(
    props.workspace.selected?.status,
  )
    ? 3
    : props.workspace.selected?.status === "CHUNKED"
      ? 2
      : 1,
);
const progress = computed(() =>
  Math.min(100, Math.max(0, Number(props.workspace.task?.progress) || 0)),
);
async function upload() {
  if (await props.workspace.upload(file.value)) {
    file.value = null;
    fileInput.value.value = "";
  }
}
</script>

<template>
  <section id="view-documents" class="view active">
    <div class="section-intro">
      <div>
        <p class="section-kicker">INGESTION PIPELINE</p>
        <h2>把资料变成可检索知识</h2>
      </div>
      <p>
        支持 TXT、Markdown、PDF 与
        DOCX。上传后依次解析、切分文本、生成向量，文档才会进入检索范围。
      </p>
    </div>
    <div class="pipeline" aria-label="文档处理流程">
      <template
        v-for="([title, description], index) in [
          ['上传解析', '保存原文并提取文本'],
          ['文本切分', '生成语义 Chunk'],
          ['向量索引', '写入 PGVector'],
        ]"
        :key="title"
      >
        <div
          v-if="index"
          class="pipeline-link"
          :class="{ done: index < pipeline }"
        />
        <div
          class="pipeline-step"
          :class="{
            current: index + 1 === pipeline,
            done: index + 1 < pipeline,
          }"
        >
          <span>{{ index + 1 }}</span>
          <div>
            <strong>{{ title }}</strong
            ><small>{{ description }}</small>
          </div>
        </div>
      </template>
    </div>
    <div class="document-grid">
      <form id="upload-form" class="card upload-card" @submit.prevent="upload">
        <div class="card-label">新增资料</div>
        <h3>上传文档</h3>
        <p>文件会上传到当前知识库，之后可开始完整处理。</p>
        <label class="file-drop" for="document-file"
          ><span class="file-icon">↥</span
          ><strong id="file-name">{{
            file?.name || "选择 TXT、Markdown、PDF 或 Word 文件"
          }}</strong
          ><small>支持 TXT、MD、PDF、DOCX，单个文件最大 10 MB</small></label
        >
        <input
          id="document-file"
          ref="fileInput"
          type="file"
          accept=".txt,.md,.pdf,.docx,text/plain,text/markdown,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          required
          hidden
          @change="file = $event.target.files[0] || null"
        />
        <button
          class="button button-primary button-block"
          type="submit"
          :disabled="!workspace.activeLibrary || workspace.busy.has('upload')"
        >
          {{ workspace.busy.has("upload") ? "上传中…" : "上传文档" }}
        </button>
      </form>
      <section class="card process-card">
        <div class="card-label">处理资料</div>
        <h3>文档状态</h3>
        <p>选择文档后创建后台任务，系统会依次完成解析、切分和向量索引。</p>
        <label for="document-id">当前文档 ID</label
        ><input
          id="document-id"
          class="mono-input"
          type="text"
          :value="workspace.selectedId"
          placeholder="尚未选择"
          readonly
        />
        <div
          id="document-summary"
          class="document-summary"
          :class="{ empty: !workspace.selected }"
        >
          <template v-if="workspace.selected"
            ><strong>{{ workspace.selected.originalName }}</strong
            ><span>ID：{{ workspace.selected.id }}</span
            ><br /><span
              >状态：{{ statusLabel(workspace.selected.status) }} ·
              {{ workspace.selected.chunkCount ?? 0 }} 个 Chunk ·
              {{ workspace.selected.vectorCount ?? 0 }} 条向量</span
            ></template
          >
          <template v-else>尚未选择文档</template>
        </div>
        <div
          id="process-task-status"
          class="task-progress"
          :class="
            workspace.task ? workspace.task.status.toLowerCase() : 'empty'
          "
          aria-live="polite"
        >
          <template v-if="workspace.task"
            ><div class="task-progress-head">
              <strong>{{ taskStepLabel(workspace.task.currentStep) }}</strong
              ><span>{{ taskStatusLabel(workspace.task.status) }}</span>
            </div>
            <div class="task-progress-bar">
              <span :style="{ width: `${progress}%` }" />
            </div>
            <div class="task-progress-meta">
              进度 {{ progress }}% · 已重试
              {{ workspace.task.retryCount ?? 0 }} 次
            </div>
            <div v-if="workspace.task.errorMessage" class="task-progress-error">
              {{ workspace.task.errorMessage }}
            </div></template
          >
          <template v-else>{{
            workspace.selected ? "尚未创建完整处理任务。" : "尚未选择文档"
          }}</template>
        </div>
        <button
          id="process-document"
          class="button button-primary button-block"
          type="button"
          :disabled="workspace.actionsDisabled"
          @click="workspace.processDocument()"
        >
          {{ workspace.taskActive ? "后台处理中…" : "开始完整处理" }}
        </button>
        <div class="document-actions">
          <button
            id="chunk-document"
            class="button button-secondary"
            type="button"
            :disabled="workspace.actionsDisabled"
            @click="workspace.processDocument('chunk')"
          >
            切分文本</button
          ><button
            id="index-document"
            class="button button-primary"
            type="button"
            :disabled="workspace.actionsDisabled"
            @click="workspace.processDocument('index')"
          >
            生成向量
          </button>
        </div>
        <p class="microcopy">
          处理进度来自服务端；任务运行时会自动刷新，手动操作仅用于调试。
        </p>
      </section>
      <section class="card document-list-card">
        <div class="document-list-head">
          <div>
            <div class="card-label">知识库文件</div>
            <h3>已上传文档</h3>
          </div>
          <div class="document-list-tools">
            <span id="document-count"
              >{{ workspace.documents.length }} 个文档</span
            ><button
              id="refresh-documents"
              class="button button-secondary button-compact"
              type="button"
              :disabled="
                !workspace.activeLibrary || workspace.busy.has('documents')
              "
              @click="
                workspace.run('documents', () => workspace.loadDocuments())
              "
            >
              刷新
            </button>
          </div>
        </div>
        <div id="document-list" class="document-list">
          <article
            v-for="document in workspace.documents"
            :key="document.id"
            class="document-row"
            :class="{ selected: workspace.selectedId === document.id }"
          >
            <button
              type="button"
              class="document-select"
              :aria-pressed="workspace.selectedId === document.id"
              @click="workspace.selectDocument(document.id)"
            >
              <span class="document-type">{{
                (document.fileExtension || "FILE").toUpperCase()
              }}</span
              ><span class="document-file-body"
                ><strong>{{ document.originalName || "未命名文档" }}</strong
                ><small
                  >{{ formatBytes(document.sizeBytes) }} ·
                  {{ formatDate(document.createdAt) }} ·
                  {{ document.chunkCount ?? 0 }} Chunk ·
                  {{ document.vectorCount ?? 0 }} 向量</small
                ></span
              ><span
                class="document-status"
                :class="document.status?.toLowerCase()"
                >{{ statusLabel(document.status) }}</span
              >
            </button>
            <button
              type="button"
              class="document-delete"
              :aria-label="`删除文档 ${document.originalName || document.id}`"
              :disabled="workspace.busy.has(`delete:${document.id}`)"
              @click="workspace.deleteDocument(document)"
            >
              删除
            </button>
          </article>
          <div v-if="!workspace.documents.length" class="document-list-empty">
            {{
              workspace.activeLibrary
                ? "这个知识库还没有文档，从上方上传第一份资料。"
                : "选择知识库后显示已上传文档。"
            }}
          </div>
        </div>
      </section>
      <DocumentInspector :workspace="workspace" />
    </div>
  </section>
</template>
