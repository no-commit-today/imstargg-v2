"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { isValidTag } from "@/lib/tag";
import { routes } from "@/config/routes";
import { toast } from "sonner";
import { renewalApi } from "@/api/endpoints/renewal";

interface Props {
  kind: "player" | "club";
  defaultTag?: string;
}

export function RenewalCard({ kind, defaultTag = "" }: Props) {
  const router = useRouter();
  const [tag, setTag] = useState(defaultTag);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = tag.trim();
    if (!isValidTag(trimmed)) {
      toast.error(
        `올바른 ${kind === "player" ? "플레이어" : "클럽"} 태그를 입력해 주세요. 예: #88J8Q9RPV`,
      );
      return;
    }
    setSubmitting(true);
    try {
      if (kind === "player") {
        await renewalApi.renewPlayer(trimmed);
      } else {
        await renewalApi.renewClub(trimmed);
      }
      toast.success("갱신 요청 접수 — 잠시 후 결과 페이지에서 확인하세요.");
      router.push(kind === "player" ? routes.player(trimmed) : routes.club(trimmed));
    } catch {
      toast.error("갱신 요청 실패 — 잠시 후 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section
      style={{
        background: "#FFFFFF",
        border: "1px solid #D8DCE5",
        borderRadius: 12,
        padding: 18,
        marginTop: 24,
      }}
    >
      <h2 style={{ margin: 0, fontSize: 15, fontWeight: 700 }}>원하는 결과가 없나요?</h2>
      <p style={{ margin: "6px 0 12px", fontSize: 13, color: "#5C6271" }}>
        태그를 입력해 {kind === "player" ? "플레이어" : "클럽"} 정보를 직접 갱신할 수 있습니다.
      </p>
      <form onSubmit={handleSubmit} className="flex w-full max-w-md items-center gap-2">
        <Input
          type="text"
          inputMode="text"
          spellCheck={false}
          placeholder={kind === "player" ? "#PLAYER_TAG" : "#CLUB_TAG"}
          value={tag}
          onChange={(e) => setTag(e.target.value)}
          aria-label={kind === "player" ? "플레이어 태그 입력" : "클럽 태그 입력"}
          disabled={submitting}
        />
        <Button type="submit" size="default" disabled={submitting}>
          {submitting ? "요청 중…" : "갱신"}
        </Button>
      </form>
    </section>
  );
}
