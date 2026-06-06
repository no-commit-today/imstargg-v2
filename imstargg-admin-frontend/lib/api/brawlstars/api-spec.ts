import { request } from "@/lib/api";
import type { ApiSpecDetail, ApiSpecListItem } from "@/lib/types";

export async function getApiSpecs(): Promise<ApiSpecListItem[]> {
  return request<ApiSpecListItem[]>("/api-specs");
}

export async function getApiSpecDetail(id: number): Promise<ApiSpecDetail> {
  return request<ApiSpecDetail>(`/api-specs/${id}`);
}
