type RateProps = {
  value: number;
  fractionDigits?: number;
  /** true 면 숫자 옆에 미니 게이지 바를 함께 렌더한다. */
  gauge?: boolean;
  /** 게이지 색상 톤. 기본은 win(green). */
  gaugeTone?: "win" | "loss" | "neutral";
};

function clamp01(v: number): number {
  if (Number.isNaN(v)) return 0;
  if (v < 0) return 0;
  if (v > 1) return 1;
  return v;
}

export function PercentCell({ value, fractionDigits = 1, gauge, gaugeTone = "win" }: RateProps) {
  const text = (
    <span className="font-num font-bold tabular-nums">
      {(value * 100).toFixed(fractionDigits)}%
    </span>
  );
  if (!gauge) return text;
  const pct = clamp01(value) * 100;
  return (
    <span className="win-gauge">
      {text}
      <span className="win-gauge-track hidden sm:inline-block" aria-hidden>
        <span
          className="win-gauge-fill"
          data-tone={gaugeTone === "win" ? undefined : gaugeTone}
          style={{ width: `${pct}%` }}
        />
      </span>
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
