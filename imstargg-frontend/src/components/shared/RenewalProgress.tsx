"use client";

import { useEffect, useState } from "react";

// prod 갱신 소요 시간 측정치(신규 6~9.5초 / 기존 0.4~3.7초) 에 여유를 둔 값.
// 백엔드 배치 INSERT 개선이 배포되면 실측(로그의 elapsedMs) 을 보고 낮춘다.
const ESTIMATE_MS = { NEW: 10_000, EXISTING: 4_000 } as const;

/** 갱신 이력이 없으면(= 아직 수집된 적 없는 태그) 전량 수집이라 훨씬 오래 걸린다. */
export function renewalEstimateMs(renewedAt: string | null) {
  return renewedAt ? ESTIMATE_MS.EXISTING : ESTIMATE_MS.NEW;
}

type Phase = "idle" | "filling" | "done";

/**
 * 갱신 진행 바. 서버가 진행률을 주지 않으므로 예상 소요 시간 동안 90% 까지 차오르다 멈추고,
 * 실제 완료 시점에 100% 로 채운 뒤 사라진다. 예상보다 오래 걸려도 90% 에서 대기할 뿐 역행하지 않는다.
 */
export function RenewalProgress({
  active,
  estimateMs,
  className,
}: {
  active: boolean;
  estimateMs: number;
  className?: string;
}) {
  const [phase, setPhase] = useState<Phase>(active ? "filling" : "idle");
  const [filling, setFilling] = useState(false);

  useEffect(() => {
    if (active) setPhase("filling");
    else setPhase((p) => (p === "filling" ? "done" : p));
  }, [active]);

  useEffect(() => {
    if (phase !== "filling") {
      setFilling(false);
      return;
    }
    // 마운트 프레임에 목표 width 가 함께 커밋되면 transition 이 생략된다 — 다음 프레임에 시작.
    const id = requestAnimationFrame(() => requestAnimationFrame(() => setFilling(true)));
    return () => cancelAnimationFrame(id);
  }, [phase]);

  useEffect(() => {
    if (phase !== "done") return;
    const t = setTimeout(() => setPhase("idle"), 800);
    return () => clearTimeout(t);
  }, [phase]);

  if (phase === "idle") return null;

  const done = phase === "done";
  return (
    <span
      role="progressbar"
      aria-label="갱신 진행률"
      className={`block h-1 overflow-hidden rounded-full bg-[#ECEEF3] ${className ?? ""}`}
    >
      <span
        className="block h-full rounded-full bg-[#FFC60A] ease-out"
        style={{
          width: done ? "100%" : filling ? "90%" : "3%",
          transitionProperty: "width",
          transitionDuration: `${done ? 300 : estimateMs}ms`,
        }}
      />
    </span>
  );
}
