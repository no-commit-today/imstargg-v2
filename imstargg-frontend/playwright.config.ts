import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,
  reporter: "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "pnpm dev",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    env: {
      // E2E 에서는 dev 서버 자체가 백엔드 프록시 역할을 수행한다.
      // 클라이언트 fetch 가 same-origin 으로 가도록 base URL 을 localhost:3000/proxy-api 로 지정.
      NEXT_PUBLIC_API_BASE_URL: "http://localhost:3000/proxy-api",
      PROXY_API_TARGET: "https://api.imstargg.com",
    },
  },
});
