"use client";

import { useFeedbackDialog } from "@/hooks/use-feedback-dialog";

export function FeedbackFooterLink() {
  const { setOpen } = useFeedbackDialog();
  return (
    <button
      type="button"
      onClick={() => setOpen(true)}
      className="text-neutral-500 hover:text-neutral-700 underline-offset-2 hover:underline"
    >
      의견 보내기
    </button>
  );
}
