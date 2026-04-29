"use client";

import { Input } from "@/components/ui/input";

type Props = {
  search: string;
  onSearchChange: (s: string) => void;
  totalBattleCount: number;
};

export function BrawlerListHeader({ search, onSearchChange, totalBattleCount }: Props) {
  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#5C6271]">
          BRAWLERS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">브롤러 메타</h1>
        <p className="mt-1 text-xs text-[#5C6271]">
          전체 전투 표본: <span className="font-num tabular-nums">{totalBattleCount.toLocaleString("ko-KR")}</span>
        </p>
      </div>
      <Input
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder="브롤러 이름 검색"
        className="sm:w-56"
      />
    </div>
  );
}
