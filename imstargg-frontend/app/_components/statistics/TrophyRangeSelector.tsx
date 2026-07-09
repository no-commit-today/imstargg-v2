"use client";

import Image from "next/image";
import {
  BRAWLER_TROPHY_RANGES,
  TROPHY_RANGE_LABELS,
  TROPHY_RANGE_ICON,
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
  onSelect: (next: BrawlerTrophyRange) => void;
};

export function TrophyRangeSelector({ trophy, onSelect }: Props) {
  return (
    <Select value={trophy} onValueChange={(v) => onSelect(v as BrawlerTrophyRange)}>
      <SelectTrigger aria-label="트로피 구간 선택" className="w-28 text-sm">
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
