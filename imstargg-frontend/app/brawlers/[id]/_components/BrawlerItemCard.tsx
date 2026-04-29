"use client";

import Image from "next/image";
import type { BrawlerItem } from "@/domain/brawler/types";

export function BrawlerItemCard({ item }: { item: BrawlerItem }) {
  return (
    <div className="flex items-center gap-3 rounded border bg-card p-2.5">
      <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded bg-muted">
        {item.imageUrl ? (
          <Image
            src={item.imageUrl}
            alt={item.name}
            width={48}
            height={48}
            unoptimized
            className="h-full w-full object-contain"
          />
        ) : (
          <span className="font-display text-lg font-extrabold text-muted-foreground">
            {item.name.slice(0, 1)}
          </span>
        )}
      </div>
      <div className="min-w-0 text-sm font-bold truncate">{item.name}</div>
    </div>
  );
}
