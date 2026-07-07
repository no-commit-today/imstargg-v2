import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toPlayerRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelect } from "@/components/ranking/CountrySelect";
import { PlayerRankingList } from "@/components/ranking/PlayerRankingList";
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
    title: `${label} 플레이어 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 플레이어 트로피 랭킹을 확인하세요.`,
    path: "/players",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.players(country, { cache: "no-store" });
  const rankings = dto.content.map(toPlayerRanking);

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-5">
      <header className="space-y-1">
        <h1 className="font-display text-3xl tracking-[0.02em]">플레이어</h1>
        <p className="text-sm text-[#5C6271]">이름 또는 #태그로 검색하세요.</p>
      </header>
      <PlayerSearchInput />
      <section className="space-y-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="section-title">
              {COUNTRY_LABELS[country]} 랭킹
            </h2>
            <p className="ml-[14px] text-xs text-[#5C6271]">
              트로피 기준 상위 플레이어
            </p>
          </div>
          <CountrySelect current={country} />
        </div>
        <PlayerRankingList rankings={rankings} />
      </section>
      <AdUnit slot={AD_SLOTS.playerRanking} />
    </main>
  );
}
