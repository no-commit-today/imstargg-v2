const NUMBER_FORMATTER = new Intl.NumberFormat("ko-KR");

export function formatNumber(n: number): string {
  return NUMBER_FORMATTER.format(n);
}
