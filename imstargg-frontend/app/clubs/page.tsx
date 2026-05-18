import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { ClubSearchInput } from "@app/clubs/_components/ClubSearchInput";
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
  const dto = await rankingApi.clubs(country, { cache: "no-store" });
  const rankings = dto.content.map(toClubRanking);

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">클럽</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <ClubSearchInput />
      <section className="mt-10 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold">
            {COUNTRY_LABELS[country]} 클럽 랭킹
          </h2>
          <CountrySelector current={country} />
        </div>
        <ClubRankingList rankings={rankings} />
      </section>
    </main>
  );
}
