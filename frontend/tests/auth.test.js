import test from "node:test";
import assert from "node:assert/strict";
import { createAuthClient } from "../src/services/auth.js";
import { createApiClient } from "../src/services/api.js";

const token = (value = "access", expiresIn = 900) =>
  Response.json({
    code: 200,
    data: { accessToken: value, tokenType: "Bearer", expiresIn },
  });
const denied = () =>
  Response.json({ code: 401, message: "请重新登录" }, { status: 401 });

test("页面重新加载后从刷新响应恢复用户名，普通请求不会再次刷新", async () => {
  let refreshes = 0;
  let state;
  const auth = createAuthClient({
    fetcher: async (path) => {
      if (path.endsWith("/refresh")) {
        refreshes++;
        return Response.json({
          code: 200,
          data: {
            accessToken: "access",
            tokenType: "Bearer",
            expiresIn: 900,
            username: "admin",
          },
        });
      }
      return Response.json({ code: 200, data: [] });
    },
  });
  auth.subscribe((value) => {
    state = value;
  });
  await auth.restore();
  assert.equal(state.status, "authenticated");
  assert.equal(state.username, "admin");
  for (let i = 0; i < 5; i++) await auth.fetch("/api/models");
  assert.equal(refreshes, 1);
});

test("登录只提交密码到登录接口，业务请求携带 Bearer 和 Cookie，退出后清理状态", async () => {
  const calls = [];
  const auth = createAuthClient({
    fetcher: async (path, options) => {
      calls.push({ path, options });
      if (path.endsWith("/login")) return token();
      if (path.endsWith("/logout"))
        return Response.json({ code: 200, data: null });
      return Response.json({ code: 200, data: [] });
    },
  });
  let state;
  auth.subscribe((value) => {
    state = value;
  });
  await auth.login("alice", "password");
  assert.equal(state.status, "authenticated");
  assert.equal(state.username, "alice");
  assert.equal(state.accessToken, undefined);
  assert.equal(calls[0].options.headers["X-Requested-With"], "XMLHttpRequest");
  assert.equal(calls[0].options.credentials, "include");
  await createApiClient((...args) => auth.fetch(...args)).api(
    "/api/knowledge-bases",
  );
  assert.equal(calls[1].options.headers.get("Authorization"), "Bearer access");
  assert.equal(calls[1].options.body, undefined);
  await auth.logout();
  assert.equal(calls[2].options.body, undefined);
  assert.equal(state.status, "anonymous");
  assert.equal(state.username, "");
});

test("首次恢复会话失败进入登录，网络故障保留重试入口", async () => {
  const auth = createAuthClient({ fetcher: async () => denied() });
  let state;
  auth.subscribe((value) => {
    state = value;
  });
  await auth.restore();
  assert.equal(state.status, "anonymous");
  assert.equal(state.error, "");
  const offline = createAuthClient({
    fetcher: async () => {
      throw new Error("服务不可用");
    },
  });
  offline.subscribe((value) => {
    state = value;
  });
  await offline.restore();
  assert.equal(state.status, "unavailable");
  assert.equal(state.error, "服务不可用");
});

test("Access Token 到期时多个请求共享一次刷新，并保留上传请求体", async () => {
  let clock = 0;
  let refreshes = 0;
  const seen = [];
  const auth = createAuthClient({
    now: () => clock,
    fetcher: async (path, options) => {
      if (path.endsWith("/login")) return token("old", 30);
      if (path.endsWith("/refresh")) {
        refreshes++;
        return token("new");
      }
      seen.push(options);
      return Response.json({ code: 200, data: "ok" });
    },
  });
  await auth.login("alice", "pw");
  clock = 31000;
  const body = new FormData();
  body.append("file", new Blob(["document"]), "doc.txt");
  await Promise.all([
    auth.fetch("/api/a"),
    auth.fetch("/api/b", { method: "POST", body }),
  ]);
  assert.equal(refreshes, 1);
  assert.ok(
    seen.every((item) => item.headers.get("Authorization") === "Bearer new"),
  );
  assert.equal(seen[1].body, body);
  assert.equal(seen[1].headers.has("Content-Type"), false);
});

test("并发 HTTP 401 只刷新一次；业务 code=401 也可触发刷新", async () => {
  for (const businessCode of [false, true]) {
    let refreshes = 0;
    const auth = createAuthClient({
      fetcher: async (path, options) => {
        if (path.endsWith("/login")) return token("old");
        if (path.endsWith("/refresh")) {
          refreshes++;
          return token("new");
        }
        if (options.headers.get("Authorization") === "Bearer old")
          return businessCode
            ? Response.json({ code: 401, message: "expired" })
            : denied();
        return Response.json({ code: 200, data: "ok" });
      },
    });
    await auth.login("alice", "pw");
    await Promise.all([auth.fetch("/api/a"), auth.fetch("/api/b")]);
    assert.equal(refreshes, 1);
  }
});

test("刷新无效时清除会话；网络故障不当作凭据失效", async () => {
  for (const invalid of [true, false]) {
    let state;
    let clock = 0;
    const auth = createAuthClient({
      now: () => clock,
      fetcher: async (path) => {
        if (path.endsWith("/login")) return token("old", 30);
        if (invalid) return denied();
        throw new Error("网络断开");
      },
    });
    auth.subscribe((value) => {
      state = value;
    });
    await auth.login("alice", "pw");
    clock = 31000;
    await assert.rejects(auth.fetch("/api/a"));
    assert.equal(state.status, invalid ? "anonymous" : "authenticated");
  }
});

test("重试仍然返回 401 时不会无限刷新", async () => {
  let refreshes = 0;
  const auth = createAuthClient({
    fetcher: async (path) => {
      if (path.endsWith("/login")) return token();
      if (path.endsWith("/refresh")) {
        refreshes++;
        return token("new");
      }
      return denied();
    },
  });
  await auth.login("alice", "pw");
  await assert.rejects(auth.fetch("/api/a"), /登录已失效/);
  assert.equal(refreshes, 1);
});

test("SSE 经过认证后直接读取事件，不重复已开始的流", async () => {
  let streamCalls = 0;
  const auth = createAuthClient({
    fetcher: async (path, options) => {
      if (path.endsWith("/login")) return token();
      streamCalls++;
      assert.equal(options.headers.get("Authorization"), "Bearer access");
      return new Response('data: {"type":"done"}\n\n', {
        headers: { "Content-Type": "text/event-stream" },
      });
    },
  });
  await auth.login("alice", "pw");
  const events = [];
  await createApiClient((...args) => auth.fetch(...args)).streamSse(
    "/api/rag/chat/stream",
    {},
    (event) => events.push(event),
  );
  assert.equal(events[0].data.type, "done");
  assert.equal(streamCalls, 1);
});

test("等待刷新期间取消请求，不发送业务请求", async () => {
  let release;
  let started;
  const ready = new Promise((resolve) => {
    started = resolve;
  });
  let clock = 0;
  let businessCalls = 0;
  const auth = createAuthClient({
    now: () => clock,
    fetcher: async (path) => {
      if (path.endsWith("/login")) return token("old", 30);
      if (path.endsWith("/refresh")) {
        started();
        return new Promise((resolve) => {
          release = () => resolve(token("new"));
        });
      }
      businessCalls++;
    },
  });
  await auth.login("alice", "pw");
  clock = 31000;
  const controller = new AbortController();
  const request = auth.fetch("/api/a", { signal: controller.signal });
  const rejected = assert.rejects(request, { name: "AbortError" });
  await ready;
  controller.abort();
  release();
  await rejected;
  assert.equal(businessCalls, 0);
});

test("退出等待正在进行的刷新，重复点击退出只发一次，迟到刷新不恢复会话", async () => {
  let clock = 0;
  let release;
  let started;
  const ready = new Promise((resolve) => {
    started = resolve;
  });
  let logoutCalls = 0;
  let businessCalls = 0;
  let state;
  const auth = createAuthClient({
    now: () => clock,
    fetcher: async (path) => {
      if (path.endsWith("/login")) return token("old", 30);
      if (path.endsWith("/refresh")) {
        started();
        return new Promise((resolve) => {
          release = () => resolve(token("new"));
        });
      }
      if (path.endsWith("/logout")) {
        logoutCalls++;
        return Response.json({ code: 200 });
      }
      businessCalls++;
      return Response.json({ code: 200 });
    },
  });
  auth.subscribe((value) => {
    state = value;
  });
  await auth.login("alice", "pw");
  clock = 31000;
  const request = auth.fetch("/api/a");
  const rejected = assert.rejects(request, /正在退出登录|登录状态已变更/);
  await ready;
  const firstLogout = auth.logout();
  const secondLogout = auth.logout();
  assert.equal(firstLogout, secondLogout);
  assert.equal(logoutCalls, 0);
  release();
  await Promise.all([firstLogout, secondLogout, rejected]);
  assert.equal(logoutCalls, 1);
  assert.equal(businessCalls, 0);
  assert.equal(state.status, "anonymous");
});

test("退出网络失败保留当前会话，允许再次退出", async () => {
  let calls = 0;
  let state;
  const auth = createAuthClient({
    fetcher: async (path) => {
      if (path.endsWith("/login")) return token();
      if (++calls === 1) throw new Error("网络断开");
      return Response.json({ code: 200 });
    },
  });
  auth.subscribe((value) => {
    state = value;
  });
  await auth.login("alice", "pw");
  await assert.rejects(auth.logout(), /网络断开/);
  assert.equal(state.status, "authenticated");
  await auth.logout();
  assert.equal(state.status, "anonymous");
});
