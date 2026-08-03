"use client";

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { useRenewal, type RenewalKind } from "@/hooks/useRenewal";

const ARIA: Record<RenewalKind, string> = {
  player: "플레이어 정보 갱신",
  club: "클럽 정보 갱신",
};

export function RenewalButton({ kind, tag }: { kind: RenewalKind; tag: string }) {
  const { progressing, canRenew, trigger, triggering } = useRenewal(kind, tag);
  // progressing 이어도 canRenew(백엔드 stale 판정)면 재시도를 허용한다 — 멈춘 갱신의 유일한 UI 복구 경로
  const busy = triggering || (progressing && !canRenew);
  const disabled = triggering || !canRenew;
  return (
    <Button type="button" variant="default" size="sm" disabled={disabled} onClick={trigger} aria-label={ARIA[kind]}>
      <RefreshCw className={busy ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{busy ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}
