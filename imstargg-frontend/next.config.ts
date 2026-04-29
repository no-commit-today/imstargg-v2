import type { NextConfig } from "next";

/**
 * E2E 테스트 시 prod CORS 정책을 우회하기 위해 Next.js 가 백엔드로의 프록시 역할을 한다.
 * `PROXY_API_TARGET` 이 설정되면 `/proxy-api/...` 가 해당 origin 으로 프록시된다.
 * 클라이언트는 same-origin (`http://localhost:3000/proxy-api/...`) 으로 요청하므로 CORS 가 발생하지 않는다.
 * prod 빌드에서는 환경변수 미설정 → rewrite 비활성.
 */
const PROXY_API_TARGET = process.env.PROXY_API_TARGET;

const nextConfig: NextConfig = {
  async rewrites() {
    if (!PROXY_API_TARGET) return [];
    return [
      {
        source: "/proxy-api/:path*",
        destination: `${PROXY_API_TARGET}/:path*`,
      },
    ];
  },
};

export default nextConfig;
