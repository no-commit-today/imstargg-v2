"use client";

import type { ClubMember } from "@/domain/club/types";
import { trophyDistribution } from "@/domain/club/members";

export function ClubTrophyDistribution({ members }: { members: ClubMember[] }) {
  const buckets = trophyDistribution(members);
  const total = members.length || 1;

  return (
    <section className="card">
      <header className="card-head">
        <div className="card-title">트로피 분포</div>
      </header>
      <div className="flex flex-col gap-1.5 p-3">
        {buckets.map((b, i) => {
          const pct = (b.count / total) * 100;
          return (
            <div key={b.label} className="flex items-center gap-2">
              <span className="w-12 text-[11px] text-muted-foreground">{b.label}</span>
              <div className="h-3 flex-1 overflow-hidden rounded-sm bg-muted">
                <div
                  className="h-full"
                  style={{
                    width: `${pct}%`,
                    background: i === 0 ? "var(--color-brand-yellow)" : "var(--muted-foreground)",
                  }}
                />
              </div>
              <span className="num w-7 text-right text-[11px]">{b.count}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
