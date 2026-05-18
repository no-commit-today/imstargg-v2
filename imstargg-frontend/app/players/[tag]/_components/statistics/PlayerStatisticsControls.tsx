"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  BrawlPassSeasonResponse,
  SoloRankSeasonResponse,
} from "@/api/types";
import type { PlayerStatBattleType } from "@/hooks/queries/usePlayerStatistics";

const TOGGLE_OPTIONS: { value: PlayerStatBattleType; label: string }[] = [
  { value: "RANKED", label: "트로피전" },
  { value: "SOLO_RANKED", label: "경쟁전" },
];

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
      <div
        role="tablist"
        aria-label="전투 유형"
        className="inline-flex rounded-md border border-[#B4B9C7] bg-white p-1 shadow-[0_1px_2px_rgba(20,22,27,0.06)]"
      >
        {TOGGLE_OPTIONS.map((opt) => {
          const active = battleType === opt.value;
          return (
            <button
              key={opt.value}
              type="button"
              role="tab"
              aria-selected={active}
              aria-pressed={active}
              onClick={() => onBattleTypeChange(opt.value)}
              className={
                active
                  ? "rounded px-3 py-1 text-sm font-bold bg-[#FFC60A] text-[#14161B] shadow-[0_1px_0_#E8A900] transition"
                  : "rounded px-3 py-1 text-sm font-bold text-[#5C6271] hover:bg-[#F6F7FA] hover:text-[#14161B] transition"
              }
            >
              {opt.label}
            </button>
          );
        })}
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
