"use client";

import { Badge } from "@/components/ui/badge";
import ItemSlot from "@/components/primitives/ItemSlot";
import type { BrawlerDisplayItem } from "@/domain/brawler/itemOwnership";
import type { ItemKind } from "@/lib/itemPlaceholder";

const KIND_LABELS: Record<BrawlerDisplayItem["kind"], string> = {
  gadget: "가젯",
  starPower: "스타파워",
  gear: "기어",
  hyperCharge: "하이퍼차지",
  buffy: "버피",
};

function OwnershipBar({ rate }: { rate: number }) {
  const pct = Math.round(rate * 100);

  return (
    <div className="flex items-center gap-2">
      <div className="h-2 min-w-0 flex-1 overflow-hidden rounded-full bg-muted">
        <div className="h-full rounded-full bg-primary" style={{ width: `${pct}%` }} />
      </div>
      <span className="w-10 text-right text-xs font-bold tabular-nums">{pct}%</span>
    </div>
  );
}

export function BrawlerItemCard({ item }: { item: BrawlerDisplayItem }) {
  const kind: ItemKind = item.kind;

  return (
    <div className="flex min-h-24 items-center gap-3 rounded-lg border bg-card p-3">
      <div className="grid size-12 shrink-0 place-items-center overflow-hidden rounded-md bg-muted">
        <ItemSlot
          kind={kind}
          imageUrl={item.imageUrl}
          owned
          gearRarity={item.gearRarity}
          size={48}
          name={item.name}
          ariaLabel={item.name}
        />
      </div>
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex min-w-0 items-start justify-between gap-2">
          <div className="min-w-0 truncate text-sm font-bold">{item.name}</div>
          <Badge variant="secondary" className="shrink-0">
            {KIND_LABELS[item.kind]}
          </Badge>
        </div>
        {item.ownership ? (
          <div className="flex flex-col gap-1">
            <OwnershipBar rate={item.ownership.rate} />
            <div className="text-xs text-muted-foreground tabular-nums">
              {item.ownership.ownerCount.toLocaleString()} /{" "}
              {item.ownership.totalCount.toLocaleString()}
            </div>
          </div>
        ) : (
          <div className="text-sm font-bold text-muted-foreground">데이터 없음</div>
        )}
      </div>
    </div>
  );
}
