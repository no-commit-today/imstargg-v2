"use client";

import type { Club } from "@/domain/club/types";
import type { ClubType } from "@/api/types";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { RenewalStatus } from "@/components/shared/RenewalStatus";
import { formatNumber } from "@/lib/format";

const TYPE_LABEL: Record<ClubType, string> = {
  OPEN: "가입 가능",
  INVITE_ONLY: "가입 신청 필요",
  CLOSED: "비공개",
  UNKNOWN: "-",
};

export function ClubHeader({ club, tag }: { club: Club; tag: string }) {
  const memberCount = club.members.length;
  const avgTrophies = memberCount
    ? Math.floor(club.members.reduce((s, m) => s + m.trophies, 0) / memberCount)
    : 0;

  return (
    <section className="card overflow-hidden">
      <div className="grid grid-cols-1 lg:grid-cols-[180px_1fr_240px] bg-[linear-gradient(135deg,#2A2E38,#1A1D24)] text-white">
        <div className="flex flex-col items-center justify-center gap-2 p-7 border-r border-white/5">
          <div className="flex h-28 w-28 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#FFC60A,#FF8A1F)] shadow-[0_6px_20px_rgba(255,198,10,0.4)]">
            <img src="/assets/icon_club_league_masters.png" alt="" className="h-16 w-16" />
          </div>
          <div className="font-mono text-xs text-white/70">{club.tag}</div>
        </div>

        <div className="p-6">
          <div className="mb-1 flex items-center gap-2">
            <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-white/60">
              CLUB
            </span>
            <span className="rounded bg-[rgba(255,198,10,0.2)] px-2 py-0.5 text-[11px] font-bold text-[var(--color-brand-yellow)]">
              {TYPE_LABEL[club.type]}
            </span>
          </div>
          <h1 className="m-0 text-3xl font-display tracking-[0.02em]">{club.name}</h1>
          <p className="mt-3 max-w-xl text-[13px] leading-relaxed text-white/85">
            {club.description}
          </p>

          <div className="mt-4 flex flex-wrap gap-6">
            <Stat label="총 트로피" value={formatNumber(club.trophies)} icon="icon_trophy.png" />
            <Stat label="멤버" value={`${memberCount}/30`} />
            <Stat label="필요 트로피" value={formatNumber(club.requiredTrophies)} />
            <Stat label="평균" value={formatNumber(avgTrophies)} />
          </div>
        </div>

        <div className="flex flex-col justify-center gap-3 border-t border-white/5 p-6 lg:border-l lg:border-t-0">
          <div className="rounded bg-white/5 p-3 text-[11px] leading-relaxed text-white/70">
            <strong className="mb-1 block text-white">🏛 클럽 타입</strong>
            <span className="font-bold text-[var(--color-brand-yellow)]">
              {TYPE_LABEL[club.type]}
            </span>
          </div>
          <div className="flex flex-col items-end gap-1 sm:flex-row sm:items-center sm:gap-3 [&_span]:text-white/60">
            <RenewalStatus kind="club" tag={tag} />
            <RenewalButton kind="club" tag={tag} />
          </div>
        </div>
      </div>
    </section>
  );
}

function Stat({ label, value, icon }: { label: string; value: string; icon?: string }) {
  return (
    <div>
      <div className="mb-0.5 text-[10px] font-bold uppercase tracking-[0.06em] text-white/60">
        {label}
      </div>
      <div className="num flex items-center gap-1 text-lg font-extrabold text-white">
        {icon && <img src={`/assets/${icon}`} alt="" className="h-4 w-4" />}
        {value}
      </div>
    </div>
  );
}
