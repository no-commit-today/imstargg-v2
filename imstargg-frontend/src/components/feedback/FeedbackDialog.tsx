"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { FeedbackForm } from "@/components/feedback/FeedbackForm";
import { useFeedbackDialog } from "@/hooks/use-feedback-dialog";

export function FeedbackDialog() {
  const { open, setOpen } = useFeedbackDialog();

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>ImStarGG에 피드백을 전달해주세요.</DialogTitle>
          <DialogDescription>
            주신 의견은 빠짐없이 확인하고 감사히 받아들이겠습니다.
          </DialogDescription>
        </DialogHeader>
        <FeedbackForm
          onSuccess={() => setOpen(false)}
          onCancel={() => setOpen(false)}
        />
      </DialogContent>
    </Dialog>
  );
}
