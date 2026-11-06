import type { Metadata } from "next";
import {
  parseBrawlerMetaView,
  parseDateRange,
  parseTierRange,
  TIER_RANGE_LABELS,
} from "@/domain/statistics/enums";
import { buildMetadata } from "@/lib/seo";
import { StatRangeSelector } from "@app/_components/statistics/StatRangeSelector";
import { BrawlerMetaViewTabs } from "./_components/BrawlerMetaViewTabs";
import { BrawlerStatTable } from "./_components/BrawlerStatTable";
import { ComboRankingTable } from "./_components/ComboRankingTable";
import { loadBrawlerMetaView, type BrawlerMetaViewData } from "./_lib/loadBrawlerMetaView";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 메타 | ImStarGG",
  description: "브롤스타즈 브롤러 메타 — 티어, 승률, 픽률을 한눈에.",
  path: "/brawlers",
});

type BrawlerViewData = Extract<BrawlerMetaViewData, { view: "brawler" }>;

function toBrawlerRows({ brawlers, stats }: BrawlerViewData) {
  const statById = new Map(stats.items.filter((s) => s.brawler).map((s) => [s.brawler!.id, s]));
  return brawlers.map((b) => ({ brawler: b, stat: statById.get(b.id) ?? null }));
}

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default async function Page({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const tierRange = parseTierRange(sp.tier);
  const dateRange = parseDateRange(sp.date);
  const view = parseBrawlerMetaView(sp.view);

  const data = await loadBrawlerMetaView(view, tierRange, dateRange);

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-5">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="font-display text-3xl tracking-[0.02em]">브롤러 메타</h1>
          <p className="mt-1 text-sm text-[#5C6271]">
            <span className="font-bold text-[#14161B]">{TIER_RANGE_LABELS[tierRange]}</span> 메타
          </p>
        </div>
        <StatRangeSelector tier={tierRange} date={dateRange} />
      </header>
      <BrawlerMetaViewTabs view={view} />
      {data.view === "brawler" ? (
        <BrawlerStatTable
          rows={toBrawlerRows(data)}
          totalBattleCount={data.stats.totalBattleCount}
        />
      ) : (
        <ComboRankingTable items={data.items} />
      )}
      <AdUnit slot={AD_SLOTS.brawlerMeta} className="mt-5" />
    </main>
  );
}
