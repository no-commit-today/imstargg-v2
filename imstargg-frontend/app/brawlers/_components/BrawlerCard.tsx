"use client";

import Link from "next/link";
import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";

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
            {ROLE_LABEL_KO[brawler.role]}
          </Badge>
          <Badge variant="outline" className="text-[10px]">
            {RARITY_LABEL_KO[brawler.rarity]}
          </Badge>
        </div>
      </Card>
    </Link>
  );
}
