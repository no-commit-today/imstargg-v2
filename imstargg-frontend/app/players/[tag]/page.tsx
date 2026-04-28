import type { Metadata } from "next";
import { playerApi } from "@/api/endpoints/player";
import { renewalApi } from "@/api/endpoints/renewal";
import { toPlayer, toPlayerBrawler } from "@/domain/player/mappers";
import { emptyRenewal } from "@/domain/renewal/empty";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { normalizeTag, toUrlPath } from "@/lib/tag";
import { formatNumber } from "@/lib/format";
import { PlayerContent } from "./_components/PlayerContent";
import { PlayerNotFoundView } from "./_components/PlayerNotFoundView";

type Params = { params: Promise<{ tag: string }> };

async function loadPlayer(tag: string) {
  return playerApi.get(tag, { next: { revalidate: 60 } });
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { tag } = await params;
  try {
    const dto = await loadPlayer(tag);
    const display = `${dto.name} (${dto.tag})`;
    return buildMetadata({
      title: `${display} | ImStarGG`,
      description: `${display} — 트로피 ${formatNumber(dto.trophies)}, ${dto.club?.name ?? "클럽 없음"}. 브롤스타즈 플레이어 통계.`,
      path: `/players/${toUrlPath(tag)}`,
    });
  } catch {
    return buildMetadata({
      title: "플레이어를 찾을 수 없음 | ImStarGG",
      description: "요청하신 플레이어를 찾을 수 없습니다.",
      path: `/players/${toUrlPath(tag)}`,
      noindex: true,
    });
  }
}

export default async function PlayerPage({ params }: Params) {
  const { tag } = await params;
  let dto;
  try {
    dto = await loadPlayer(tag);
  } catch (e) {
    if (e instanceof NotFoundError) {
      return <PlayerNotFoundView tag={normalizeTag(tag)} />;
    }
    throw e;
  }
  const brawlers = await playerApi.getBrawlers(tag, { next: { revalidate: 60 } });
  const renewal = await renewalApi
    .getPlayer(tag, { next: { revalidate: 0 } })
    .catch((e: unknown) => (e instanceof NotFoundError ? emptyRenewal(tag) : null));

  return (
    <PlayerContent
      tag={normalizeTag(tag)}
      initialPlayer={toPlayer(dto)}
      initialBrawlers={brawlers.content.map(toPlayerBrawler)}
      initialRenewal={renewal}
    />
  );
}
