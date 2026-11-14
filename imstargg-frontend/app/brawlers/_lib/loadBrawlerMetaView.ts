import { brawlerApi } from "@/api/endpoints/brawler";
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import { sortByName } from "@/domain/brawler/filters";
import { toBrawler } from "@/domain/brawler/mappers";
import type { Brawler } from "@/domain/brawler/types";
import type {
  BrawlerMetaView,
  StatsDateRange,
  StatsTierRange,
} from "@/domain/statistics/enums";
import { toBrawlerResultStatistics, toComboRankingStat } from "@/domain/statistics/mappers";
import type { BrawlerResultStatistics, ComboRankingStat } from "@/domain/statistics/types";

export type BrawlerMetaViewData =
  | { view: "brawler"; stats: BrawlerResultStatistics; brawlers: Brawler[] }
  | { view: "pair" | "trio"; items: ComboRankingStat[] };

/** 선택된 뷰의 엔드포인트만 부른다 — 탭마다 서버 렌더가 따로 돈다. */
export async function loadBrawlerMetaView(
  view: BrawlerMetaView,
  tierRange: StatsTierRange,
  dateRange: StatsDateRange,
): Promise<BrawlerMetaViewData> {
  const next = { revalidate: CACHE_SECONDS.SHORT };

  if (view === "brawler") {
    const [brawlersList, statsDto] = await Promise.all([
      brawlerApi.list({ next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE } }),
      statisticsApi.brawlerResult({ tierRange, dateRange }, { next }),
    ]);
    return {
      view,
      stats: toBrawlerResultStatistics(statsDto),
      brawlers: sortByName(brawlersList.content.map(toBrawler)),
    };
  }

  const ranking = view === "pair" ? statisticsApi.pairRanking : statisticsApi.trioRanking;
  const response = await ranking({ tierRange, dateRange }, { next });
  return { view, items: response.content.map(toComboRankingStat) };
}
