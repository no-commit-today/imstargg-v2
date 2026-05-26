"use client";

import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  BRAWLER_TROPHY_RANGES,
  TROPHY_RANGE_LABELS,
  TROPHY_RANGE_ICON,
  DEFAULT_TROPHY_RANGE,
  type BrawlerTrophyRange,
} from "@/domain/statistics/enums";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type Props = {
  trophy: BrawlerTrophyRange;
  paramName?: string; // default "trophy"
};

export function TrophyRangeSelector({ trophy, paramName = "trophy" }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function navigate(next: BrawlerTrophyRange) {
    const sp = new URLSearchParams(params);
    if (next === DEFAULT_TROPHY_RANGE) sp.delete(paramName);
    else sp.set(paramName, next);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <Select value={trophy} onValueChange={(v) => navigate(v as BrawlerTrophyRange)}>
      <SelectTrigger className="w-28 text-sm">
        <SelectValue>
          <span className="inline-flex items-center gap-1.5">
            <Image
              src={TROPHY_RANGE_ICON}
              alt=""
              width={16}
              height={16}
              unoptimized
              className="h-4 w-4 shrink-0 object-contain"
            />
            {TROPHY_RANGE_LABELS[trophy]}
          </span>
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {BRAWLER_TROPHY_RANGES.map((t) => (
          <SelectItem key={t} value={t}>
            <span className="inline-flex items-center gap-1.5">
              <Image
                src={TROPHY_RANGE_ICON}
                alt=""
                width={16}
                height={16}
                unoptimized
                className="h-4 w-4 shrink-0 object-contain"
              />
              {TROPHY_RANGE_LABELS[t]}
            </span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
