"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { Card } from "@/components/ui/card";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";
import PowerLevel from "@/components/primitives/PowerLevel";
import PrestigeBadge from "@/components/primitives/PrestigeBadge";
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
        <div className="flex items-center justify-center gap-1.5">
          <PowerLevel level={pb.power} size={20} />
          <PrestigeBadge trophies={pb.trophies} size={18} />
          <BrawlerTrophy value={pb.trophies} size={14} />
        </div>
      </div>
      <hr className="border-muted" />
      <PlayerBrawlerOwnership pb={pb} />
    </Card>
  );
}
