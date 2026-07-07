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
  const disabled = progressing || triggering || !canRenew;
  return (
    <Button type="button" variant="default" size="sm" disabled={disabled} onClick={trigger} aria-label={ARIA[kind]}>
      <RefreshCw className={progressing ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{progressing ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}
