import { apiFetch } from "@/api/client";

export type FeedbackRating =
  | "VERY_SATISFIED"
  | "SATISFIED"
  | "NEUTRAL"
  | "DISSATISFIED"
  | "VERY_DISSATISFIED";

export interface FeedbackCreateRequest {
  rating: FeedbackRating;
  content: string;
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
