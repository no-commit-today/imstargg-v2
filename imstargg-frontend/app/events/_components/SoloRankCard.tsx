"use client";

import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import type { BattleEvent } from "@/domain/event/types";

export function SoloRankCard({ event }: { event: BattleEvent }) {
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);

  return (
    <Link href={`/events/${event.id}`} className="block">
      <Card className="flex items-center gap-3 p-3 transition hover:shadow-md">
        {event.mode?.iconImageUrl ? (
          <Image
            src={event.mode.iconImageUrl}
            alt={modeName}
            width={40}
            height={40}
            className="rounded-md"
            style={{ width: 40, height: 40 }}
            unoptimized
          />
        ) : (
          <div
            aria-hidden
            className="flex h-10 w-10 items-center justify-center rounded-md bg-gradient-to-br from-neutral-700 to-neutral-900 font-display text-lg font-bold text-white"
          >
            {initial}
          </div>
        )}
        <div className="min-w-0 flex-1">
          <div className="text-[10px] font-bold uppercase tracking-[0.08em] text-muted-foreground">
            {modeName}
          </div>
          <div className="truncate text-sm font-bold">{event.mapName}</div>
        </div>
      </Card>
    </Link>
  );
}
