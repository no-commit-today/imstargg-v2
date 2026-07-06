import Link from "next/link";
import type { RotationEvent } from "@/domain/event/types";
import { RotationCard } from "@app/events/_components/RotationCard";

type Props = { rotation: RotationEvent[] };

export function HomeEventsSection({ rotation }: Props) {
  const current = rotation.slice(0, 6);
  if (current.length === 0) return null;
  return (
    <section className="space-y-3" aria-labelledby="home-events-title">
      <header className="flex items-end justify-between">
        <div>
          <h2 id="home-events-title" className="section-title">
            진행 중인 이벤트
          </h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">현재 로테이션 맵</p>
        </div>
        <Link href="/events" className="text-sm font-semibold text-[#2E7CF6]">
          더 보기
        </Link>
      </header>
      <div className="grid grid-cols-2 gap-2 sm:gap-3 lg:grid-cols-3">
        {current.map((r) => (
          <RotationCard key={r.event.id} rotation={r} />
        ))}
      </div>
    </section>
  );
}
