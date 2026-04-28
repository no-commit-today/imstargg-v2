import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { BrawlerContent } from "./_components/BrawlerContent";

type Params = { params: Promise<{ id: string }> };

export const revalidate = 86400;

export async function generateStaticParams() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  return list.content.map((b) => ({ id: String(b.id) }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
    const brawler = toBrawler(dto);
    const role = ROLE_LABEL_KO[brawler.role];
    const rarity = RARITY_LABEL_KO[brawler.rarity];
    return buildMetadata({
      title: `${brawler.name} | ImStarGG`,
      description: `${brawler.name} — ${role} · ${rarity} · 가젯 ${brawler.gadgets.length} · 스타파워 ${brawler.starPowers.length} · 기어 ${brawler.gears.length} · 하이퍼차지 ${brawler.hyperCharges.length}.`,
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

export default async function BrawlerPage({ params }: Params) {
  const { id } = await params;
  let dto;
  try {
    dto = await brawlerApi.get(id, { next: { revalidate: 86400 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  return <BrawlerContent initialBrawler={toBrawler(dto)} />;
}
