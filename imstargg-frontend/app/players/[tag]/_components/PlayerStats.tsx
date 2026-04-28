"use client";

import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";

export function PlayerStats({ player }: { player: Player }) {
  return (
    <Card className="grid grid-cols-2 gap-3 p-4 text-sm">
      <Row label="현재 트로피" value={formatNumber(player.trophies)} />
      <Row label="최고 트로피" value={formatNumber(player.highestTrophies)} />
      <Row label="총 프레스티지" value={formatNumber(player.totalPrestigeLevel)} />
      <Row label="EXP 포인트" value={formatNumber(player.expPoints)} />
    </Card>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b py-1 last:border-b-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
