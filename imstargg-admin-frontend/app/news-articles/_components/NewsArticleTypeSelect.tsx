"use client";

import { useState } from "react";
import { toast } from "sonner";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { updateNewsArticleType } from "@/lib/api/brawlstars/news-article";
import type { NewsArticleType } from "@/lib/types";

export const NEWS_ARTICLE_TYPE_LABELS: Record<NewsArticleType, string> = {
  RELEASE_NOTE: "릴리스 노트",
  OTHER: "기타",
};

const TYPES = Object.keys(NEWS_ARTICLE_TYPE_LABELS) as NewsArticleType[];

interface Props {
  id: number;
  initialType: NewsArticleType;
  onChanged?: (type: NewsArticleType) => void;
}

export function NewsArticleTypeSelect({ id, initialType, onChanged }: Props) {
  const [type, setType] = useState(initialType);
  const [busy, setBusy] = useState(false);

  const handle = async (next: NewsArticleType | null) => {
    if (next === null || next === type) return;
    setBusy(true);
    const prev = type;
    setType(next); // optimistic
    try {
      await updateNewsArticleType(id, next);
      toast.success("유형이 변경되었습니다");
      onChanged?.(next);
    } catch {
      setType(prev);
      toast.error("유형 변경에 실패했습니다");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Select
      value={type}
      items={NEWS_ARTICLE_TYPE_LABELS}
      disabled={busy}
      onValueChange={(v) => handle(v as NewsArticleType | null)}
    >
      <SelectTrigger size="sm">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {TYPES.map((t) => (
          <SelectItem key={t} value={t}>
            {NEWS_ARTICLE_TYPE_LABELS[t]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
