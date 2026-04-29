type RateProps = { value: number; fractionDigits?: number };

export function PercentCell({ value, fractionDigits = 1 }: RateProps) {
  return (
    <span className="font-num font-bold tabular-nums">
      {(value * 100).toFixed(fractionDigits)}%
    </span>
  );
}

export function AverageRankCell({ value }: { value: number }) {
  return (
    <span className="font-num font-bold tabular-nums">{value.toFixed(2)}</span>
  );
}

export function CountCell({ value }: { value: number }) {
  return (
    <span className="font-num tabular-nums text-[#5C6271]">
      {value.toLocaleString("ko-KR")}
    </span>
  );
}
