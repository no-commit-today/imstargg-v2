"use client";

import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Card } from "@/components/ui/card";
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

export function BrawlerCard({ brawler }: { brawler: Brawler }) {
  return (
    <Link href={`/brawlers/${brawler.id}`} className="block">
      <Card className="flex flex-col items-center gap-2 p-3 transition hover:border-foreground">
        <BrawlerAvatar
          name={brawler.name}
          imageUrl={brawler.portraitImageUrl}
          rarity={brawler.rarity}
          size={88}
          showFrame
        />
        <div className="w-full truncate text-center text-sm font-bold">
          {brawler.name}
        </div>
        <div className="flex flex-wrap items-center justify-center gap-1">
          <Badge variant="secondary" className="text-[10px]">
            {ROLE_LABEL[brawler.role]}
          </Badge>
          <Badge variant="outline" className="text-[10px]">
            {RARITY_LABEL[brawler.rarity]}
          </Badge>
        </div>
      </Card>
    </Link>
  );
}
