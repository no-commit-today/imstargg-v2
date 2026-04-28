"use client";

import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Badge } from "@/components/ui/badge";

const ROLE_LABEL: Record<Brawler["role"], string> = {
  DAMAGE_DEALER: "어태커",
  TANK: "탱커",
  MARKSMAN: "마크스맨",
  ASSASSIN: "어쌔신",
  SUPPORT: "서포터",
  CONTROLLER: "컨트롤러",
  ARTILLERY: "아틸러리",
};

const RARITY_LABEL: Record<Brawler["rarity"], string> = {
  COMMON: "기본",
  RARE: "레어",
  SUPER_RARE: "슈퍼 레어",
  EPIC: "에픽",
  MYTHIC: "미식",
  LEGENDARY: "레전더리",
  ULTRA_LEGENDARY: "울트라 레전더리",
};

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
          <Badge variant="secondary">{ROLE_LABEL[brawler.role]}</Badge>
          <Badge variant="outline">{RARITY_LABEL[brawler.rarity]}</Badge>
        </div>
      </div>
    </section>
  );
}
