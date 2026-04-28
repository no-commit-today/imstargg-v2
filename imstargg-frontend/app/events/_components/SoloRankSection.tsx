import type { BattleEvent } from "@/domain/event/types";
import { SoloRankCard } from "./SoloRankCard";

export function SoloRankSection({ events }: { events: BattleEvent[] }) {
  if (events.length === 0) return null;
  return (
    <section className="space-y-3">
      <header className="flex items-baseline gap-3">
        <span className="rounded bg-amber-500 px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.1em] text-white">
          RANKED
        </span>
        <h2 className="font-display text-xl font-bold tracking-[0.02em]">경쟁전 맵</h2>
        <span className="text-xs text-muted-foreground">{events.length}개</span>
      </header>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {events.map((e) => (
          <SoloRankCard key={e.id} event={e} />
        ))}
      </div>
    </section>
  );
}
