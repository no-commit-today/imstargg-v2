import type { Metadata } from "next";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { sortByName } from "@/domain/brawler/filters";
import { buildMetadata } from "@/lib/seo";
import BrawlersListPage from "./_components/BrawlersListPage";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 도감 | ImStarGG",
  description:
    "브롤스타즈 브롤러의 정보, 가젯, 스타파워, 기어, 하이퍼차지를 확인하세요.",
  path: "/brawlers",
});

export const revalidate = 86400;

export default async function Page() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  const brawlers = sortByName(list.content.map(toBrawler));
  return <BrawlersListPage initialBrawlers={brawlers} />;
}
