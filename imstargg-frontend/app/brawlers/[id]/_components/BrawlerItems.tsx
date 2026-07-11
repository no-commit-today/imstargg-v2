"use client";

import { useEffect, useState } from "react";
import type { BrawlerItems as BrawlerItemsModel } from "@/domain/brawler/types";
import {
  buildBrawlerItemGroups,
  type BrawlerItemSection,
} from "@/domain/brawler/itemOwnership";
import {
  DEFAULT_TROPHY_RANGE,
  parseTrophyRange,
  type BrawlerTrophyRange,
} from "@/domain/statistics/enums";
import type { ItemOwnership } from "@/domain/statistics/types";
import { useItemOwnership } from "@/hooks/queries/useItemOwnership";
import { applySearchShallow } from "@/lib/shallowSearch";
import { cn } from "@/lib/cn";
import { TrophyRangeSelector } from "@app/_components/statistics/TrophyRangeSelector";
import { BrawlerItemCard } from "./BrawlerItemCard";

type Props = {
  items: BrawlerItemsModel;
  brawlerId: number;
  initialOwnership: ItemOwnership;
};

function hasOwnershipRows(ownership: ItemOwnership) {
  return (
    ownership.gadgets.length > 0 ||
    ownership.gears.length > 0 ||
    ownership.starPowers.length > 0 ||
    ownership.hyperCharges.length > 0 ||
    ownership.buffies.length > 0
  );
}

function ItemSection({ section }: { section: BrawlerItemSection }) {
  if (section.items.length === 0) return null;

  return (
    <section className="flex min-w-0 flex-col gap-2">
      <h3 className="text-sm font-bold">
        {section.title}{" "}
        <span className="text-muted-foreground">({section.items.length})</span>
      </h3>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {section.items.map((item) => (
          <BrawlerItemCard key={item.key} item={item} />
        ))}
      </div>
    </section>
  );
}

export function BrawlerItems({ items, brawlerId, initialOwnership }: Props) {
  const [trophy, setTrophy] = useState<BrawlerTrophyRange>(DEFAULT_TROPHY_RANGE);

  useEffect(() => {
    const sync = () => {
      const sp = new URLSearchParams(window.location.search);
      setTrophy(parseTrophyRange(sp.get("trophy") ?? undefined));
    };
    sync();
    window.addEventListener("popstate", sync);
    return () => window.removeEventListener("popstate", sync);
  }, []);

  const query = useItemOwnership(brawlerId, trophy, initialOwnership);
  const ownership = query.data ?? initialOwnership;

  function handleTrophyChange(next: BrawlerTrophyRange) {
    setTrophy(next);
    applySearchShallow({ trophy: next === DEFAULT_TROPHY_RANGE ? null : next });
  }

  const groups = buildBrawlerItemGroups(items, ownership);
  const isOwnershipEmpty = !hasOwnershipRows(ownership);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <h2 className="text-lg font-bold">아이템</h2>
        <TrophyRangeSelector trophy={trophy} onSelect={handleTrophyChange} />
      </div>
      {isOwnershipEmpty && (
        <div className="rounded-lg border bg-card px-4 py-3 text-sm text-muted-foreground">
          보유율 데이터가 없습니다. 아이템은 데이터 없음 상태로 표시됩니다.
        </div>
      )}
      <div className="flex flex-col gap-5">
        {groups.map((group) => (
          <div
            key={group.key}
            className={cn(
              "grid grid-cols-1 gap-4",
              group.key !== "gears" && "lg:grid-cols-2",
            )}
          >
            {group.sections.map((section) => (
              <ItemSection key={section.key} section={section} />
            ))}
          </div>
        ))}
      </div>
    </section>
  );
}
