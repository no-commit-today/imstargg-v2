"use client";

import { useEffect, useState } from "react";
import { computeCountdown, formatCountdown } from "@/lib/countdown";

export function EventCountdown({ endTime }: { endTime: string }) {
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    const update = () => setNow(Date.now());
    // 첫 갱신은 microtask로 defer — effect 본문에서 동기 setState 회피 (lint rule)
    queueMicrotask(update);
    const t = setInterval(update, 1000);
    return () => clearInterval(t);
  }, []);

  if (now === null) {
    return <span className="font-num text-base font-bold tabular-nums">--:--:--</span>;
  }

  const cd = computeCountdown(endTime, now);
  const urgent = !cd.ended && cd.totalMs < 60_000;

  return (
    <span
      className={`font-num text-base font-bold tabular-nums ${urgent ? "text-danger" : ""}`}
    >
      {formatCountdown(cd)}
    </span>
  );
}
