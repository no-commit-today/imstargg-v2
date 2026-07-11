import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { CACHE_SECONDS } from "@/config/cache";
import { ClubSearchInput } from "@app/clubs/_components/ClubSearchInput";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toClubRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelect } from "@/components/ranking/CountrySelect";
import { ClubRankingList } from "@/components/ranking/ClubRankingList";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";

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
    description: `브롤스타즈 ${label} 클럽 트로피 랭킹과 클럽 검색.`,
    path: "/clubs",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.clubs(country, {
    next: { revalidate: CACHE_SECONDS.SHORT },
  });
  const rankings = dto.content.map(toClubRanking);

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-5">
      <header className="space-y-1">
        <h1 className="font-display text-3xl tracking-[0.02em]">클럽</h1>
        <p className="text-sm text-[#5C6271]">이름 또는 #태그로 검색하세요.</p>
      </header>
      <ClubSearchInput />
      <section className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="section-title">
              {COUNTRY_LABELS[country]} 랭킹
            </h2>
            <p className="ml-[14px] text-xs text-[#5C6271]">
              트로피 기준 상위 클럽
            </p>
          </div>
          <CountrySelect current={country} />
        </div>
        <ClubRankingList rankings={rankings} />
      </section>
      <AdUnit slot={AD_SLOTS.clubRanking} />
    </main>
  );
}
