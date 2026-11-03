<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useWorkspace } from "./composables/useWorkspace.js";
import { formatDate } from "./utils/format.js";
import DocumentsPanel from "./components/DocumentsPanel.vue";
import SearchPanel from "./components/SearchPanel.vue";
import ChatPanel from "./components/ChatPanel.vue";

const toast = reactive({ message: "", type: "", visible: false });
let toastTimer;
function notify(message, type = "success") {
  clearTimeout(toastTimer);
  Object.assign(toast, { message, type, visible: true });
  toastTimer = setTimeout(() => (toast.visible = false), 3200);
}
// 将 ref 解包后的 workspace 作为组件间共享的响应式上下文。
const workspace = reactive(useWorkspace(notify));
const view = ref("documents");
const createOpen = ref(false);
const createValues = reactive({ name: "", description: "" });
const nameInput = ref(null);
const tabs = [
  ["documents", "文档处理"],
  ["retrieval", "向量检索"],
  ["keyword-search", "关键词检索"],
  ["chat", "知识问答"],
];
async function toggleCreate() {
  createOpen.value = !createOpen.value;
  if (createOpen.value) {
    await nextTick();
    nameInput.value?.focus();
  }
}
async function createLibrary() {
  if (
    await workspace.createLibrary({
      name: createValues.name.trim(),
      description: createValues.description.trim(),
    })
  ) {
    createOpen.value = false;
    createValues.name = "";
    createValues.description = "";
  }
}
onMounted(() => workspace.loadLibraries());
onBeforeUnmount(() => clearTimeout(toastTimer));
</script>

<template>
  <div class="app-shell">
    <aside class="library-rail">
      <div class="brand">
        <span class="brand-mark">A</span>
        <div><strong>Atlas</strong><small>知识库工作台</small></div>
      </div>
      <button
        id="create-toggle"
        class="create-toggle"
        type="button"
        :aria-expanded="createOpen"
        @click="toggleCreate"
      >
        <span>＋</span> 新建知识库
      </button>
      <form
        v-show="createOpen"
        id="create-form"
        class="create-form"
        @submit.prevent="createLibrary"
      >
        <label for="kb-name">知识库名称</label>
        <input
          id="kb-name"
          ref="nameInput"
          v-model="createValues.name"
          required
          maxlength="128"
          placeholder="例如：Java 技术手册"
        />
        <label for="kb-description">描述</label>
        <textarea
          id="kb-description"
          v-model="createValues.description"
          maxlength="500"
          placeholder="说明这组资料的用途"
        />
        <div class="form-actions">
          <button
            class="button button-primary"
            :disabled="workspace.busy.has('create')"
            type="submit"
          >
            创建</button
          ><button
            id="create-cancel"
            class="button button-quiet"
            type="button"
            @click="createOpen = false"
          >
            取消
          </button>
        </div>
      </form>
      <div class="rail-heading">
        <span>知识库</span
        ><button
          id="refresh-libraries"
          class="icon-button"
          type="button"
          title="刷新知识库"
          aria-label="刷新知识库"
          :disabled="workspace.busy.has('libraries')"
          @click="workspace.loadLibraries()"
        >
          ↻
        </button>
      </div>
      <div id="library-list" class="library-list" aria-live="polite">
        <div v-if="workspace.busy.has('libraries')" class="library-empty">
          正在读取知识库…
        </div>
        <template v-else>
          <button
            v-for="library in workspace.libraries"
            :key="library.id"
            type="button"
            class="library-item"
            :class="{ active: workspace.activeLibrary?.id === library.id }"
            @click="workspace.selectLibrary(library.id)"
          >
            <strong>{{ library.name }}</strong
            ><small
              >{{ library.status || "UNKNOWN" }} ·
              {{ formatDate(library.updatedAt || library.createdAt) }}</small
            >
          </button>
          <div v-if="!workspace.libraries.length" class="library-empty">
            {{
              workspace.connection === "连接失败"
                ? "无法读取知识库，请确认服务已启动。"
                : "还没有知识库，先创建一个。"
            }}
          </div>
        </template>
      </div>
      <div class="rail-foot">
        接口状态
        <span
          id="api-status"
          class="status-dot"
          :class="{ error: workspace.connection === '连接失败' }"
          >{{ workspace.connection }}</span
        >
      </div>
    </aside>
    <main class="workspace">
      <header class="workspace-head">
        <div>
          <p class="eyebrow">ACTIVE LIBRARY</p>
          <h1 id="active-name">
            {{ workspace.activeLibrary?.name || "选择一个知识库" }}
          </h1>
          <p id="active-description" class="workspace-description">
            {{
              workspace.activeLibrary
                ? workspace.activeLibrary.description ||
                  `知识库 ID：${workspace.activeLibrary.id}`
                : "从左侧选择知识库，开始整理与检索资料。"
            }}
          </p>
        </div>
        <div
          id="active-badge"
          class="active-badge"
          :class="{ enabled: workspace.activeLibrary?.status === 'ENABLED' }"
        >
          {{ workspace.activeLibrary?.status || "未选择" }}
        </div>
      </header>
      <nav class="view-tabs" aria-label="工作台功能">
        <button
          v-for="[name, label] in tabs"
          :key="name"
          class="view-tab"
          :class="{ active: view === name }"
          type="button"
          @click="view = name"
        >
          {{ label }}
        </button>
      </nav>
      <DocumentsPanel v-show="view === 'documents'" :workspace="workspace" />
      <SearchPanel
        v-show="view === 'retrieval'"
        :workspace="workspace"
        :notify="notify"
      />
      <SearchPanel
        v-show="view === 'keyword-search'"
        keyword
        :workspace="workspace"
        :notify="notify"
      />
      <ChatPanel
        v-show="view === 'chat'"
        :workspace="workspace"
        :notify="notify"
      />
    </main>
  </div>
  <div
    id="toast"
    class="toast"
    :class="{ visible: toast.visible, error: toast.type === 'error' }"
    role="status"
    aria-live="polite"
  >
    {{ toast.message }}
  </div>
</template>
