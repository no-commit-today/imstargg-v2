import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import { env } from "@/config/env";
import { rankingApi } from "@/api/endpoints/ranking";
import { statisticsApi } from "@/api/endpoints/statistics";
import { eventApi } from "@/api/endpoints/event";
import { CACHE_SECONDS } from "@/config/cache";
import { toPlayerRanking } from "@/domain/ranking/mappers";
import { toBrawlerResultStatistics } from "@/domain/statistics/mappers";
import { toRotationEvent } from "@/domain/event/mappers";
import { parseDateRange, parseTierRange } from "@/domain/statistics/enums";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";
import HomeScreen from "./_components/HomeScreen";
import { HomeRankingSection } from "./_components/home/HomeRankingSection";
import { HomeMetaSection } from "./_components/home/HomeMetaSection";
import { HomeEventsSection } from "./_components/home/HomeEventsSection";

export const metadata: Metadata = buildMetadata({
  title: "ImStarGG — 브롤스타즈 전적 · 통계 · 메타 검색",
  description:
    "브롤스타즈 전적 검색, 브롤러 메타 티어, 글로벌·국가별 트로피 랭킹, 이벤트 로테이션 통계까지. 한국어 브롤스타즈 통계 사이트.",
  path: "/",
});

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebSite",
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  potentialAction: {
    "@type": "SearchAction",
    target: {
      "@type": "EntryPoint",
      // routes.player encodes "#" → "%23"; SearchAction expects user to type the
      // tag body without "#", so we prepend the encoded "#" in the template.
      urlTemplate: `${env.NEXT_PUBLIC_SITE_URL}/players/%23{search_term_string}`,
    },
    "query-input": "required name=search_term_string",
  },
};

// 홈은 섹션 하나가 실패해도 나머지로 렌더링한다 — 검색(핵심 기능)이 죽으면 안 된다.
export default async function Page() {
  const [rankingDto, statsDto, rotationDto] = await Promise.all([
    rankingApi
      .players("global", { next: { revalidate: CACHE_SECONDS.SHORT } })
      .catch(() => null),
    statisticsApi
      .brawlerResult(
        { tierRange: parseTierRange(undefined), dateRange: parseDateRange(undefined) },
        { next: { revalidate: CACHE_SECONDS.SHORT } },
      )
      .catch(() => null),
    eventApi.rotation({ next: { revalidate: CACHE_SECONDS.SHORT } }).catch(() => null),
  ]);

  const rankings = rankingDto?.content.map(toPlayerRanking) ?? [];
  const stats = statsDto ? toBrawlerResultStatistics(statsDto).items : [];
  const rotation = rotationDto?.content.map(toRotationEvent) ?? [];

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <HomeScreen />
      <div className="mx-auto w-full max-w-7xl space-y-8 px-4 pb-10">
        <HomeEventsSection rotation={rotation} />
        <div className="grid gap-8 lg:grid-cols-2">
          <HomeRankingSection rankings={rankings} />
          <HomeMetaSection stats={stats} />
        </div>
        <section className="space-y-2">
          <h2 className="section-title">ImStarGG 소개</h2>
          <p className="ml-[14px] max-w-3xl text-sm leading-6 text-[#5C6271]">
            ImStarGG는 브롤스타즈 전투 데이터를 수집·집계해 플레이어 전적, 브롤러
            메타 티어, 맵·모드별 통계와 글로벌/국가별 랭킹을 제공하는 한국어 통계
            서비스입니다. 플레이어 태그로 전적을 검색하고, 갱신 버튼으로 최신 전투
            기록을 불러올 수 있어요.
          </p>
        </section>
        <AdUnit slot={AD_SLOTS.home} />
      </div>
    </>
  );
}
