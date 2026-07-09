"use client";

import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  STATS_TIER_RANGES,
  STATS_DATE_RANGES,
  TIER_RANGE_LABELS,
  TIER_RANGE_ICONS,
  DATE_RANGE_LABELS,
  DEFAULT_TIER_RANGE,
  DEFAULT_DATE_RANGE,
  type StatsTierRange,
  type StatsDateRange,
} from "@/domain/statistics/enums";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

function TierIcon({ tier }: { tier: StatsTierRange }) {
  return (
    <Image
      src={TIER_RANGE_ICONS[tier]}
      alt=""
      width={16}
      height={16}
      unoptimized
      className="h-4 w-4 shrink-0 object-contain"
    />
  );
}

type ViewProps = {
  tier: StatsTierRange;
  date: StatsDateRange;
  onSelect: (next: { tier: StatsTierRange; date: StatsDateRange }) => void;
};

/**
 * 프레젠테이션 전용 셀렉터 — 내비게이션 훅이 없어 정적(ISR) 페이지의
 * 클라이언트 패널에서 프리렌더 bailout 없이 사용할 수 있다.
 */
export function StatRangeSelectorView({ tier, date, onSelect }: ViewProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Select
        value={tier}
        onValueChange={(v) => onSelect({ tier: v as StatsTierRange, date })}
      >
        <SelectTrigger aria-label="티어 범위 선택" className="w-40 text-sm">
          <SelectValue>
            <span className="inline-flex items-center gap-1.5">
              <TierIcon tier={tier} />
              {TIER_RANGE_LABELS[tier]}
            </span>
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {STATS_TIER_RANGES.map((t) => (
            <SelectItem key={t} value={t}>
              <span className="inline-flex items-center gap-1.5">
                <TierIcon tier={t} />
                {TIER_RANGE_LABELS[t]}
              </span>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select
        value={date}
        onValueChange={(v) => onSelect({ tier, date: v as StatsDateRange })}
      >
        <SelectTrigger aria-label="기간 선택" className="w-32 text-sm">
          <SelectValue>{DATE_RANGE_LABELS[date]}</SelectValue>
        </SelectTrigger>
        <SelectContent>
          {STATS_DATE_RANGES.map((d) => (
            <SelectItem key={d} value={d}>
              {DATE_RANGE_LABELS[d]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

type Props = {
  tier: StatsTierRange;
  date: StatsDateRange;
};

/** 동적(서버 렌더) 목록 페이지용 — 선택 시 URL 내비게이션으로 서버 재렌더. */
export function StatRangeSelector({ tier, date }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function handleSelect(next: { tier: StatsTierRange; date: StatsDateRange }) {
    const sp = new URLSearchParams(params);
    if (next.tier === DEFAULT_TIER_RANGE) sp.delete("tier");
    else sp.set("tier", next.tier);
    if (next.date === DEFAULT_DATE_RANGE) sp.delete("date");
    else sp.set("date", next.date);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return <StatRangeSelectorView tier={tier} date={date} onSelect={handleSelect} />;
}
