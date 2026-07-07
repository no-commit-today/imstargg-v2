import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { statisticsApi } from "@/api/endpoints/statistics";
import { CACHE_SECONDS } from "@/config/cache";
import { toBrawlerWithItems } from "@/domain/brawler/mappers";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import {
  DEFAULT_DATE_RANGE,
  DEFAULT_TIER_RANGE,
  DEFAULT_TROPHY_RANGE,
} from "@/domain/statistics/enums";
import {
  toDailyResultStat,
  toEnemyResultStat,
  toItemOwnership,
  toPairResultStat,
} from "@/domain/statistics/mappers";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { BrawlerContent } from "./_components/BrawlerContent";
import { BrawlerStatsPanel } from "./_components/BrawlerStatsPanel";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";

type Params = { params: Promise<{ id: string }> };

export const revalidate = 60;

export async function generateStaticParams() {
  const list = await brawlerApi.list({ next: { revalidate: CACHE_SECONDS.SEO_REFERENCE } });
  return list.content.map((b) => ({ id: String(b.id) }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await brawlerApi.get(id, { next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE } });
    const detail = toBrawlerWithItems(dto);
    const { brawler, items } = detail;
    const role = ROLE_LABEL_KO[brawler.role];
    const rarity = RARITY_LABEL_KO[brawler.rarity];
    const hcCount = items.hyperCharge ? 1 : 0;
    return buildMetadata({
      title: `${brawler.name} | ImStarGG`,
      description: `${brawler.name} — ${role} · ${rarity} · 가젯 ${items.gadgets.length} · 스타파워 ${items.starPowers.length} · 기어 ${items.gears.length} · 하이퍼차지 ${hcCount}.`,
      path: `/brawlers/${id}`,
      image: brawler.portraitImageUrl ?? undefined,
    });
  } catch {
    return buildMetadata({
      title: "브롤러를 찾을 수 없음 | ImStarGG",
      description: "요청하신 브롤러를 찾을 수 없습니다.",
      path: `/brawlers/${id}`,
      noindex: true,
    });
  }
}

export default async function BrawlerPage({ params }: Params) {
  const { id } = await params;
  let detail;
  try {
    const dto = await brawlerApi.get(id, {
      next: { revalidate: CACHE_SECONDS.BRAWLER_REFERENCE },
    });
    detail = toBrawlerWithItems(dto);
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }

  const statsParams = {
    brawlerId: detail.brawler.id,
    tierRange: DEFAULT_TIER_RANGE,
    dateRange: DEFAULT_DATE_RANGE,
  };
  const opts = { next: { revalidate: CACHE_SECONDS.SHORT } };
  const [ownershipDto, dailyDto, pairDto, enemyDto] = await Promise.all([
    statisticsApi.itemOwnership(
      { brawlerId: detail.brawler.id, trophyRange: DEFAULT_TROPHY_RANGE },
      opts,
    ),
    statisticsApi.daily(statsParams, opts),
    statisticsApi.pairResult(statsParams, opts),
    statisticsApi.enemyResult(statsParams, opts),
  ]);

  return (
    <main className="mx-auto w-full max-w-6xl px-4 pb-10">
      <BrawlerContent
        initialDetail={detail}
        initialOwnership={toItemOwnership(ownershipDto)}
      />
      <BrawlerStatsPanel
        brawlerId={detail.brawler.id}
        initial={{
          daily: dailyDto.content.map(toDailyResultStat),
          pair: pairDto.content.map(toPairResultStat),
          enemy: enemyDto.content.map(toEnemyResultStat),
        }}
      />
      <AdUnit slot={AD_SLOTS.brawlerDetail} className="mt-6" />
    </main>
  );
}
