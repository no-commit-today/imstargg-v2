"use client";

import Image from "next/image";
import Link from "next/link";
import type { RotationEvent } from "@/domain/event/types";
import { EventCountdown } from "./EventCountdown";
import { getModeStyle } from "./modeStyles";

export function RotationCard({ rotation }: { rotation: RotationEvent }) {
  const { event, endTime } = rotation;
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);
  const modeStyle = getModeStyle(modeName);

  return (
    <Link href={`/events/${event.id}`} className="event-card group">
      <div
        className="event-card-head"
        style={{ background: modeStyle.bg, color: modeStyle.fg }}
      >
        <div className="event-card-mode">
          {event.mode?.iconImageUrl ? (
            <Image
              src={event.mode.iconImageUrl}
              alt={modeName}
              width={56}
              height={56}
              className="event-card-mode-icon"
              unoptimized
            />
          ) : (
            <div className="event-card-mode-icon event-card-mode-fallback">
              {initial}
            </div>
          )}
          <span className="event-card-mode-label">{modeName}</span>
        </div>
        <span className="event-card-live">
          <span className="event-card-live-dot" aria-hidden="true" />
          LIVE
        </span>
      </div>
      <div className="event-card-body">
        <span className="event-card-overline">MAP</span>
        <h3 className="event-card-mapname">{event.mapName}</h3>
      </div>
      <div className="event-card-footer">
        <span className="event-card-footer-label">남은 시간</span>
        <EventCountdown endTime={endTime} large />
      </div>
    </Link>
  );
}
