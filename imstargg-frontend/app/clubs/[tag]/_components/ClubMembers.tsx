"use client";

import { useRouter } from "next/navigation";
import type { ClubMember } from "@/domain/club/types";
import type { ClubMemberRole } from "@/api/types";
import { Button } from "@/components/ui/button";
import { stripHash } from "@/lib/tag";
import { formatNumber } from "@/lib/format";

const ROLE_INFO: Record<ClubMemberRole, { label: string; color: string } | null> = {
  PRESIDENT: { label: "회장", color: "#FFC60A" },
  VICE_PRESIDENT: { label: "부회장", color: "#B83BFF" },
  SENIOR: { label: "시니어", color: "#2E7CF6" },
  MEMBER: { label: "멤버", color: "#8A90A0" },
  UNKNOWN: null,
};

export function ClubMembers({ members }: { members: ClubMember[] }) {
  const router = useRouter();
  const goPlayer = (tag: string) => router.push(`/players/${stripHash(tag)}`);

  return (
    <section className="card">
      <header className="card-head">
        <div className="card-title">멤버 ({members.length})</div>
      </header>
      <div role="table" aria-label="클럽 멤버 목록">
        <div
          role="row"
          className="grid grid-cols-[40px_1fr_100px_120px_64px] gap-2 border-b bg-muted/30 px-4 py-2 text-[11px] font-bold uppercase tracking-[0.06em] text-muted-foreground"
        >
          <span role="columnheader">#</span>
          <span role="columnheader">이름</span>
          <span role="columnheader">역할</span>
          <span role="columnheader" className="text-right">트로피</span>
          <span role="columnheader" />
        </div>
        {members.map((m, i) => {
          const role = ROLE_INFO[m.role];
          return (
            <div
              key={m.tag}
              role="row"
              onClick={() => goPlayer(m.tag)}
              className="grid cursor-pointer grid-cols-[40px_1fr_100px_120px_64px] items-center gap-2 border-b px-4 py-2 hover:bg-muted/40"
            >
              <span className="num text-sm text-muted-foreground">{i + 1}</span>
              <div className="flex items-center gap-2.5 min-w-0">
                <div
                  aria-hidden
                  className="flex h-7 w-7 items-center justify-center rounded-full font-display text-xs font-extrabold text-white"
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
                <img
                  src="/assets/trophy.png"
                  alt=""
                  className="mr-1 inline-block h-3 w-3 align-middle"
                />
                {formatNumber(m.trophies)}
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
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
