// Access Token 仅保存在内存；Refresh Token 由浏览器的 HttpOnly Cookie 管理。
export function createAuthClient({
  fetcher = (...args) => fetch(...args),
  now = Date.now,
} = {}) {
  let token = "";
  let expiresAt = 0;
  let generation = 0;
  let refreshPromise;
  let logoutPromise;
  let state = { status: "checking", username: "", error: "" };
  const listeners = new Set();
  const publish = (patch) => {
    state = { ...state, ...patch };
    listeners.forEach((listener) => listener({ ...state }));
  };
  const clear = (message = "") => {
    generation++;
    token = "";
    expiresAt = 0;
    publish({ status: "anonymous", username: "", error: message });
  };

  async function authRequest(action, data) {
    const response = await fetcher(`/api/auth/${action}`, {
      method: "POST",
      credentials: "include",
      headers: {
        "X-Requested-With": "XMLHttpRequest",
        ...(data ? { "Content-Type": "application/json" } : {}),
      },
      ...(data ? { body: JSON.stringify(data) } : {}),
    });
    const body = await response.json().catch(() => null);
    if (!response.ok || body?.code !== 200) {
      const error = new Error(
        body?.message || `认证请求失败（HTTP ${response.status}）`,
      );
      error.status = body?.code || response.status;
      throw error;
    }
    return body.data;
  }

  function accept(data) {
    if (
      !data?.accessToken ||
      !Number.isFinite(data.expiresIn) ||
      data.expiresIn <= 0
    ) {
      throw new Error("登录响应不完整，请联系管理员");
    }
    token = data.accessToken;
    expiresAt = now() + data.expiresIn * 1000;
    publish({
      status: state.status === "logging-out" ? "logging-out" : "authenticated",
      username: data.username || state.username,
      error: "",
    });
    return token;
  }

  function refresh() {
    if (refreshPromise) return refreshPromise;
    const version = generation;
    const execute = async () => {
      try {
        const data = await authRequest("refresh");
        if (version !== generation) throw new Error("登录状态已变更，请重试");
        return accept(data);
      } catch (error) {
        if (version === generation && error.status === 401)
          clear("登录已过期，请重新登录");
        throw error;
      }
    };
    // 不同标签页也串行发送刷新请求，每次发送时由浏览器读取最新 Cookie。
    const operation = globalThis.navigator?.locks
      ? navigator.locks.request("atlas-auth-refresh", execute)
      : execute();
    refreshPromise = operation.finally(() => {
      refreshPromise = undefined;
    });
    return refreshPromise;
  }

  async function unauthorized(response) {
    if (response.status === 401) return true;
    if (
      !(response.headers.get("content-type") || "").includes("application/json")
    )
      return false;
    return (
      (
        await response
          .clone()
          .json()
          .catch(() => null)
      )?.code === 401
    );
  }

  return {
    subscribe(listener) {
      listeners.add(listener);
      listener({ ...state });
      return () => listeners.delete(listener);
    },
    async restore() {
      publish({ status: "checking", error: "" });
      try {
        await refresh();
      } catch (error) {
        if (error.status === 401) publish({ error: "" });
        else if (state.status !== "anonymous")
          publish({ status: "unavailable", error: error.message });
      }
    },
    async login(username, password) {
      await logoutPromise?.catch(() => {});
      await refreshPromise?.catch(() => {});
      generation++;
      const data = await authRequest("login", { username, password });
      publish({ username });
      accept(data);
    },
    logout() {
      if (logoutPromise) return logoutPromise;
      // 等待刷新完成，再用最新 Cookie 退出，避免旧请求把会话重新建立起来。
      publish({ status: "logging-out", error: "" });
      const execute = async () => {
        try {
          await refreshPromise?.catch(() => {});
          await authRequest("logout");
          clear();
        } catch (error) {
          publish({
            status: token ? "authenticated" : "anonymous",
            error: error.message,
          });
          throw error;
        }
      };
      logoutPromise = execute().finally(() => {
        logoutPromise = undefined;
      });
      return logoutPromise;
    },
    async fetch(path, options = {}) {
      options.signal?.throwIfAborted();
      if (logoutPromise) throw new Error("正在退出登录");
      if (!token || now() >= expiresAt - 10000) await refresh();
      const version = generation;
      const send = () => {
        options.signal?.throwIfAborted();
        if (logoutPromise) throw new Error("正在退出登录");
        if (version !== generation) throw new Error("登录状态已变更");
        const headers = new Headers(options.headers);
        headers.set("Authorization", `Bearer ${token}`);
        return fetcher(path, { ...options, headers, credentials: "include" });
      };
      const sentToken = token;
      let response = await send();
      if (version !== generation || state.status === "logging-out") {
        await response.body?.cancel();
        throw new Error("登录状态已变更");
      }
      if (await unauthorized(response)) {
        await response.body?.cancel();
        if (version !== generation || logoutPromise)
          throw new Error("登录状态已变更");
        // 别的请求若已刷新成功，直接使用新令牌；否则共享一次刷新。
        if (token === sentToken) await refresh();
        response = await send();
        if (await unauthorized(response)) {
          if (version !== generation || logoutPromise)
            throw new Error("登录状态已变更");
          clear("登录已失效，请重新登录");
          throw new Error("登录已失效，请重新登录");
        }
      }
      if (version !== generation) throw new Error("登录状态已变更");
      return response;
    },
  };
}

export const auth = createAuthClient();
