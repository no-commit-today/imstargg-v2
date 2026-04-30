import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { eventApi } from "@/api/endpoints/event";
import { toBattleEvent } from "@/domain/event/mappers";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { EventContent } from "./_components/EventContent";
import { EventStatsSection } from "./_components/EventStatsSection";

type Params = { params: Promise<{ id: string }> };

export const revalidate = 3600;

export async function generateStaticParams() {
  const [rotation, soloRank] = await Promise.all([
    eventApi.rotation({ next: { revalidate: 3600 } }),
    eventApi.soloRank({ next: { revalidate: 3600 } }),
  ]);
  const ids = new Set<string>();
  for (const r of rotation.content) ids.add(String(r.event.id));
  for (const e of soloRank.content) ids.add(String(e.id));
  return [...ids].map((id) => ({ id }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { id } = await params;
  try {
    const dto = await eventApi.get(id, { next: { revalidate: 3600 } });
    const event = toBattleEvent(dto);
    const modeName = event.mode?.name ?? "이벤트";
    const resultLabel = event.mode?.resultType === "RANK" ? "순위형" : "결과형";
    return buildMetadata({
      title: `${event.mapName} (${modeName}) | ImStarGG`,
      description: `모드: ${modeName} · 결과 타입: ${resultLabel}`,
      path: `/events/${id}`,
      image: event.mode?.iconImageUrl ?? undefined,
    });
  } catch {
    return buildMetadata({
      title: "이벤트를 찾을 수 없음 | ImStarGG",
      description: "요청하신 이벤트를 찾을 수 없습니다.",
      path: `/events/${id}`,
      noindex: true,
    });
  }
}

export default async function EventPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { id } = await params;
  const sp = await searchParams;
  let dto;
  try {
    dto = await eventApi.get(id, { next: { revalidate: 3600 } });
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  const event = toBattleEvent(dto);
  return (
    <main className="mx-auto w-full max-w-7xl px-4 pb-10">
      <EventContent initialEvent={event} />
      <EventStatsSection event={event} searchParams={sp} />
    </main>
  );
}
