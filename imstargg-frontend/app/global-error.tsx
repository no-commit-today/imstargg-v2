"use client";

import { useEffect } from "react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <html lang="ko">
      <body style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 16, padding: "96px 16px", fontFamily: "system-ui, sans-serif" }}>
        <h1 style={{ fontSize: 24, fontWeight: 600 }}>문제가 발생했어요</h1>
        <p style={{ color: "#5C6271" }}>잠시 후 다시 시도해 주세요.</p>
        <button onClick={reset} style={{ padding: "8px 16px", borderRadius: 8, border: "1px solid #D8DCE5" }}>다시 시도</button>
      </body>
    </html>
  );
}
