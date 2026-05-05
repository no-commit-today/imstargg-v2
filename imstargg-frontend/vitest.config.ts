import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@app": fileURLToPath(new URL("./app", import.meta.url)),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./tests/setup/vitest.setup.ts"],
    css: false,
    include: ["tests/**/*.test.{ts,tsx}"],
    exclude: ["tests/e2e/**"],
    env: {
      NEXT_PUBLIC_API_BASE_URL: "https://core-api.test",
      NEXT_PUBLIC_SITE_URL: "https://imstargg.test",
      NEXT_PUBLIC_APP_VERSION: "0.0.0-test",
      NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT: "ca-pub-5084387143997125",
    },
  },
});
