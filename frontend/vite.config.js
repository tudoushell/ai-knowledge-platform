import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxy = {
    "/api": {
      target: env.API_PROXY_TARGET || "http://localhost:8090",
      changeOrigin: true,
      // 流式问答可能耗时较长，代理不提前中断连接。
      timeout: 0,
      proxyTimeout: 0,
    },
  };
  return {
    plugins: [vue()],
    server: { port: 5173, strictPort: true, proxy },
    preview: { port: 4173, strictPort: true, proxy },
  };
});
