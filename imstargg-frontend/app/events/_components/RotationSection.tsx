import type { RotationEvent } from "@/domain/event/types";
import { RotationCard } from "./RotationCard";

export function RotationSection({ rotation }: { rotation: RotationEvent[] }) {
  if (rotation.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-[#5C6271]">
        진행 중인 이벤트가 없어요.
      </p>
    );
  }
  return (
    <section className="space-y-4">
      <header className="flex items-end justify-between gap-3">
        <div>
          <h2 className="section-title">현재 진행 중</h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            지금 플레이할 수 있는 모드와 맵
          </p>
        </div>
        <span className="text-[11px] font-bold uppercase tracking-[0.1em] text-[#8A90A0]">
          {rotation.length}개 진행 중
        </span>
      </header>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {rotation.map((r) => (
          <RotationCard key={r.event.id} rotation={r} />
        ))}
      </div>
    </section>
  );
}
