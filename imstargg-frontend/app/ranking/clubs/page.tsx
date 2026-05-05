import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toClubRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@app/ranking/_components/CountrySelector";
import { ClubRankingList } from "@app/ranking/_components/ClubRankingList";

type SearchParams = Promise<{ country?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const country = parseCountry((await searchParams).country);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${label} 클럽 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 클럽 트로피 랭킹.`,
    path: "/ranking/clubs",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.clubs(country, { cache: "no-store" });
  const rankings = dto.content.map(toClubRanking);

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">
          {COUNTRY_LABELS[country]} 클럽 랭킹
        </h1>
        <CountrySelector current={country} />
      </div>
      <ClubRankingList rankings={rankings} />
    </>
  );
}
