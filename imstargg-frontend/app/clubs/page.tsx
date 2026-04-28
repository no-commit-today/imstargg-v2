import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import ClubSearchPage from "./_components/ClubSearchPage";

export const metadata: Metadata = buildMetadata({
  title: "클럽 검색 | ImStarGG",
  description: "브롤스타즈 클럽을 태그로 검색하고 멤버·트로피 정보를 확인하세요.",
  path: "/clubs",
});

export default function Page() {
  return <ClubSearchPage />;
}
