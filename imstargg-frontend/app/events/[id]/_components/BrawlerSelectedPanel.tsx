import Link from "next/link";
import { statisticsApi } from "@/api/endpoints/statistics";
import {
  toEnemyResultStat,
  toPairRankStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import { parseResultSortBy, parseRankSortBy, parseSortOrder } from "@/domain/statistics/sort";
import type { StatsTierRange, StatsDateRange } from "@/domain/statistics/enums";
import type { BattleEventResultType } from "@/api/types";
import { PairStatsTable } from "@app/brawlers/[id]/_components/PairStatsTable";
import { EnemyStatsTable } from "@app/brawlers/[id]/_components/EnemyStatsTable";
import { PairRankTable } from "./PairRankTable";

type Props = {
  eventId: number;
  brawlerId: number;
  resultType: BattleEventResultType;
  tier: StatsTierRange;
  date: StatsDateRange;
  searchParams: Record<string, string | string[] | undefined>;
};

export async function BrawlerSelectedPanel({
  eventId,
  brawlerId,
  resultType,
  tier,
  date,
  searchParams,
}: Props) {
  const pairOrder = parseSortOrder(searchParams.pairOrder);
  const enemyOrder = parseSortOrder(searchParams.enemyOrder);

  const closeHref = (() => {
    const sp = new URLSearchParams();
    for (const [k, v] of Object.entries(searchParams)) {
      if (k === "brawlerId") continue;
      if (typeof v === "string") sp.set(k, v);
    }
    const qs = sp.toString();
    return qs ? `?${qs}` : "";
  })();

  if (resultType === "RANK") {
    const pairSort = parseRankSortBy(searchParams.pairSort);
    const pair = await statisticsApi.pairRank(
      { eventId, brawlerId, tierRange: tier, dateRange: date, sortBy: pairSort, sortOrder: pairOrder },
      { next: { revalidate: 600 } },
    );
    return (
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold">선택한 브롤러 분석</h3>
          <Link href={closeHref} scroll={false} className="text-xs font-bold text-[#5C6271] hover:text-[#14161B]">
            닫기 ✕
          </Link>
        </div>
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          <PairRankTable items={pair.content.map(toPairRankStat)} sort={pairSort} order={pairOrder} />
          <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
            적군 통계는 결과형 모드 전용입니다.
          </div>
        </div>
      </section>
    );
  }

  const pairSort = parseResultSortBy(searchParams.pairSort);
  const enemySort = parseResultSortBy(searchParams.enemySort);
  const [pair, enemy] = await Promise.all([
    statisticsApi.pairResult(
      { eventId, brawlerId, tierRange: tier, dateRange: date, sortBy: pairSort, sortOrder: pairOrder },
      { next: { revalidate: 600 } },
    ),
    statisticsApi.enemyResult(
      { eventId, brawlerId, tierRange: tier, dateRange: date, sortBy: enemySort, sortOrder: enemyOrder },
      { next: { revalidate: 600 } },
    ),
  ]);

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold">선택한 브롤러 분석</h3>
        <Link href={closeHref} scroll={false} className="text-xs font-bold text-[#5C6271] hover:text-[#14161B]">
          닫기 ✕
        </Link>
      </div>
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.content.map(toPairResultStat)} sort={pairSort} order={pairOrder} />
        <EnemyStatsTable items={enemy.content.map(toEnemyResultStat)} sort={enemySort} order={enemyOrder} />
      </div>
    </section>
  );
}
