"use client";

import type { BrawlerItems, BrawlerItem } from "@/domain/brawler/types";
import { BrawlerItemCard } from "./BrawlerItemCard";

function Section({ title, items }: { title: string; items: BrawlerItem[] }) {
  if (items.length === 0) return null;
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="mb-3 text-sm font-bold">
        {title} <span className="text-muted-foreground">({items.length})</span>
      </h2>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {items.map((it) => (
          <BrawlerItemCard key={it.id} item={it} />
        ))}
      </div>
    </section>
  );
}

export function BrawlerItems({ items }: { items: BrawlerItems }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Section title="가젯" items={items.gadgets} />
      <Section title="스타파워" items={items.starPowers} />
      <Section title="기어" items={items.gears} />
      <Section title="하이퍼차지" items={items.hyperCharge ? [items.hyperCharge] : []} />
    </div>
  );
}
