"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Checkbox } from "@/components/ui/checkbox";
import { updateFeedbackCheck } from "@/lib/api/operation/feedback";

interface Props {
  id: number;
  initialChecked: boolean;
  onChanged?: (checked: boolean) => void;
}

export function FeedbackCheckToggle({ id, initialChecked, onChanged }: Props) {
  const [checked, setChecked] = useState(initialChecked);
  const [busy, setBusy] = useState(false);

  const handle = async (next: boolean) => {
    setBusy(true);
    const prev = checked;
    setChecked(next); // optimistic
    try {
      await updateFeedbackCheck(id, next);
      onChanged?.(next);
    } catch {
      setChecked(prev);
      toast.error("체크 상태 변경에 실패했습니다");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Checkbox
      checked={checked}
      disabled={busy}
      onCheckedChange={(v) => handle(Boolean(v))}
    />
  );
}
