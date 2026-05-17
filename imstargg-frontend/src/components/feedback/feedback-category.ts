import type { FeedbackCategory } from "@/api/operation/feedback";

export const FEEDBACK_CATEGORY_LABELS: Record<FeedbackCategory, string> = {
  BUG: "버그 신고",
  FEATURE_REQUEST: "기능 제안",
  ETC: "기타",
};

export const FEEDBACK_CATEGORIES: FeedbackCategory[] = [
  "BUG",
  "FEATURE_REQUEST",
  "ETC",
];
