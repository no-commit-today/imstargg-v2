"use client";

import { usePlayerRenewal } from "@/hooks/usePlayerRenewal";
import { useClubRenewal } from "@/hooks/useClubRenewal";
import { formatRelativeTime } from "@/lib/relativeTime";

type Props =
  | { kind: "player"; tag: string }
  | { kind: "club"; tag: string };

export function RenewalStatus(props: Props) {
  if (props.kind === "player") return <PlayerRenewalStatus tag={props.tag} />;
  return <ClubRenewalStatus tag={props.tag} />;
}

function PlayerRenewalStatus({ tag }: { tag: string }) {
  const { renewedAt, progressing } = usePlayerRenewal(tag);
  return <StatusText progressing={progressing} renewedAt={renewedAt} />;
}

function ClubRenewalStatus({ tag }: { tag: string }) {
  const { renewedAt, progressing } = useClubRenewal(tag);
  return <StatusText progressing={progressing} renewedAt={renewedAt} />;
}

function StatusText({
  progressing,
  renewedAt,
}: {
  progressing: boolean;
  renewedAt: string | null;
}) {
  const text = (() => {
    if (progressing) return "최근 업데이트: 진행 중…";
    if (!renewedAt) return "최근 업데이트 없음";
    return `최근 업데이트: ${formatRelativeTime(renewedAt)}`;
  })();
  return <span className="text-sm text-[#5C6271]">{text}</span>;
}
