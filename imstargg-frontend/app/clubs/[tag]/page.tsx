import type { Metadata } from "next";
import { clubApi } from "@/api/endpoints/club";
import { renewalApi } from "@/api/endpoints/renewal";
import { toClub } from "@/domain/club/mappers";
import { emptyRenewal } from "@/domain/renewal/empty";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { normalizeTag, toUrlPath } from "@/lib/tag";
import { formatNumber } from "@/lib/format";
import { CACHE_SECONDS } from "@/config/cache";
import { NotFoundView } from "@/components/shared/NotFoundView";
import { ClubContent } from "./_components/ClubContent";

type Params = { params: Promise<{ tag: string }> };

async function loadClub(tag: string) {
  return clubApi.get(tag, { next: { revalidate: CACHE_SECONDS.SHORT } });
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { tag } = await params;
  try {
    const dto = await loadClub(tag);
    const display = `${dto.name} (${dto.tag})`;
    return buildMetadata({
      title: `${display} | ImStarGG`,
      description: `${display} — 트로피 ${formatNumber(dto.trophies)}, 멤버 ${dto.members.length}/30. 브롤스타즈 클럽 통계.`,
      path: `/clubs/${toUrlPath(tag)}`,
      dynamicOg: true,
    });
  } catch {
    return buildMetadata({
      title: "클럽을 찾을 수 없음 | ImStarGG",
      description: "요청하신 클럽을 찾을 수 없습니다.",
      path: `/clubs/${toUrlPath(tag)}`,
      noindex: true,
    });
  }
}

export default async function ClubPage({ params }: Params) {
  const { tag } = await params;
  const [clubResult, renewalResult] = await Promise.allSettled([
    loadClub(tag),
    renewalApi.getClub(tag, { next: { revalidate: 0 } }),
  ]);

  if (clubResult.status === "rejected") {
    if (clubResult.reason instanceof NotFoundError) {
      return <NotFoundView kind="club" tag={normalizeTag(tag)} />;
    }
    throw clubResult.reason;
  }

  const renewal =
    renewalResult.status === "fulfilled"
      ? renewalResult.value
      : renewalResult.reason instanceof NotFoundError
        ? emptyRenewal(tag)
        : null;

  return (
    <ClubContent
      tag={normalizeTag(tag)}
      initialClub={toClub(clubResult.value)}
      initialRenewal={renewal}
    />
  );
}
