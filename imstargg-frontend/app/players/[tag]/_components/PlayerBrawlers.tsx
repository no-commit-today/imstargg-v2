"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  if (brawlers.length === 0) {
    return <p className="text-sm text-muted-foreground">보유한 브롤러가 없습니다.</p>;
  }
  const sorted = [...brawlers].sort((a, b) => b.trophies - a.trophies);
  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
      {sorted.map((b) => (
        <li key={`${b.id}-${b.name}`}>
          <Card className="flex flex-col items-center gap-1 p-3 text-center text-sm">
            <BrawlerAvatar
              name={b.name}
              imageUrl={b.portraitImageUrl}
              rarity={b.rarity}
              size={80}
              showFrame
            />
            <p className="font-medium">{b.name}</p>
            <p className="text-muted-foreground">
              파워 {b.power} · 🏆 {formatNumber(b.trophies)}
            </p>
          </Card>
        </li>
      ))}
    </ul>
  );
}
