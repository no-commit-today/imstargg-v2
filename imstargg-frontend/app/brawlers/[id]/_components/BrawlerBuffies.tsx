"use client";

import Image from "next/image";
import type { BuffyResponse } from "@/api/types";
import type { Buffies } from "@/domain/brawler/types";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";

function BuffyChip({ buffy }: { buffy: BuffyResponse }) {
  const label = buffyTypeLabel(buffy.type);
  return (
    <div className="flex items-center gap-2 rounded border bg-background px-2.5 py-1.5">
      {buffy.imageUrl && (
        <Image
          src={buffy.imageUrl}
          alt={label}
          width={20}
          height={20}
          unoptimized
          className="h-5 w-5"
        />
      )}
      <span className="text-xs font-bold">{label}</span>
    </div>
  );
}

export function BrawlerBuffies({ buffies }: { buffies: Buffies }) {
  // 캐시 등으로 인해 옛 array shape 또는 부분 누락이 들어올 수 있어 방어
  if (!buffies?.gadget || !buffies?.starPower || !buffies?.hyperCharge) {
    return null;
  }
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="mb-3 text-sm font-bold">버피</h2>
      <div className="flex flex-wrap gap-2">
        <BuffyChip buffy={buffies.gadget} />
        <BuffyChip buffy={buffies.starPower} />
        <BuffyChip buffy={buffies.hyperCharge} />
      </div>
    </section>
  );
}
