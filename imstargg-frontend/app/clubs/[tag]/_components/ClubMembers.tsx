"use client";

import { useRouter } from "next/navigation";
import type { ClubMember } from "@/domain/club/types";
import type { ClubMemberRole } from "@/api/types";
import { Button } from "@/components/ui/button";
import { stripHash } from "@/lib/tag";
import Trophy from "@/components/primitives/Trophy";
import { cn } from "@/lib/cn";

const ROLE_INFO: Record<ClubMemberRole, { label: string; color: string } | null> = {
  PRESIDENT: { label: "회장", color: "#FFC60A" },
  VICE_PRESIDENT: { label: "부회장", color: "#B83BFF" },
  SENIOR: { label: "시니어", color: "#2E7CF6" },
  MEMBER: { label: "멤버", color: "#8A90A0" },
  UNKNOWN: null,
};

export function ClubMembers({ members, className }: { members: ClubMember[]; className?: string }) {
  const router = useRouter();
  const goPlayer = (tag: string) => router.push(`/players/${stripHash(tag)}`);

  return (
    <section className={cn("card", className)}>
      <header className="card-head">
        <div className="card-title">멤버 ({members.length})</div>
      </header>
      <div role="table" aria-label="클럽 멤버 목록">
        <div
          role="row"
          className="grid grid-cols-[1fr_84px_88px] gap-2 border-b bg-muted/30 px-3 py-2 text-[11px] font-bold uppercase tracking-[0.06em] text-muted-foreground sm:grid-cols-[40px_1fr_100px_120px_64px] sm:px-4"
        >
          <span role="columnheader" className="hidden sm:block">#</span>
          <span role="columnheader">이름</span>
          <span role="columnheader">역할</span>
          <span role="columnheader" className="text-right">트로피</span>
          <span role="columnheader" className="hidden sm:block" />
        </div>
        {members.map((m, i) => {
          const role = ROLE_INFO[m.role];
          return (
            <div
              key={m.tag}
              role="row"
              onClick={() => goPlayer(m.tag)}
              className="grid cursor-pointer grid-cols-[1fr_84px_88px] items-center gap-2 border-b px-3 py-2 hover:bg-muted/40 sm:grid-cols-[40px_1fr_100px_120px_64px] sm:px-4"
            >
              <span className="num hidden text-sm text-muted-foreground sm:block">{i + 1}</span>
              <div className="flex min-w-0 items-center gap-2.5">
                <div
                  aria-hidden
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full font-display text-xs font-extrabold text-white"
                  style={{ background: `hsl(${(i * 47) % 360} 60% 55%)` }}
                >
                  {m.name.slice(0, 1)}
                </div>
                <div className="min-w-0">
                  <div className="truncate text-sm font-bold">{m.name}</div>
                  <div className="num truncate text-[10px] text-muted-foreground">{m.tag}</div>
                </div>
              </div>
              {role ? (
                <span
                  className="inline-flex w-fit items-center gap-1 rounded px-2 py-0.5 text-[11px] font-bold"
                  style={{ background: role.color + "22", color: role.color }}
                >
                  <span
                    aria-hidden
                    className="h-1.5 w-1.5 rounded-full"
                    style={{ background: role.color }}
                  />
                  {role.label}
                </span>
              ) : (
                <span />
              )}
              <span className="num text-right text-sm font-extrabold">
                <Trophy value={m.trophies} size={14} />
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="hidden sm:inline-flex"
                onClick={(e) => {
                  e.stopPropagation();
                  goPlayer(m.tag);
                }}
              >
                전적
              </Button>
            </div>
          );
        })}
      </div>
    </section>
  );
}
