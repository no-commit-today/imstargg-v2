"use client";

import type {
  BattleEvent,
  EventStatsSummary,
  RotationEvent,
} from "@/domain/event/types";
import { useRotationEvents } from "@/hooks/queries/useRotationEvents";
import { useSoloRankEvents } from "@/hooks/queries/useSoloRankEvents";
import { EventSearch } from "./EventSearch";
import { RotationSection } from "./RotationSection";
import { SoloRankSection } from "./SoloRankSection";

interface Props {
  initialRotation: RotationEvent[];
  initialSoloRank: BattleEvent[];
  summaries?: Record<number, EventStatsSummary>;
}

export default function EventsListPage({
  initialRotation,
  initialSoloRank,
  summaries,
}: Props) {
  const { data: rotation = initialRotation } = useRotationEvents({
    initialData: initialRotation,
  });
  const { data: soloRank = initialSoloRank } = useSoloRankEvents({
    initialData: initialSoloRank,
  });

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-6">
      <header className="space-y-1">
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#8A90A0]">
          EVENTS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">이벤트 로테이션</h1>
        <p className="text-sm text-[#5C6271]">
          현재 진행 중인 모드와 경쟁전 맵을 확인하세요.
        </p>
      </header>

      <EventSearch>
        <div className="space-y-8">
          <RotationSection rotation={rotation} summaries={summaries} />
          <SoloRankSection events={soloRank} />
        </div>
      </EventSearch>
    </main>
  );
}
