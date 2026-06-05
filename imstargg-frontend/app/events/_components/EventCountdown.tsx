"use client";

import { useEffect, useState } from "react";
import { computeCountdown, formatCountdown } from "@/lib/countdown";

type Props = {
  endTime: string;
  large?: boolean;
};

export function EventCountdown({ endTime, large = false }: Props) {
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    const update = () => setNow(Date.now());
    // 첫 갱신은 microtask로 defer — effect 본문에서 동기 setState 회피 (lint rule)
    queueMicrotask(update);
    const t = setInterval(update, 1000);
    return () => clearInterval(t);
  }, []);

  const sizeClass = large
    ? "text-[15px] font-extrabold leading-none sm:text-[22px]"
    : "text-base font-bold";

  if (now === null) {
    return (
      <span className={`font-num tabular-nums ${sizeClass}`}>--:--:--</span>
    );
  }

  const cd = computeCountdown(endTime, now);
  const urgent = !cd.ended && cd.totalMs < 60_000;

  return (
    <span
      className={`font-num tabular-nums ${sizeClass} ${urgent ? "text-[var(--color-accent-red)] event-countdown-urgent" : ""}`}
    >
      {formatCountdown(cd)}
    </span>
  );
}
