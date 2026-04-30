import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { rankingApi } from "@/api/endpoints/ranking";
import { toBrawler } from "@/domain/brawler/mappers";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toBrawlerRanking } from "@/domain/ranking/mappers";
import { ApiError } from "@/api/errors";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@app/ranking/_components/CountrySelector";
import { BrawlerRankingList } from "@app/ranking/_components/BrawlerRankingList";

export const revalidate = 600;

type Params = Promise<{ brawlerId: string }>;
type SearchParams = Promise<{ country?: string }>;

async function loadBrawler(brawlerId: string) {
  const idNum = Number(brawlerId);
  if (!Number.isFinite(idNum) || idNum <= 0) notFound();
  try {
    const dto = await brawlerApi.get(idNum, {
      next: { revalidate: 86400 },
    });
    return toBrawler(dto);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
}

export async function generateMetadata({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: SearchParams;
}): Promise<Metadata> {
  const { brawlerId } = await params;
  const country = parseCountry((await searchParams).country);
  const brawler = await loadBrawler(brawlerId);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${brawler.name} ${label} 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${brawler.name}의 ${label} 트로피 랭킹.`,
    path: `/ranking/brawlers/${brawler.id}`,
  });
}

export default async function Page({
  params,
  searchParams,
}: {
  params: Params;
  searchParams: SearchParams;
}) {
  const { brawlerId } = await params;
  const country = parseCountry((await searchParams).country);
  const brawler = await loadBrawler(brawlerId);

  const dto = await rankingApi.brawlers(brawler.id, country, {
    next: { revalidate: 600 },
  });
  const rankings = dto.content.map(toBrawlerRanking);

  return (
    <main className="container mx-auto max-w-7xl space-y-4 px-4 py-8">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <BrawlerAvatar
            name={brawler.name}
            imageUrl={brawler.portraitImageUrl}
            rarity={brawler.rarity}
            size={56}
            showFrame
          />
          <h1 className="text-2xl font-bold">
            {brawler.name} {COUNTRY_LABELS[country]} 랭킹
          </h1>
        </div>
        <CountrySelector current={country} />
      </div>
      <BrawlerRankingList rankings={rankings} />
    </main>
  );
}
