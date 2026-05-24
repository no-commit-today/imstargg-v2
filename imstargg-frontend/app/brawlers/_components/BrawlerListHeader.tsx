"use client";

import { Input } from "@/components/ui/input";

type Props = {
  search: string;
  onSearchChange: (s: string) => void;
  totalBattleCount: number;
};

export function BrawlerListHeader({ search, onSearchChange, totalBattleCount }: Props) {
  return (
    <div className="data-table-toolbar">
      <div className="min-w-0">
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#5C6271]">
          BRAWLERS
        </div>
        <p className="mt-1 text-xs text-[#5C6271]">
          전체 전투 표본:{" "}
          <span className="font-num tabular-nums text-[#14161B]">
            {totalBattleCount.toLocaleString("ko-KR")}
          </span>
        </p>
      </div>
      <Input
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder="브롤러 이름 검색"
        className="w-full max-w-[220px]"
      />
    </div>
  );
}
