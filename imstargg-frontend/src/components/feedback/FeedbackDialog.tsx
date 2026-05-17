"use client";

import {
  Dialog,
  DialogContent,
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
          <DialogTitle>의견 보내기</DialogTitle>
        </DialogHeader>
        <FeedbackForm
          onSuccess={() => setOpen(false)}
          onCancel={() => setOpen(false)}
        />
      </DialogContent>
    </Dialog>
  );
}
