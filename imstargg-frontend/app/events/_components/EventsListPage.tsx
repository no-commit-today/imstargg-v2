"use client";

import type { BattleEvent, RotationEvent } from "@/domain/event/types";
import { useRotationEvents } from "@/hooks/queries/useRotationEvents";
import { useSoloRankEvents } from "@/hooks/queries/useSoloRankEvents";
import { EventSearch } from "./EventSearch";
import { RotationSection } from "./RotationSection";
import { SoloRankSection } from "./SoloRankSection";

interface Props {
  initialRotation: RotationEvent[];
  initialSoloRank: BattleEvent[];
}

export default function EventsListPage({ initialRotation, initialSoloRank }: Props) {
  const { data: rotation = initialRotation } = useRotationEvents({
    initialData: initialRotation,
  });
  const { data: soloRank = initialSoloRank } = useSoloRankEvents({
    initialData: initialSoloRank,
  });

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6 space-y-6">
      <header>
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted-foreground">
          EVENTS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">이벤트 로테이션</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          현재 진행 중인 모드와 경쟁전 맵을 확인하세요.
        </p>
      </header>

      <EventSearch>
        <div className="space-y-6">
          <RotationSection rotation={rotation} />
          <SoloRankSection events={soloRank} />
        </div>
      </EventSearch>
    </main>
  );
}
