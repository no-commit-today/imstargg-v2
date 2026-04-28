"use client";

import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import type { RotationEvent } from "@/domain/event/types";
import { EventCountdown } from "./EventCountdown";

export function RotationCard({ rotation }: { rotation: RotationEvent }) {
  const { event, endTime } = rotation;
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);

  return (
    <Link href={`/events/${event.id}`} className="block">
      <Card className="flex h-full flex-col overflow-hidden transition hover:shadow-md">
        <div className="flex items-center gap-3 p-4">
          {event.mode?.iconImageUrl ? (
            <Image
              src={event.mode.iconImageUrl}
              alt={modeName}
              width={48}
              height={48}
              className="rounded-md"
              unoptimized
            />
          ) : (
            <div
              aria-hidden
              className="flex h-12 w-12 items-center justify-center rounded-md bg-gradient-to-br from-neutral-700 to-neutral-900 font-display text-2xl font-bold text-white"
            >
              {initial}
            </div>
          )}
          <div className="min-w-0 flex-1">
            <div className="text-[11px] font-bold uppercase tracking-[0.08em] text-muted-foreground">
              {modeName}
            </div>
            <div className="truncate text-base font-bold">{event.mapName}</div>
          </div>
        </div>
        <div className="flex items-center justify-between border-t bg-muted/40 px-4 py-2">
          <span className="text-[10px] font-bold uppercase tracking-[0.06em] text-muted-foreground">
            남은 시간
          </span>
          <EventCountdown endTime={endTime} />
        </div>
      </Card>
    </Link>
  );
}
