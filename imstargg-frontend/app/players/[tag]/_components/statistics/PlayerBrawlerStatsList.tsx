"use client";

import { useMemo } from "react";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { formatNumber } from "@/lib/format";
import type { PlayerBrawlerStat } from "@/domain/statistics/player";

export function PlayerBrawlerStatsList({ items }: { items: PlayerBrawlerStat[] }) {
  const sorted = useMemo(
    () => [...items].sort((a, b) => b.winRate - a.winRate),
    [items],
  );
  if (sorted.length === 0) {
    return (
      <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-12 text-center text-sm text-[#5C6271]">
        이 시즌 통계 데이터가 없습니다.
      </div>
    );
  }
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white">
      <div className="grid grid-cols-[40px_1fr_72px_72px_120px] items-center gap-2 border-b border-[#D8DCE5] bg-[#F6F7FA] px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-[#5C6271]">
        <span>#</span>
        <span>브롤러</span>
        <span className="text-right">전투수</span>
        <span className="text-right">승률</span>
        <span className="text-right">V-D-Dr</span>
      </div>
      {sorted.map((it, i) => (
        <div
          key={it.brawler?.id ?? `unknown-${i}`}
          className={`grid grid-cols-[40px_1fr_72px_72px_120px] items-center gap-2 border-b border-[#ECEEF3] px-3 py-2 text-sm ${it.totalBattleCount === 0 ? "opacity-40" : ""}`}
        >
          <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
          <div className="flex items-center gap-2 overflow-hidden">
            {it.brawler && (
              <BrawlerAvatar
                name={it.brawler.name}
                imageUrl={it.brawler.portraitImageUrl}
                rarity={it.brawler.rarity}
                size={28}
                className="shrink-0"
              />
            )}
            <span className="truncate font-medium">{it.brawler?.name ?? "-"}</span>
          </div>
          <span className="text-right tabular-nums">{formatNumber(it.totalBattleCount)}</span>
          <span className="text-right tabular-nums">{(it.winRate * 100).toFixed(1)}%</span>
          <span className="text-right tabular-nums text-[#5C6271]">
            {it.victoryCount}-{it.defeatCount}-{it.drawCount}
          </span>
        </div>
      ))}
    </div>
  );
}
