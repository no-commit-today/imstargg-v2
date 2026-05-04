"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import type {
  BrawlPassSeasonResponse,
  SoloRankSeasonResponse,
} from "@/api/types";
import type { PlayerStatBattleType } from "@/hooks/queries/usePlayerStatistics";

interface Props {
  battleType: PlayerStatBattleType;
  onBattleTypeChange: (v: PlayerStatBattleType) => void;
  brawlPassSeasons: BrawlPassSeasonResponse[];
  soloRankSeasons: SoloRankSeasonResponse[];
  brawlPassSeasonNumber: number | null;
  soloRankSeasonKey: string | null;
  onBrawlPassChange: (n: number) => void;
  onSoloRankChange: (k: string) => void;
}

export function PlayerStatisticsControls({
  battleType,
  onBattleTypeChange,
  brawlPassSeasons,
  soloRankSeasons,
  brawlPassSeasonNumber,
  soloRankSeasonKey,
  onBrawlPassChange,
  onSoloRankChange,
}: Props) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex rounded-md border border-[#D8DCE5] bg-white p-0.5">
        <Button
          variant={battleType === "RANKED" ? "default" : "ghost"}
          size="sm"
          onClick={() => onBattleTypeChange("RANKED")}
        >
          랭크전
        </Button>
        <Button
          variant={battleType === "SOLO_RANKED" ? "default" : "ghost"}
          size="sm"
          onClick={() => onBattleTypeChange("SOLO_RANKED")}
        >
          경쟁전
        </Button>
      </div>

      {battleType === "RANKED" ? (
        <Select
          value={brawlPassSeasonNumber?.toString() ?? ""}
          onValueChange={(v) => onBrawlPassChange(Number(v))}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="시즌 선택" />
          </SelectTrigger>
          <SelectContent>
            {[...brawlPassSeasons].reverse().map((s) => (
              <SelectItem key={s.number} value={s.number.toString()}>
                시즌 {s.number}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      ) : (
        <Select
          value={soloRankSeasonKey ?? ""}
          onValueChange={onSoloRankChange}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="시즌 선택" />
          </SelectTrigger>
          <SelectContent>
            {[...soloRankSeasons].reverse().map((s) => (
              <SelectItem key={s.key} value={s.key}>
                시즌 {s.number}-{s.month}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}
    </div>
  );
}
