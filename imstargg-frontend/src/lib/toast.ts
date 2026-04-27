// Toast store — client singleton with subscriptions.

export type ToastKind = "default" | "error" | "success";

export interface Toast {
  id: number;
  message: string;
  kind: ToastKind;
}

const toasts: Toast[] = [];
const listeners = new Set<(list: Toast[]) => void>();
let counter = 0;

export function pushToast(message: string, kind: ToastKind = "default", duration = 3000): void {
  const id = ++counter;
  toasts.push({ id, message, kind });
  listeners.forEach((fn) => fn([...toasts]));
  setTimeout(() => {
    const idx = toasts.findIndex((t) => t.id === id);
    if (idx >= 0) {
      toasts.splice(idx, 1);
      listeners.forEach((fn) => fn([...toasts]));
    }
  }, duration);
}

export function subscribeToasts(fn: (list: Toast[]) => void): () => void {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}

export function getToasts(): Toast[] {
  return [...toasts];
}
