import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toPlayerRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@app/ranking/_components/CountrySelector";
import { PlayerRankingList } from "@app/ranking/_components/PlayerRankingList";

export const revalidate = 600;

type SearchParams = Promise<{ country?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const country = parseCountry((await searchParams).country);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${label} 플레이어 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 플레이어 트로피 랭킹.`,
    path: "/ranking/players",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.players(country, {
    next: { revalidate: 600 },
  });
  const rankings = dto.content.map(toPlayerRanking);

  return (
    <main className="container mx-auto max-w-3xl space-y-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">
          {COUNTRY_LABELS[country]} 플레이어 랭킹
        </h1>
        <CountrySelector current={country} />
      </div>
      <PlayerRankingList rankings={rankings} />
    </main>
  );
}
