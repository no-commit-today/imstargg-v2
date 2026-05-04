import type { PlayerBrawlerRoleStat } from "@/domain/statistics/player";
import { Card } from "@/components/ui/card";

const ROLE_LABEL: Record<string, string> = {
  DAMAGE_DEALER: "데미지",
  TANK: "탱커",
  MARKSMAN: "저격수",
  ASSASSIN: "암살자",
  SUPPORT: "서포터",
  CONTROLLER: "컨트롤러",
  ARTILLERY: "투척",
};

export function PlayerBrawlerRoleSummary({ items }: { items: PlayerBrawlerRoleStat[] }) {
  if (items.every((i) => i.totalBattleCount === 0)) {
    return <Card className="p-4 text-center text-sm text-[#5C6271]">이 시즌 통계 데이터가 없습니다.</Card>;
  }
  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 md:grid-cols-7">
      {items.map((it) => (
        <Card key={it.role} className="p-3">
          <div className="text-xs font-medium text-[#5C6271]">{ROLE_LABEL[it.role] ?? it.role}</div>
          <div className="mt-1 text-xl font-bold tabular-nums">{(it.winRate * 100).toFixed(1)}%</div>
          <div className="text-xs text-[#8A90A0] tabular-nums">{it.totalBattleCount}전</div>
        </Card>
      ))}
    </div>
  );
}
