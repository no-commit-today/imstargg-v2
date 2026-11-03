import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
} from "@/domain/statistics/enums";
import {
  toBrawlerRankStat,
  toBrawlerResultStatistics,
} from "@/domain/statistics/mappers";
import type { BattleEvent } from "@/domain/event/types";
import { EventStatsPanel } from "./EventStatsPanel";

export async function EventStatsSection({ event }: { event: BattleEvent }) {
  // null(mode 없음 포함) = 통계를 모으지 않는 모드. 통계를 요청하지 않고 안내만 보인다.
  const resultType = event.mode?.resultType ?? null;
  if (resultType === null) {
    return (
      <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
        이 모드는 통계를 제공하지 않습니다
      </div>
    );
  }

  const base = {
    eventId: event.id,
    tierRange: DEFAULT_TIER_RANGE,
    dateRange: DEFAULT_DATE_RANGE,
  };
  const opts = { next: { revalidate: CACHE_SECONDS.SHORT } };

  if (resultType === "RANK") {
    const main = await statisticsApi.brawlerRank(base, opts);
    return (
      <EventStatsPanel
        eventId={event.id}
        resultType="RANK"
        initialResult={null}
        initialRank={main.content.map(toBrawlerRankStat)}
      />
    );
  }

  const main = await statisticsApi.brawlerResult(base, opts);
  return (
    <EventStatsPanel
      eventId={event.id}
      resultType="RESULT"
      initialResult={toBrawlerResultStatistics(main)}
      initialRank={null}
    />
  );
}
