import type { Metadata } from "next";
import { playerApi } from "@/api/endpoints/player";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { PlayerSearchResults } from "@/components/shared/SearchResults";
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
    title: q ? `"${q}" 플레이어 검색 결과 | ImStarGG` : "플레이어 검색 | ImStarGG",
    description: "브롤스타즈 플레이어 이름 또는 태그로 검색합니다.",
    path: `/players/search`,
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
    ? await playerApi.search(q, 0, 20, { cache: "no-store" })
    : { content: [], hasNext: false };
  const isTagQuery = q.startsWith("#");

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">플레이어 검색</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <PlayerSearchInput defaultValue={q} />
      <div style={{ marginTop: 20 }}>
        {q && <PlayerSearchResults query={q} initial={initial} />}
      </div>
      <RenewalCard kind="player" defaultTag={isTagQuery ? q : ""} />
    </main>
  );
}
