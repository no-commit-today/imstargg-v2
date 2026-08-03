import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import { env } from "@/config/env";
import HomeScreen from "./_components/HomeScreen";

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

export default function Page() {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <HomeScreen />
    </>
  );
}
