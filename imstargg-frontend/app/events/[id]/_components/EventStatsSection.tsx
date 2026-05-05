import { Suspense } from "react";
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
} from "@/domain/statistics/mappers";
import {
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { parseSortOrder } from "@/domain/statistics/sort";
import type { BattleEvent } from "@/domain/event/types";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { EventResultStatsTable } from "./EventResultStatsTable";
import { EventRankStatsTable } from "./EventRankStatsTable";
import { BrawlerSelectedPanel } from "./BrawlerSelectedPanel";

type Props = {
  event: BattleEvent;
  searchParams: Record<string, string | string[] | undefined>;
};

export async function EventStatsSection({ event, searchParams }: Props) {
  const tier = parseTierRange(searchParams.tier);
  const date = parseDateRange(searchParams.date);
  const brawlerIdRaw = searchParams.brawlerId;
  const selectedBrawlerId =
    typeof brawlerIdRaw === "string" && /^\d+$/.test(brawlerIdRaw) ? Number(brawlerIdRaw) : null;

  const resultType = event.mode?.resultType ?? "RESULT";

  if (resultType === "RANK") {
    const rankSort =
      typeof searchParams.sort === "string" ? searchParams.sort : "averageRank";
    const rankOrder = parseSortOrder(searchParams.order ?? "ASC");
    const main = await statisticsApi.brawlerRank(
      { eventId: event.id, tierRange: tier, dateRange: date },
      { next: { revalidate: CACHE_SECONDS.SHORT } },
    );
    const items = sortRankItems(main.content.map(toBrawlerRankStat), rankSort, rankOrder);
    return (
      <section className="space-y-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-lg font-bold">이벤트 통계 (순위형)</h2>
            <p className="text-xs text-[#5C6271]">{TIER_RANGE_LABELS[tier]} 기준</p>
          </div>
          <StatRangeSelector tier={tier} date={date} />
        </div>
        <EventRankStatsTable
          items={items}
          sort={rankSort}
          order={rankOrder}
          selectedBrawlerId={selectedBrawlerId}
        />
        {selectedBrawlerId && (
          <Suspense fallback={<PanelFallback />}>
            <BrawlerSelectedPanel
              eventId={event.id}
              brawlerId={selectedBrawlerId}
              resultType="RANK"
              tier={tier}
              date={date}
              searchParams={searchParams}
            />
          </Suspense>
        )}
      </section>
    );
  }

  const sort = typeof searchParams.sort === "string" ? searchParams.sort : "tier";
  const order = parseSortOrder(searchParams.order);
  const main = await statisticsApi.brawlerResult(
    { eventId: event.id, tierRange: tier, dateRange: date },
    { next: { revalidate: CACHE_SECONDS.SHORT } },
  );
  const stats = toBrawlerResultStatistics(main);
  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-lg font-bold">이벤트 통계</h2>
          <p className="text-xs text-[#5C6271]">{TIER_RANGE_LABELS[tier]} 기준</p>
        </div>
        <StatRangeSelector tier={tier} date={date} />
      </div>
      <EventResultStatsTable
        items={sortResultItems(stats.items, sort, order)}
        sort={sort}
        order={order}
        selectedBrawlerId={selectedBrawlerId}
      />
      {selectedBrawlerId && (
        <Suspense fallback={<PanelFallback />}>
          <BrawlerSelectedPanel
            eventId={event.id}
            brawlerId={selectedBrawlerId}
            resultType="RESULT"
            tier={tier}
            date={date}
            searchParams={searchParams}
          />
        </Suspense>
      )}
    </section>
  );
}

function sortRankItems<
  T extends { averageRank: number; totalBattleCount: number },
>(items: T[], sort: string, order: import("@/domain/statistics/sort").SortOrder): T[] {
  const sign = order === "DESC" ? -1 : 1;
  const cmp = (a: T, b: T) => {
    switch (sort) {
      case "totalBattleCount":
        return (a.totalBattleCount - b.totalBattleCount) * sign;
      case "averageRank":
      default:
        return (a.averageRank - b.averageRank) * sign;
    }
  };
  return [...items].sort(cmp);
}

function sortResultItems<
  T extends { tierScore: number; winRate: number; pickRate: number; starPlayerRate: number; totalBattleCount: number },
>(items: T[], sort: string, order: import("@/domain/statistics/sort").SortOrder): T[] {
  const sign = order === "DESC" ? -1 : 1;
  const cmp = (a: T, b: T) => {
    switch (sort) {
      case "winRate": return (a.winRate - b.winRate) * sign;
      case "pickRate": return (a.pickRate - b.pickRate) * sign;
      case "starPlayerRate": return (a.starPlayerRate - b.starPlayerRate) * sign;
      case "totalBattleCount": return (a.totalBattleCount - b.totalBattleCount) * sign;
      case "tier":
      default: return (a.tierScore - b.tierScore) * sign;
    }
  };
  return [...items].sort(cmp);
}

function PanelFallback() {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
      불러오는 중…
    </div>
  );
}
