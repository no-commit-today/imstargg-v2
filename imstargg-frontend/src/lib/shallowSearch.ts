export function mergeSearchParams(
  current: string,
  updates: Record<string, string | null>,
): string {
  const sp = new URLSearchParams(current);
  for (const [key, value] of Object.entries(updates)) {
    if (value === null) sp.delete(key);
    else sp.set(key, value);
  }
  const qs = sp.toString();
  return qs ? `?${qs}` : "";
}

/**
 * 서버 왕복 없이 현재 URL 의 쿼리스트링만 갱신한다 (Next shallow routing).
 * 정적(ISR) 상세 페이지의 필터 상태 영속화 전용 — RSC 재렌더를 유발하지 않는다.
 */
export function applySearchShallow(updates: Record<string, string | null>): void {
  const qs = mergeSearchParams(window.location.search, updates);
  window.history.replaceState(null, "", `${window.location.pathname}${qs}`);
}
