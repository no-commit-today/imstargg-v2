"use client";

import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Badge } from "@/components/ui/badge";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";

export function BrawlerHero({ brawler }: { brawler: Brawler }) {
  return (
    <section className="flex flex-col items-center gap-4 rounded-lg border bg-card p-6 sm:flex-row sm:gap-6">
      <BrawlerAvatar
        name={brawler.name}
        imageUrl={brawler.portraitImageUrl}
        rarity={brawler.rarity}
        size={140}
        showFrame
      />
      <div className="flex-1 text-center sm:text-left">
        <h1 className="font-display text-3xl tracking-[0.02em]">{brawler.name}</h1>
        <div className="mt-2 flex flex-wrap justify-center gap-2 sm:justify-start">
          <Badge variant="secondary">{ROLE_LABEL_KO[brawler.role]}</Badge>
          <Badge variant="outline">{RARITY_LABEL_KO[brawler.rarity]}</Badge>
        </div>
      </div>
    </section>
  );
}
