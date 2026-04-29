import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import { env } from "@/config/env";
import HomeScreen from "./_components/HomeScreen";

export const metadata: Metadata = buildMetadata({
  title: "ImStarGG — 브롤스타즈 전적 · 통계 · 메타 검색",
  description:
    "플레이어 태그로 전적을 찾고, 브롤러 메타와 이벤트 정보를 한눈에. 한국어 브롤스타즈 통계 사이트.",
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
      urlTemplate: `${env.NEXT_PUBLIC_SITE_URL}/players/{search_term_string}`,
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
