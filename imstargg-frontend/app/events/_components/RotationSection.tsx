import type { RotationEvent } from "@/domain/event/types";
import { RotationCard } from "./RotationCard";

export function RotationSection({ rotation }: { rotation: RotationEvent[] }) {
  if (rotation.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        진행 중인 이벤트가 없어요.
      </p>
    );
  }
  return (
    <section className="space-y-3">
      <header className="flex items-baseline gap-3">
        <span className="rounded bg-danger px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-[0.1em] text-white">
          LIVE
        </span>
        <h2 className="font-display text-xl font-bold tracking-[0.02em]">
          현재 진행 중
        </h2>
        <span className="text-xs text-muted-foreground">{rotation.length}개</span>
      </header>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {rotation.map((r) => (
          <RotationCard key={r.event.id} rotation={r} />
        ))}
      </div>
    </section>
  );
}
