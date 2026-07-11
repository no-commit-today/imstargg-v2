import type { Metadata } from "next";
import { clubApi } from "@/api/endpoints/club";
import { ClubSearchInput } from "@app/clubs/_components/ClubSearchInput";
import { ClubSearchResults } from "@/components/shared/SearchResults";
import { RenewalCard } from "@/components/shared/RenewalCard";
import { buildMetadata } from "@/lib/seo";

type SearchParams = Promise<{ q?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const q = (await searchParams).q ?? "";
  return buildMetadata({
    title: q ? `"${q}" 클럽 검색 결과 | ImStarGG` : "클럽 검색 | ImStarGG",
    description: "브롤스타즈 클럽 이름 또는 태그로 검색합니다.",
    path: `/clubs/search`,
    noindex: true,
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const q = ((await searchParams).q ?? "").trim();
  const initial = q
    ? await clubApi.search(q, 0, 20, { cache: "no-store" })
    : { content: [], hasNext: false };
  const isTagQuery = q.startsWith("#");

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">클럽 검색</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <ClubSearchInput defaultValue={q} />
      <div style={{ marginTop: 20 }}>
        {q && <ClubSearchResults query={q} initial={initial} />}
      </div>
      <RenewalCard kind="club" defaultTag={isTagQuery ? q : ""} />
    </main>
  );
}
