import type { PlayerBrawlerRoleStat } from "@/domain/statistics/player";
import type { BrawlerRole } from "@/api/types";
import { Card } from "@/components/ui/card";
import ClassIcon from "@/components/primitives/ClassIcon";
import { ROLE_LABEL_KO } from "@/domain/brawler/labels";

const ROLE_COLOR: Record<BrawlerRole, string> = {
  DAMAGE_DEALER: "#FF3A3A",
  TANK: "#9146FF",
  MARKSMAN: "#FF8A1F",
  ASSASSIN: "#FF5DAA",
  SUPPORT: "#2BD576",
  CONTROLLER: "#3DE0FF",
  ARTILLERY: "#FFC60A",
};

export function PlayerBrawlerRoleSummary({ items }: { items: PlayerBrawlerRoleStat[] }) {
  if (items.every((i) => i.totalBattleCount === 0)) {
    return <Card className="p-4 text-center text-sm text-[#5C6271]">이 시즌 통계 데이터가 없습니다.</Card>;
  }
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 md:grid-cols-7">
      {items.map((it) => {
        const winRateColor = it.totalBattleCount === 0 ? "#8A90A0" : ROLE_COLOR[it.role];
        return (
          <Card key={it.role} className="p-3">
            <div className="flex items-center gap-1.5 text-xs font-medium text-[#5C6271]">
              <ClassIcon role={it.role} size={16} />
              <span>{ROLE_LABEL_KO[it.role]}</span>
            </div>
            <div
              className="mt-1 text-xl font-bold tabular-nums"
              style={{ color: winRateColor }}
            >
              {(it.winRate * 100).toFixed(1)}%
            </div>
            <div className="text-xs text-[#8A90A0] tabular-nums">{it.totalBattleCount}전</div>
          </Card>
        );
      })}
    </div>
  );
}
