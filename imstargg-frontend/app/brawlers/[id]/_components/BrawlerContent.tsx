"use client";

import type { BrawlerWithItems } from "@/domain/brawler/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import type { ItemOwnership } from "@/domain/statistics/types";
import { useBrawler } from "@/hooks/queries/useBrawler";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";

type Props = {
  initialDetail: BrawlerWithItems;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

export function BrawlerContent({ initialDetail, ownership, trophy }: Props) {
  const { data: detail = initialDetail } = useBrawler(initialDetail.brawler.id, {
    initialData: initialDetail,
  });
  return (
    <section className="py-6 space-y-5">
      <BrawlerHero brawler={detail.brawler} />
      <BrawlerItems items={detail.items} ownership={ownership} trophy={trophy} />
    </section>
  );
}
