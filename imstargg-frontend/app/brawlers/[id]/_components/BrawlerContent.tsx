"use client";

import type { BrawlerDetail } from "@/domain/brawler/types";
import { useBrawler } from "@/hooks/queries/useBrawler";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";
import { BrawlerBuffies } from "./BrawlerBuffies";

export function BrawlerContent({ initialDetail }: { initialDetail: BrawlerDetail }) {
  const { data: detail = initialDetail } = useBrawler(initialDetail.brawler.id, {
    initialData: initialDetail,
  });
  return (
    <section className="py-6 space-y-5">
      <BrawlerHero brawler={detail.brawler} />
      <BrawlerItems items={detail.items} />
      {detail.items.buffies && <BrawlerBuffies buffies={detail.items.buffies} />}
    </section>
  );
}
