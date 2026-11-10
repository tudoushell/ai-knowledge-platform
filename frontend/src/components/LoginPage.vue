<script setup>
import { ref } from "vue";
import { auth } from "../services/auth.js";

defineProps({ sessionMessage: { type: String, default: "" } });
const username = ref("");
const password = ref("");
const showPassword = ref(false);
const busy = ref(false);
const error = ref("");

async function login() {
  if (busy.value) return;
  error.value = "";
  if (!username.value.trim() || !password.value) {
    error.value = "请输入用户名和密码。";
    return;
  }
  busy.value = true;
  try {
    await auth.login(username.value.trim(), password.value);
    password.value = "";
  } catch (failure) {
    error.value = failure.message;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story" aria-label="Atlas 知识库工作台">
      <div class="brand">
        <span class="brand-mark">A</span>
        <div><strong>Atlas</strong><small>知识库工作台</small></div>
      </div>
      <div class="login-intro">
        <p class="login-eyebrow">把知识连接起来</p>
        <h1>让资料成为<br />可追溯的答案。</h1>
        <p class="login-lead">整理文档，检索知识，在每一次提问中找到依据。</p>
        <div class="knowledge-path" aria-label="文档、检索、问答工作流">
          <div class="knowledge-sheet">
            <span>文档</span><i></i><i></i><i></i>
          </div>
          <span class="path-arrow" aria-hidden="true">→</span>
          <div class="knowledge-index">
            <span>检索</span><b>找到相关片段</b><small>连接上下文</small>
          </div>
          <span class="path-arrow" aria-hidden="true">→</span>
          <div class="knowledge-answer">
            <span>问答</span><b>回答有据可查</b><small>回到原文</small>
          </div>
        </div>
      </div>
      <p class="login-story-foot">你的资料，你的知识空间。</p>
    </section>
    <section class="login-form-side" aria-labelledby="login-heading">
      <form class="login-form" @submit.prevent="login" :aria-busy="busy">
        <span class="login-section-label">进入工作台</span>
        <h2 id="login-heading">欢迎回来</h2>
        <p class="login-help">使用分配给你的账号登录。</p>
        <p
          v-if="error || sessionMessage"
          id="login-error"
          class="login-error"
          role="alert"
        >
          {{ error || sessionMessage }}
        </p>
        <label for="login-username">用户名</label>
        <input
          id="login-username"
          v-model="username"
          name="username"
          autocomplete="username"
          maxlength="64"
          required
          :disabled="busy"
          :aria-describedby="error ? 'login-error' : undefined"
          placeholder="输入用户名"
        />
        <label for="login-password">密码</label>
        <div class="password-field">
          <input
            id="login-password"
            v-model="password"
            name="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            required
            :disabled="busy"
            placeholder="输入密码"
          />
          <button
            type="button"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            :aria-pressed="showPassword"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? "隐藏" : "显示" }}
          </button>
        </div>
        <button
          class="button button-primary login-submit"
          type="submit"
          :disabled="busy"
        >
          {{ busy ? "正在登录…" : "登录工作台"
          }}<span v-if="!busy" aria-hidden="true">→</span>
        </button>
        <p class="login-support">无法登录？请联系管理员检查账号状态。</p>
      </form>
      <span class="login-footer">ATLAS · 知识有迹可循</span>
    </section>
  </main>
</template>
