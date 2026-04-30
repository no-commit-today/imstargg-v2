const NUMBER_FORMATTER = new Intl.NumberFormat("ko-KR");

export function formatNumber(n: number): string {
  return NUMBER_FORMATTER.format(n);
}

export function formatRelativeTime(
  iso: string | Date,
  now: Date = new Date(),
): string {
  const then = typeof iso === "string" ? new Date(iso) : iso;
  const diffSec = Math.floor((now.getTime() - then.getTime()) / 1000);
  if (diffSec < 10) return "방금 전";
  if (diffSec < 60) return `${diffSec}초 전`;
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}분 전`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 30) return `${diffDay}일 전`;
  return then.toLocaleDateString("ko-KR");
}

export function formatMinutesAgo(min: number): string {
  if (min < 1) return "방금 전";
  if (min < 60) return `${min}분 전`;
  const hours = Math.floor(min / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  return `${days}일 전`;
}
