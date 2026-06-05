"use client";

import { useRenewal } from "@/hooks/useRenewal";

export function usePlayerRenewal(tag: string) {
  return useRenewal("player", tag);
}
