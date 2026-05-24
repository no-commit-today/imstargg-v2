"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { createFeedback, type FeedbackRating } from "@/api/operation/feedback";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  FEEDBACK_RATINGS,
  FEEDBACK_RATING_LABELS,
  FEEDBACK_RATING_EMOJIS,
} from "@/components/feedback/feedback-rating";

const schema = z.object({
  rating: z.enum(
    ["VERY_SATISFIED", "SATISFIED", "NEUTRAL", "DISSATISFIED", "VERY_DISSATISFIED"],
    {
      required_error: "만족도를 선택해주세요",
      invalid_type_error: "만족도를 선택해주세요",
    },
  ),
  content: z
    .string()
    .trim()
    .min(1, "의견을 입력해주세요")
    .max(2000, "최대 2000자까지 입력 가능합니다"),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  onSuccess: () => void;
  onCancel: () => void;
}

export function FeedbackForm({ onSuccess, onCancel }: Props) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { content: "" },
  });

  const content = watch("content") ?? "";

  const onSubmit = async (values: FormValues) => {
    try {
      await createFeedback({
        rating: values.rating as FeedbackRating,
        content: values.content,
      });
      toast.success("의견이 접수되었습니다. 감사합니다!");
      onSuccess();
    } catch {
      toast.error("전송에 실패했습니다. 잠시 후 다시 시도해주세요");
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <Label className="mb-2 block">
          만족도 <span className="text-red-500">*</span>
        </Label>
        <div role="radiogroup" className="grid grid-cols-5 gap-2">
          {FEEDBACK_RATINGS.map((r) => (
            <label
              key={r}
              className="flex cursor-pointer flex-col items-center gap-1 rounded-md border border-neutral-200 bg-white p-2 text-center transition hover:border-[#B4B9C7] hover:shadow-[0_3px_0_0_rgba(0,0,0,0.35)] has-[:checked]:border-yellow-400 has-[:checked]:shadow-[0_3px_0_0_rgba(0,0,0,0.35),0_0_0_4px_rgba(255,198,10,0.35)]"
            >
              <input
                type="radio"
                value={r}
                {...register("rating")}
                className="sr-only"
              />
              <span className="text-xl" aria-hidden>
                {FEEDBACK_RATING_EMOJIS[r]}
              </span>
              <span className="text-[12px] font-bold leading-none">
                {FEEDBACK_RATING_LABELS[r]}
              </span>
            </label>
          ))}
        </div>
        {errors.rating && (
          <p className="mt-1 text-xs text-red-500">{errors.rating.message}</p>
        )}
      </div>

      <div>
        <Label htmlFor="feedback-content">
          의견 <span className="text-red-500">*</span>
        </Label>
        <Textarea
          id="feedback-content"
          rows={6}
          maxLength={2000}
          aria-invalid={Boolean(errors.content)}
          {...register("content")}
        />
        <div className="flex justify-between text-xs text-muted-foreground">
          <span>{errors.content?.message}</span>
          <span>{content.length} / 2000</span>
        </div>
      </div>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          취소
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "보내는 중..." : "보내기"}
        </Button>
      </div>
    </form>
  );
}
