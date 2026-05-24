import { request } from "@/lib/api";
import type { FeedbackListResponse, FeedbackSearchParams } from "@/lib/types";

export async function getFeedbacks(params: FeedbackSearchParams): Promise<FeedbackListResponse> {
  const search = new URLSearchParams();
  if (params.rating) search.set("rating", params.rating);
  if (params.checked !== undefined) search.set("checked", String(params.checked));
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));

  const query = search.toString();
  return request<FeedbackListResponse>(`/feedbacks${query ? `?${query}` : ""}`);
}

export async function updateFeedbackCheck(id: number, checked: boolean): Promise<void> {
  return request<void>(`/feedbacks/${id}/check`, {
    method: "PUT",
    body: JSON.stringify({ checked }),
  });
}
