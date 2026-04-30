import type { Metadata } from "next";
import { brawlerApi } from "@/api/endpoints/brawler";
import { statisticsApi } from "@/api/endpoints/statistics";
import { toBrawler } from "@/domain/brawler/mappers";
import { toBrawlerResultStatistics } from "@/domain/statistics/mappers";
import {
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { parseSortOrder } from "@/domain/statistics/sort";
import { sortByName } from "@/domain/brawler/filters";
import { buildMetadata } from "@/lib/seo";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { BrawlerStatTable } from "./_components/BrawlerStatTable";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 메타 | ImStarGG",
  description: "브롤스타즈 브롤러 메타 — 티어, 승률, 픽률을 한눈에.",
  path: "/brawlers",
});

export const revalidate = 600;

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default async function Page({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const tierRange = parseTierRange(sp.tier);
  const dateRange = parseDateRange(sp.date);
  const sort = typeof sp.sort === "string" ? sp.sort : "tier";
  const order = parseSortOrder(sp.order);

  const [brawlersList, statsDto] = await Promise.all([
    brawlerApi.list({ next: { revalidate: 86400 } }),
    statisticsApi.brawlerResult(
      { tierRange, dateRange },
      { next: { revalidate: 600 } },
    ),
  ]);

  const stats = toBrawlerResultStatistics(statsDto);
  const brawlers = sortByName(brawlersList.content.map(toBrawler));
  const statById = new Map(stats.items.filter((s) => s.brawler).map((s) => [s.brawler!.id, s]));
  const rows = brawlers.map((b) => ({ brawler: b, stat: statById.get(b.id) ?? null }));

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div className="text-sm text-[#5C6271]">
          <span className="font-bold text-[#14161B]">{TIER_RANGE_LABELS[tierRange]}</span> 메타
        </div>
        <StatRangeSelector tier={tierRange} date={dateRange} />
      </div>
      <BrawlerStatTable
        rows={rows}
        sort={sort}
        order={order}
        totalBattleCount={stats.totalBattleCount}
      />
    </main>
  );
}
