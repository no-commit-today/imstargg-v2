import { apiFetch } from "@/api/client";

export type FeedbackCategory = "BUG" | "FEATURE_REQUEST" | "ETC";

export interface FeedbackCreateRequest {
  category: FeedbackCategory;
  content: string;
  email?: string;
}

export interface FeedbackCreateResponse {
  id: number;
}

export async function createFeedback(
  body: FeedbackCreateRequest,
): Promise<FeedbackCreateResponse> {
  return apiFetch<FeedbackCreateResponse>("/v1/feedback", {
    method: "POST",
    body,
  });
}
