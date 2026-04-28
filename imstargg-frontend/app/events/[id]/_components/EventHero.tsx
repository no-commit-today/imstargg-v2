import Image from "next/image";
import { Badge } from "@/components/ui/badge";
import type { BattleEvent } from "@/domain/event/types";

const RESULT_TYPE_LABEL: Record<"RESULT" | "RANK", string> = {
  RESULT: "결과형 (승/패)",
  RANK: "순위형",
};

export function EventHero({ event }: { event: BattleEvent }) {
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);
  return (
    <section className="flex flex-col items-start gap-4 sm:flex-row sm:items-center">
      {event.mode?.iconImageUrl ? (
        <Image
          src={event.mode.iconImageUrl}
          alt={modeName}
          width={96}
          height={96}
          className="rounded-lg"
          unoptimized
        />
      ) : (
        <div
          aria-hidden
          className="flex h-24 w-24 items-center justify-center rounded-lg bg-gradient-to-br from-neutral-700 to-neutral-900 font-display text-4xl font-bold text-white"
        >
          {initial}
        </div>
      )}
      <div className="min-w-0 flex-1">
        <div className="text-xs font-bold uppercase tracking-[0.1em] text-muted-foreground">
          {modeName}
        </div>
        <h1 className="mt-1 font-display text-3xl font-bold tracking-[0.02em]">
          {event.mapName}
        </h1>
        {event.mode && (
          <div className="mt-2 flex flex-wrap gap-2">
            <Badge variant="outline">{RESULT_TYPE_LABEL[event.mode.resultType]}</Badge>
          </div>
        )}
      </div>
    </section>
  );
}
