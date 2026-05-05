import type { Metadata } from "next";
import { eventApi } from "@/api/endpoints/event";
import { CACHE_SECONDS } from "@/config/cache";
import { toBattleEvent, toRotationEvent } from "@/domain/event/mappers";
import { buildMetadata } from "@/lib/seo";
import EventsListPage from "./_components/EventsListPage";

export const metadata: Metadata = buildMetadata({
  title: "이벤트 로테이션 | ImStarGG",
  description:
    "현재 진행 중인 이벤트 로테이션과 경쟁전 맵 정보를 확인하세요.",
  path: "/events",
});

export default async function Page() {
  const [rotationDto, soloRankDto] = await Promise.all([
    eventApi.rotation({ cache: "no-store" }),
    eventApi.soloRank({ next: { revalidate: CACHE_SECONDS.SHORT } }),
  ]);
  const initialRotation = rotationDto.content.map(toRotationEvent);
  const initialSoloRank = soloRankDto.content.map(toBattleEvent);
  return (
    <EventsListPage
      initialRotation={initialRotation}
      initialSoloRank={initialSoloRank}
    />
  );
}
