"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { PlayerBrawlerCard } from "./PlayerBrawlerCard";

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  if (brawlers.length === 0) {
    return <p className="text-sm text-muted-foreground">보유한 브롤러가 없습니다.</p>;
  }
  const sorted = [...brawlers].sort((a, b) => b.trophies - a.trophies);
  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
      {sorted.map((pb) => (
        <li key={`${pb.brawler?.id ?? "x"}-${pb.brawler?.name ?? "x"}`}>
          <PlayerBrawlerCard pb={pb} />
        </li>
      ))}
    </ul>
  );
}
