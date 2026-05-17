import Link from "next/link";
import type { EnemyResultStat } from "@/domain/statistics/types";
import type { SortOrder, ResultStatsSortBy } from "@/domain/statistics/sort";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { SortableHeader } from "@app/_components/statistics/SortableHeader";
import {
  CountCell,
  PercentCell,
} from "@app/_components/statistics/StatNumberCell";

type Props = {
  items: EnemyResultStat[];
  sort: ResultStatsSortBy;
  order: SortOrder;
};

/**
 * 카운터 표는 행의 의미가 반전된다.
 * - 적의 승률 ≥55%: 나에게 "위협(loss)" — 빨간 rail
 * - 적의 승률 ≤45%: 나에게 "좋음(win)"  — 초록 rail
 * - 그 외: neutral
 */
function enemyRail(enemyWinRate: number): "win" | "neutral" | "loss" {
  if (enemyWinRate >= 0.55) return "loss";
  if (enemyWinRate <= 0.45) return "win";
  return "neutral";
}

function enemyTone(enemyWinRate: number): "win" | "neutral" | "loss" {
  return enemyRail(enemyWinRate);
}

export function EnemyStatsTable({ items, sort, order }: Props) {
  return (
    <div className="data-table">
      <div className="data-table-toolbar">
        <h3 className="text-sm font-bold flex items-center gap-2">
          <span aria-hidden>⚔️</span> 카운터
        </h3>
        <span className="text-[11px] text-[#5C6271]">적 픽일 때의 적 승률</span>
      </div>
      <div className="data-table-head grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2">
        <span>순위</span>
        <span>카운터 브롤러</span>
        <SortableHeader column="PICK_RATE" label="픽률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <SortableHeader column="WIN_RATE" label="승률" currentSort={sort} currentOrder={order} paramPrefix="enemy" />
        <span>전투수</span>
      </div>
      {items.length === 0 ? (
        <div className="px-4 py-8 text-center text-sm text-[#5C6271]">데이터가 부족합니다.</div>
      ) : (
        items.map((it, i) =>
          it.enemyBrawler ? (
            <Link
              key={`${it.enemyBrawler.id}-${i}`}
              href={`/brawlers/${it.enemyBrawler.id}`}
              data-rail={enemyRail(it.winRate)}
              className="data-row grid grid-cols-[28px_minmax(0,1fr)_44px_56px_56px] items-center gap-1 text-[13px] sm:grid-cols-[48px_minmax(0,1fr)_72px_88px_96px] sm:gap-2 sm:text-sm"
            >
              <span className="font-bold text-[#5C6271] tabular-nums">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-1 overflow-hidden sm:gap-2">
                <BrawlerAvatar
                  name={it.enemyBrawler.name}
                  imageUrl={it.enemyBrawler.portraitImageUrl}
                  rarity={it.enemyBrawler.rarity}
                  size={32}
                  className="shrink-0"
                />
                <span className="truncate font-bold">{it.enemyBrawler.name}</span>
              </div>
              <PercentCell value={it.pickRate} />
              <PercentCell value={it.winRate} gauge gaugeTone={enemyTone(it.winRate)} />
              <CountCell value={it.totalBattleCount} />
            </Link>
          ) : null,
        )
      )}
    </div>
  );
}
