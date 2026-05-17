import { useFeedbackDialogStore } from "@/stores/feedback-dialog-store";

export function useFeedbackDialog() {
  const open = useFeedbackDialogStore((s) => s.open);
  const setOpen = useFeedbackDialogStore((s) => s.setOpen);
  return { open, setOpen };
}
