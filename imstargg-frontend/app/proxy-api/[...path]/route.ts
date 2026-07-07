import type { NextRequest } from "next/server";

/**
 * E2E 전용 백엔드 프록시. `PROXY_API_TARGET` 이 설정된 경우에만 활성화된다.
 *
 * next.config rewrite 가 아닌 Route Handler 인 이유: rewrite 프록시는 브라우저가
 * POST 에 자동으로 붙이는 `Origin: http://localhost:3000` 헤더를 업스트림에 그대로
 * 전달해 prod CORS(허용 origin: imstargg.com)가 403 "Invalid CORS request" 로
 * 거부한다. 여기서는 업스트림 요청을 새로 구성하므로 Origin 이 전달되지 않는다.
 */
const PROXY_API_TARGET = process.env.PROXY_API_TARGET;

async function proxy(
  req: NextRequest,
  { params }: { params: Promise<{ path: string[] }> },
) {
  if (!PROXY_API_TARGET) return new Response("Not Found", { status: 404 });
  const { path } = await params;
  const url = `${PROXY_API_TARGET}/${path.map(encodeURIComponent).join("/")}${req.nextUrl.search}`;
  const contentType = req.headers.get("content-type");
  const upstream = await fetch(url, {
    method: req.method,
    headers: {
      Accept: req.headers.get("accept") ?? "application/json",
      ...(contentType ? { "Content-Type": contentType } : {}),
    },
    body:
      req.method === "GET" || req.method === "HEAD"
        ? undefined
        : await req.arrayBuffer(),
    cache: "no-store",
    redirect: "manual",
  });
  const upstreamContentType = upstream.headers.get("content-type");
  return new Response(upstream.body, {
    status: upstream.status,
    headers: upstreamContentType
      ? { "Content-Type": upstreamContentType }
      : undefined,
  });
}

export { proxy as GET, proxy as POST };
