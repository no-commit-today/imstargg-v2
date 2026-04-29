"use client";

import type { BattleEvent } from "@/domain/event/types";
import { useEvent } from "@/hooks/queries/useEvent";
import { EventHero } from "./EventHero";

export function EventContent({ initialEvent }: { initialEvent: BattleEvent }) {
  const { data: event = initialEvent } = useEvent(initialEvent.id, {
    initialData: initialEvent,
  });
  return (
    <section className="py-6 space-y-5">
      <EventHero event={event} />
    </section>
  );
}
