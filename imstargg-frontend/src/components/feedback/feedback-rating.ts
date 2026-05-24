import type { FeedbackRating } from "@/api/operation/feedback";

export const FEEDBACK_RATINGS: FeedbackRating[] = [
  "VERY_SATISFIED",
  "SATISFIED",
  "NEUTRAL",
  "DISSATISFIED",
  "VERY_DISSATISFIED",
];

export const FEEDBACK_RATING_LABELS: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "매우 만족",
  SATISFIED: "만족",
  NEUTRAL: "보통",
  DISSATISFIED: "불만족",
  VERY_DISSATISFIED: "매우 불만족",
};

export const FEEDBACK_RATING_EMOJIS: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "😄",
  SATISFIED: "🙂",
  NEUTRAL: "😐",
  DISSATISFIED: "🙁",
  VERY_DISSATISFIED: "😡",
};
