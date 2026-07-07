"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useClub } from "@/hooks/queries/useClub";
import { queryKeys } from "@/hooks/queries/queryKeys";
import type { Club } from "@/domain/club/types";
import type { RenewalResponse } from "@/api/types";
import { sortMembers } from "@/domain/club/members";
import { ClubHeader } from "./ClubHeader";
import { ClubMembers } from "./ClubMembers";
import { ClubTrophyDistribution } from "./ClubTrophyDistribution";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";

type Props = {
  tag: string;
  initialClub: Club;
  initialRenewal: RenewalResponse | null;
};

export function ClubContent({ tag, initialClub, initialRenewal }: Props) {
  const qc = useQueryClient();

  useEffect(() => {
    if (initialRenewal) {
      qc.setQueryData(queryKeys.club.renewal(tag), initialRenewal);
    }
  }, [tag, initialRenewal, qc]);

  const { data: club } = useClub(tag, { initialData: initialClub });
  if (!club) return null;

  const sorted = sortMembers(club.members);

  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-6">
      <ClubHeader club={club} tag={tag} />
      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <ClubTrophyDistribution members={club.members} className="lg:order-2" />
        <ClubMembers members={sorted} className="lg:order-1" />
      </div>
      <AdUnit slot={AD_SLOTS.clubDetail} />
    </main>
  );
}
