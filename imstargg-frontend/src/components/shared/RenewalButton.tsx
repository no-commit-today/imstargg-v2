"use client";

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { usePlayerRenewal } from "@/hooks/usePlayerRenewal";

type Props =
  | { kind: "player"; tag: string }
  | { kind: "club"; tag: string };

export function RenewalButton(props: Props) {
  if (props.kind === "player") return <PlayerRenewalButton tag={props.tag} />;
  return <ClubRenewalButtonPlaceholder />;
}

function PlayerRenewalButton({ tag }: { tag: string }) {
  const { progressing, canRenew, trigger, triggering } = usePlayerRenewal(tag);
  const disabled = progressing || triggering || !canRenew;
  return (
    <Button
      type="button"
      variant="default"
      size="sm"
      disabled={disabled}
      onClick={trigger}
      aria-label="플레이어 정보 갱신"
    >
      <RefreshCw className={progressing ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{progressing ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}

function ClubRenewalButtonPlaceholder() {
  return (
    <Button type="button" variant="default" size="sm" disabled>
      <RefreshCw className="size-4" />
      <span className="ml-2">갱신</span>
    </Button>
  );
}
