import type { MessagesResponse } from "@/api/types";

export function pickName(messages: MessagesResponse | undefined | null): string {
  if (!messages) return "(unknown)";
  return messages.ko ?? messages.en ?? Object.values(messages)[0] ?? "(unknown)";
}
