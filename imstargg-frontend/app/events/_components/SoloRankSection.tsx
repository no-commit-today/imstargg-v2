import type { BattleEvent } from "@/domain/event/types";
import { SoloRankCard } from "./SoloRankCard";

export function SoloRankSection({ events }: { events: BattleEvent[] }) {
  if (events.length === 0) return null;
  return (
    <section className="space-y-4">
      <header className="flex items-end justify-between gap-3">
        <div>
          <h2 className="section-title">경쟁전 맵</h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            솔로 경쟁전 시즌 맵 풀
          </p>
        </div>
        <span className="text-[11px] font-bold uppercase tracking-[0.1em] text-[#8A90A0]">
          {events.length}개 맵
        </span>
      </header>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {events.map((e) => (
          <SoloRankCard key={e.id} event={e} />
        ))}
      </div>
    </section>
  );
}
