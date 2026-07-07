import type { NextConfig } from "next";
import packageJson from "./package.json";

/**
 * E2E 백엔드 프록시는 `app/proxy-api/[...path]/route.ts` (Route Handler) 가 담당한다.
 * `PROXY_API_TARGET` 미설정(prod) 시 해당 경로는 404.
 */
const nextConfig: NextConfig = {
  env: {
    NEXT_PUBLIC_APP_VERSION: packageJson.version,
  },
};

export default nextConfig;
