import type { Metadata } from "next";
import { eventApi } from "@/api/endpoints/event";
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import { toBattleEvent, toRotationEvent } from "@/domain/event/mappers";
import type { EventStatsSummary } from "@/domain/event/types";
import { toBrawlerResultStatistics } from "@/domain/statistics/mappers";
import { parseDateRange, parseTierRange } from "@/domain/statistics/enums";
import { buildMetadata } from "@/lib/seo";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";
import EventsListPage from "./_components/EventsListPage";

export const metadata: Metadata = buildMetadata({
  title: "이벤트 로테이션 | ImStarGG",
  description:
    "현재 진행 중인 이벤트 로테이션과 맵별 전투 통계, 경쟁전 맵 정보를 확인하세요.",
  path: "/events",
});

export default async function Page() {
  const [rotationDto, soloRankDto] = await Promise.all([
    eventApi.rotation({ cache: "no-store" }),
    eventApi.soloRank({ next: { revalidate: CACHE_SECONDS.SHORT } }),
  ]);
  const initialRotation = rotationDto.content.map(toRotationEvent);
  const initialSoloRank = soloRankDto.content.map(toBattleEvent);

  const summaries: Record<number, EventStatsSummary> = {};
  await Promise.all(
    initialRotation.map(async (r) => {
      const dto = await statisticsApi
        .brawlerResult(
          {
            tierRange: parseTierRange(undefined),
            dateRange: parseDateRange(undefined),
            eventId: r.event.id,
          },
          { next: { revalidate: CACHE_SECONDS.SHORT } },
        )
        .catch(() => null);
      if (!dto || dto.totalBattleCount === 0) return;
      const stats = toBrawlerResultStatistics(dto);
      summaries[r.event.id] = {
        totalBattleCount: stats.totalBattleCount,
        topBrawlers: stats.items
          .filter((s) => s.brawler !== null)
          .sort((a, b) => b.winRate - a.winRate)
          .slice(0, 3)
          .map((s) => ({
            brawlerId: s.brawler!.id,
            name: s.brawler!.name,
            winRate: s.winRate,
          })),
      };
    }),
  );

  return (
    <>
      <EventsListPage
        initialRotation={initialRotation}
        initialSoloRank={initialSoloRank}
        summaries={summaries}
      />
      <div className="mx-auto w-full max-w-7xl px-4 pb-10">
        <AdUnit slot={AD_SLOTS.events} />
      </div>
    </>
  );
}
