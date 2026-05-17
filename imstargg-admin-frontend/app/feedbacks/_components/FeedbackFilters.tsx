"use client";

import { useRouter, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const ALL = "ALL";

const CATEGORIES = [ALL, "BUG", "FEATURE_REQUEST", "ETC"] as const;
const CHECKED_OPTIONS = [ALL, "false", "true"] as const;

const CATEGORY_LABELS: Record<string, string> = {
  [ALL]: "카테고리 (전체)",
  BUG: "BUG",
  FEATURE_REQUEST: "FEATURE_REQUEST",
  ETC: "ETC",
};

const CHECKED_LABELS: Record<string, string> = {
  [ALL]: "상태 (전체)",
  true: "체크됨",
  false: "미체크",
};

export function FeedbackFilters() {
  const router = useRouter();
  const params = useSearchParams();

  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params?.toString() ?? "");
    if (value === ALL) next.delete(key);
    else next.set(key, value);
    next.delete("page");
    router.replace(`/feedbacks?${next.toString()}`);
  };

  const category = params?.get("category") ?? ALL;
  const checked = params?.get("checked") ?? ALL;

  return (
    <div className="flex gap-2">
      <Select value={category} onValueChange={(v) => update("category", v ?? ALL)}>
        <SelectTrigger className="w-44">
          <SelectValue placeholder={CATEGORY_LABELS[ALL]} />
        </SelectTrigger>
        <SelectContent>
          {CATEGORIES.map((c) => (
            <SelectItem key={c} value={c}>
              {CATEGORY_LABELS[c]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={checked} onValueChange={(v) => update("checked", v ?? ALL)}>
        <SelectTrigger className="w-36">
          <SelectValue placeholder={CHECKED_LABELS[ALL]} />
        </SelectTrigger>
        <SelectContent>
          {CHECKED_OPTIONS.map((c) => (
            <SelectItem key={c} value={c}>
              {CHECKED_LABELS[c]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
