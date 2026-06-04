"use client";

import { useRenewal } from "@/hooks/useRenewal";

export function useClubRenewal(tag: string) {
  return useRenewal("club", tag);
}
