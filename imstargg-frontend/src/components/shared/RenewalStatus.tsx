"use client";

import { RenewalProgress, renewalEstimateMs } from "@/components/shared/RenewalProgress";
import { useRenewal, type RenewalKind } from "@/hooks/useRenewal";
import { formatRelativeTime } from "@/lib/relativeTime";

export function RenewalStatus({ kind, tag }: { kind: RenewalKind; tag: string }) {
  const { renewal, renewedAt, progressing, canRenew, triggerFailed } = useRenewal(kind, tag);
  const text = (() => {
    if (triggerFailed) return "갱신 실패 — 잠시 후 다시 시도해 주세요";
    if (progressing && canRenew) return "갱신이 지연되고 있어요 — 다시 시도해 주세요";
    if (progressing) return "최근 업데이트: 진행 중…";
    if (renewal?.status === "FAILED") return "갱신 실패 — 잠시 후 다시 시도해 주세요";
    if (!renewedAt) return "최근 업데이트 없음";
    return `최근 업데이트: ${formatRelativeTime(renewedAt)}`;
  })();
  return (
    <span className="flex flex-col items-end gap-1 text-sm text-[#5C6271] sm:items-start">
      <span>{text}</span>
      {progressing && (
        <RenewalProgress
          active={progressing}
          estimateMs={renewalEstimateMs(renewedAt)}
          className="w-28"
        />
      )}
    </span>
  );
}
