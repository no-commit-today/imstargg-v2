import { create } from "zustand";

interface FeedbackDialogState {
  open: boolean;
  setOpen: (open: boolean) => void;
}

export const useFeedbackDialogStore = create<FeedbackDialogState>((set) => ({
  open: false,
  setOpen: (open) => set({ open }),
}));
