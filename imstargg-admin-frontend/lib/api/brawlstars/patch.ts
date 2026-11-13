import { request } from "@/lib/api";
import type { BrawlStarsPatch, BrawlStarsPatchList } from "@/lib/types";

interface PatchBody {
  patchDate: string;
  memo: string | null;
}

export async function getPatches(): Promise<BrawlStarsPatchList> {
  return request<BrawlStarsPatchList>("/patches");
}

export async function registerPatch(data: PatchBody): Promise<BrawlStarsPatch> {
  return request<BrawlStarsPatch>("/patches", { method: "POST", body: JSON.stringify(data) });
}

export async function updatePatch(id: number, data: PatchBody): Promise<void> {
  return request<void>(`/patches/${id}`, { method: "PUT", body: JSON.stringify(data) });
}

export async function deletePatch(id: number): Promise<void> {
  return request<void>(`/patches/${id}`, { method: "DELETE" });
}
