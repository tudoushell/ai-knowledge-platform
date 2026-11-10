<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { auth } from "./services/auth.js";
import LoginPage from "./components/LoginPage.vue";
import Workspace from "./Workspace.vue";

const session = reactive({ status: "checking", username: "", error: "" });
const logoutError = ref("");
const unsubscribe = auth.subscribe((state) => Object.assign(session, state));
onMounted(() => auth.restore());
onBeforeUnmount(unsubscribe);

async function logout() {
  logoutError.value = "";
  try {
    await auth.logout();
  } catch (error) {
    logoutError.value = `退出失败：${error.message}，请重试。`;
  }
}
</script>

<template>
  <template
    v-if="
      session.status === 'authenticated' || session.status === 'logging-out'
    "
  >
    <Workspace
      :account-name="session.username || '已登录用户'"
      :logging-out="session.status === 'logging-out'"
      @logout="logout"
    />
    <div v-if="logoutError" class="session-alert" role="alert">
      {{ logoutError }}
      <button type="button" @click="logoutError = ''" aria-label="关闭提示">
        ×
      </button>
    </div>
  </template>
  <LoginPage
    v-else-if="session.status === 'anonymous'"
    :session-message="session.error"
  />
  <main v-else class="session-screen" aria-live="polite">
    <span class="brand-mark">A</span>
    <h1>
      {{
        session.status === "checking"
          ? "正在恢复登录状态"
          : "暂时无法连接登录服务"
      }}
    </h1>
    <p>
      {{
        session.status === "checking"
          ? "即将进入你的知识库工作台…"
          : session.error
      }}
    </p>
    <button
      v-if="session.status === 'unavailable'"
      class="button button-primary"
      type="button"
      @click="auth.restore()"
    >
      重新连接
    </button>
  </main>
</template>
