import type { Metadata } from "next";
import { playerApi } from "@/api/endpoints/player";
import { renewalApi } from "@/api/endpoints/renewal";
import { battleApi } from "@/api/endpoints/battle";
import { BATTLES_PAGE_SIZE } from "@/hooks/queries/usePlayerBattles";
import { CACHE_SECONDS } from "@/config/cache";
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
  return playerApi.get(tag, { next: { revalidate: CACHE_SECONDS.SHORT } });
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
      dynamicOg: true,
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

  // 4개 요청을 동시에 시작한다. allSettled 이므로 개별 실패가
  // unhandled rejection 이 되지 않고, 아래에서 의미론을 명시적으로 복원한다.
  const [playerResult, brawlersResult, renewalResult, battlesResult] =
    await Promise.allSettled([
      loadPlayer(tag),
      playerApi.getBrawlers(tag, { next: { revalidate: CACHE_SECONDS.SHORT } }),
      renewalApi.getPlayer(tag, { next: { revalidate: 0 } }),
      battleApi.getPlayerBattles(
        tag,
        { offset: 0, limit: BATTLES_PAGE_SIZE },
        { next: { revalidate: CACHE_SECONDS.SHORT } },
      ),
    ]);

  // player 404 → 갱신 유도 화면 (기존 동작)
  if (playerResult.status === "rejected") {
    if (playerResult.reason instanceof NotFoundError) {
      return <PlayerNotFoundView tag={normalizeTag(tag)} />;
    }
    throw playerResult.reason;
  }
  // player 정상인데 brawlers 실패 → 기존처럼 error.tsx 로
  if (brawlersResult.status === "rejected") throw brawlersResult.reason;

  const renewal =
    renewalResult.status === "fulfilled"
      ? renewalResult.value
      : renewalResult.reason instanceof NotFoundError
        ? emptyRenewal(tag)
        : null;

  // 배틀 프리페치 실패는 페이지 실패로 승격하지 않는다 — 클라이언트 훅이 자체 페치
  const initialBattles =
    battlesResult.status === "fulfilled" ? battlesResult.value : undefined;

  return (
    <PlayerContent
      tag={normalizeTag(tag)}
      initialPlayer={toPlayer(playerResult.value)}
      initialBrawlers={brawlersResult.value.content.map(toPlayerBrawler)}
      initialRenewal={renewal}
      initialBattles={initialBattles}
      battlesFetchedAt={initialBattles ? Date.now() : undefined}
    />
  );
}
