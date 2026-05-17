import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import {
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { parseResultSortBy, parseSortOrder } from "@/domain/statistics/sort";
import {
  toDailyResultStat,
  toEnemyResultStat,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import type { Brawler } from "@/domain/brawler/types";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { DailyTrendChart } from "./DailyTrendChart";
import { PairStatsTable } from "./PairStatsTable";
import { EnemyStatsTable } from "./EnemyStatsTable";

type Props = {
  brawler: Brawler;
  searchParams: Record<string, string | string[] | undefined>;
};

export async function BrawlerStatsSection({ brawler, searchParams }: Props) {
  const tier = parseTierRange(searchParams.tier);
  const date = parseDateRange(searchParams.date);
  const pairSort = parseResultSortBy(searchParams.pairSort);
  const pairOrder = parseSortOrder(searchParams.pairOrder);
  const enemySort = parseResultSortBy(searchParams.enemySort);
  const enemyOrder = parseSortOrder(searchParams.enemyOrder);

  const [daily, pair, enemy] = await Promise.all([
    statisticsApi.daily(
      { brawlerId: brawler.id, tierRange: tier, dateRange: date },
      { next: { revalidate: CACHE_SECONDS.SHORT } },
    ),
    statisticsApi.pairResult(
      {
        brawlerId: brawler.id,
        tierRange: tier,
        dateRange: date,
        sortBy: pairSort,
        sortOrder: pairOrder,
      },
      { next: { revalidate: CACHE_SECONDS.SHORT } },
    ),
    statisticsApi.enemyResult(
      {
        brawlerId: brawler.id,
        tierRange: tier,
        dateRange: date,
        sortBy: enemySort,
        sortOrder: enemyOrder,
      },
      { next: { revalidate: CACHE_SECONDS.SHORT } },
    ),
  ]);

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="section-title">통계</h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            {TIER_RANGE_LABELS[tier]} 기준
          </p>
        </div>
        <StatRangeSelector tier={tier} date={date} />
      </div>
      <DailyTrendChart items={daily.content.map(toDailyResultStat)} />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <PairStatsTable items={pair.content.map(toPairResultStat)} sort={pairSort} order={pairOrder} />
        <EnemyStatsTable items={enemy.content.map(toEnemyResultStat)} sort={enemySort} order={enemyOrder} />
      </div>
    </section>
  );
}
