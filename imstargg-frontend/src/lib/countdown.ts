export interface Countdown {
  hours: number;
  minutes: number;
  seconds: number;
  totalMs: number;
  ended: boolean;
}

export function computeCountdown(endIso: string, now: number = Date.now()): Countdown {
  const end = new Date(endIso).getTime();
  const totalMs = end - now;
  const ended = totalMs <= 0;
  const safe = Math.max(totalMs, 0);
  const totalSeconds = Math.floor(safe / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return { hours, minutes, seconds, totalMs, ended };
}

export function formatCountdown(c: Countdown): string {
  if (c.ended) return "종료됨";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(c.hours)}:${pad(c.minutes)}:${pad(c.seconds)}`;
}
