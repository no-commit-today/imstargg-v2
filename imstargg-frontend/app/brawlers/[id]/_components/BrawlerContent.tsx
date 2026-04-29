"use client";

import type { Brawler } from "@/domain/brawler/types";
import { useBrawler } from "@/hooks/queries/useBrawler";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";
import { BrawlerBuffies } from "./BrawlerBuffies";

export function BrawlerContent({ initialBrawler }: { initialBrawler: Brawler }) {
  const { data: brawler = initialBrawler } = useBrawler(initialBrawler.id, {
    initialData: initialBrawler,
  });
  return (
    <section className="py-6 space-y-5">
      <BrawlerHero brawler={brawler} />
      <BrawlerItems brawler={brawler} />
      {brawler.buffies.length > 0 && <BrawlerBuffies buffies={brawler.buffies} />}
    </section>
  );
}
