"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { Card } from "@/components/ui/card";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { formatNumber } from "@/lib/format";
import { PlayerBrawlerOwnership } from "./PlayerBrawlerOwnership";

export function PlayerBrawlerCard({ pb }: { pb: PlayerBrawler }) {
  return (
    <Card className="flex flex-col gap-2 p-3 text-sm">
      <div className="flex flex-col items-center gap-1 text-center">
        <BrawlerAvatar
          name={pb.brawler?.name ?? "?"}
          imageUrl={pb.brawler?.portraitImageUrl ?? null}
          rarity={pb.brawler?.rarity ?? null}
          size={80}
          showFrame
        />
        <p className="font-medium">{pb.brawler?.name ?? "Unknown"}</p>
        <p className="text-muted-foreground text-xs">
          파워 {pb.power} · 🏆 {formatNumber(pb.trophies)}
        </p>
      </div>
      <hr className="border-muted" />
      <PlayerBrawlerOwnership pb={pb} />
    </Card>
  );
}
