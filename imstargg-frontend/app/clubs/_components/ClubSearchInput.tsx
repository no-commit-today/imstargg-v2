"use client";

import { useState, type SubmitEvent } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { isValidTag } from "@/lib/tag";
import { routes } from "@/config/routes";
import { toast } from "sonner";

export function ClubSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  const router = useRouter();
  const [value, setValue] = useState(defaultValue);

  function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!isValidTag(trimmed)) {
      toast.error("올바른 클럽 태그를 입력해 주세요. 예: #2U8CVLQQ9");
      return;
    }
    router.push(routes.club(trimmed));
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-md items-center gap-2">
      <Input
        type="text"
        inputMode="text"
        autoCapitalize="characters"
        spellCheck={false}
        placeholder="#CLUB_TAG"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        aria-label="클럽 태그 입력"
      />
      <Button type="submit" size="default" aria-label="클럽 검색">
        <Search className="size-4" />
      </Button>
    </form>
  );
}
