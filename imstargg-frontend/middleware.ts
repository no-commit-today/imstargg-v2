import { NextResponse, type NextRequest } from "next/server";

/**
 * E2E 프록시(`/proxy-api` rewrite, next.config.ts) 보조 미들웨어.
 *
 * 브라우저는 same-origin 이라도 POST 등 mutating 요청에는 항상 `Origin` 헤더를
 * 붙인다(GET 은 붙이지 않음). rewrite 는 이 헤더를 그대로 백엔드에 전달하는데,
 * prod core-api 의 CORS 허용 목록에 localhost 가 없어 POST 만 403
 * ("Invalid CORS request") 으로 거부된다. 프록시의 목적이 CORS 우회이므로
 * 백엔드로 나가기 전에 Origin 을 제거한다. `PROXY_API_TARGET` 미설정(prod)
 * 시에는 아무것도 하지 않는다.
 */
export function middleware(request: NextRequest) {
  if (!process.env.PROXY_API_TARGET) return NextResponse.next();
  const headers = new Headers(request.headers);
  headers.delete("origin");
  return NextResponse.next({ request: { headers } });
}

export const config = {
  matcher: "/proxy-api/:path*",
};
