import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawlerDetail } from "@/domain/brawler/mappers";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { BrawlerContent } from "./_components/BrawlerContent";
import { BrawlerStatsSection } from "./_components/BrawlerStatsSection";

type Params = { params: Promise<{ id: string }> };
type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export const revalidate = 600;

export async function generateStaticParams() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  return list.content.map((b) => ({ id: String(b.id) }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
    const detail = toBrawlerDetail(dto);
    const { brawler, items } = detail;
    const role = ROLE_LABEL_KO[brawler.role];
    const rarity = RARITY_LABEL_KO[brawler.rarity];
    const hcCount = items.hyperCharge ? 1 : 0;
    return buildMetadata({
      title: `${brawler.name} | ImStarGG`,
      description: `${brawler.name} — ${role} · ${rarity} · 가젯 ${items.gadgets.length} · 스타파워 ${items.starPowers.length} · 기어 ${items.gears.length} · 하이퍼차지 ${hcCount}.`,
      path: `/brawlers/${id}`,
      image: brawler.portraitImageUrl ?? undefined,
    });
  } catch {
    return buildMetadata({
      title: "브롤러를 찾을 수 없음 | ImStarGG",
      description: "요청하신 브롤러를 찾을 수 없습니다.",
      path: `/brawlers/${id}`,
      noindex: true,
    });
  }
}

export default async function BrawlerPage({
  params,
  searchParams,
}: {
  params: Params["params"];
  searchParams: SearchParams;
}) {
  const { id } = await params;
  const sp = await searchParams;
  let detail;
  try {
    const dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
    detail = toBrawlerDetail(dto);
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  return (
    <main className="mx-auto w-full max-w-6xl px-4 pb-10">
      <BrawlerContent initialDetail={detail} />
      <BrawlerStatsSection brawler={detail.brawler} items={detail.items} searchParams={sp} />
    </main>
  );
}
