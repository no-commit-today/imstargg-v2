"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  STATS_TIER_RANGES,
  STATS_DATE_RANGES,
  TIER_RANGE_LABELS,
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

type Props = {
  tier: StatsTierRange;
  date: StatsDateRange;
};

export function StatRangeSelector({ tier, date }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function navigate(next: { tier?: StatsTierRange; date?: StatsDateRange }) {
    const sp = new URLSearchParams(params);
    const newTier = next.tier ?? tier;
    const newDate = next.date ?? date;
    if (newTier === DEFAULT_TIER_RANGE) sp.delete("tier");
    else sp.set("tier", newTier);
    if (newDate === DEFAULT_DATE_RANGE) sp.delete("date");
    else sp.set("date", newDate);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Select value={tier} onValueChange={(v) => navigate({ tier: v as StatsTierRange })}>
        <SelectTrigger className="w-40 text-sm">
          <SelectValue>{TIER_RANGE_LABELS[tier]}</SelectValue>
        </SelectTrigger>
        <SelectContent>
          {STATS_TIER_RANGES.map((t) => (
            <SelectItem key={t} value={t}>
              {TIER_RANGE_LABELS[t]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={date} onValueChange={(v) => navigate({ date: v as StatsDateRange })}>
        <SelectTrigger className="w-32 text-sm">
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
