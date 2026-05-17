"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { createFeedback, type FeedbackCategory } from "@/api/operation/feedback";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  FEEDBACK_CATEGORIES,
  FEEDBACK_CATEGORY_LABELS,
} from "@/components/feedback/feedback-category";

const schema = z.object({
  category: z.enum(["BUG", "FEATURE_REQUEST", "ETC"]),
  content: z
    .string()
    .trim()
    .min(1, "내용을 입력해주세요")
    .max(2000, "최대 2000자까지 입력 가능합니다"),
  email: z
    .union([z.literal(""), z.string().email("이메일 형식이 아닙니다").max(254)])
    .optional(),
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
    defaultValues: { category: "BUG", content: "", email: "" },
  });

  const content = watch("content") ?? "";

  const onSubmit = async (values: FormValues) => {
    try {
      await createFeedback({
        category: values.category as FeedbackCategory,
        content: values.content,
        email: values.email && values.email !== "" ? values.email : undefined,
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
        <Label className="mb-2 block">유형</Label>
        <div className="flex flex-col gap-1">
          {FEEDBACK_CATEGORIES.map((cat) => (
            <label key={cat} className="flex items-center gap-2 text-sm">
              <input type="radio" value={cat} {...register("category")} />
              {FEEDBACK_CATEGORY_LABELS[cat]}
            </label>
          ))}
        </div>
      </div>

      <div>
        <Label htmlFor="feedback-content">
          내용 <span className="text-red-500">*</span>
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

      <div>
        <Label htmlFor="feedback-email">이메일 (선택)</Label>
        <Input
          id="feedback-email"
          type="email"
          placeholder="답변이 필요한 경우 입력"
          aria-invalid={Boolean(errors.email)}
          {...register("email")}
        />
        {errors.email && (
          <p className="text-xs text-red-500">{errors.email.message}</p>
        )}
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
