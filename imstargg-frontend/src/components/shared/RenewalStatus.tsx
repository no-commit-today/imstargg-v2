"use client";

import { useRenewal, type RenewalKind } from "@/hooks/useRenewal";
import { formatRelativeTime } from "@/lib/relativeTime";

export function RenewalStatus({ kind, tag }: { kind: RenewalKind; tag: string }) {
  const { renewedAt, progressing, triggerFailed } = useRenewal(kind, tag);
  const text = (() => {
    if (triggerFailed) return "갱신 실패 — 잠시 후 다시 시도해 주세요";
    if (progressing) return "최근 업데이트: 진행 중…";
    if (!renewedAt) return "최근 업데이트 없음";
    return `최근 업데이트: ${formatRelativeTime(renewedAt)}`;
  })();
  return <span className="text-sm text-[#5C6271]">{text}</span>;
}
