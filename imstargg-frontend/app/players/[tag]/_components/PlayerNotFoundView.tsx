"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { usePlayerRenewal } from "@/hooks/usePlayerRenewal";

export function PlayerNotFoundView({ tag }: { tag: string }) {
  const router = useRouter();
  const { progressing } = usePlayerRenewal(tag);
  const prev = useRef(progressing);

  useEffect(() => {
    if (prev.current && !progressing) {
      router.refresh();
    }
    prev.current = progressing;
  }, [progressing, router]);

  return (
    <main className="mx-auto flex w-full max-w-md flex-col items-center gap-4 px-4 py-24 text-center">
      <h1 className="text-2xl font-semibold">플레이어를 찾을 수 없어요</h1>
      <p className="text-sm text-muted-foreground">
        <span className="num">{tag}</span> 플레이어 정보가 아직 없어요. 갱신 요청을 보내면
        브롤스타즈에서 데이터를 가져와요.
      </p>
      <div className="flex items-center gap-2">
        <RenewalButton kind="player" tag={tag} />
        <Button asChild variant="outline">
          <Link href="/">홈으로</Link>
        </Button>
      </div>
      {progressing && (
        <p className="text-xs text-muted-foreground">
          갱신 중… 완료되면 자동으로 플레이어 페이지로 이동해요.
        </p>
      )}
    </main>
  );
}
