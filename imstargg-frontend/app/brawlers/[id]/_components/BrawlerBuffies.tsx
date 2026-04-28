"use client";

import type { BuffyResponse } from "@/api/types";

const TYPE_LABEL: Record<string, string> = {
  MYTHIC_GEAR: "미식 기어",
};

export function BrawlerBuffies({ buffies }: { buffies: BuffyResponse[] }) {
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="mb-3 text-sm font-bold">버피 ({buffies.length})</h2>
      <div className="flex flex-wrap gap-2">
        {buffies.map((b, i) => {
          const label = TYPE_LABEL[b.type] ?? b.type;
          return (
            <div
              key={`${b.type}-${i}`}
              className="flex items-center gap-2 rounded border bg-background px-2.5 py-1.5"
            >
              {b.imageUrl && (
                <img src={b.imageUrl} alt={label} width={20} height={20} className="h-5 w-5" />
              )}
              <span className="text-xs font-bold">{label}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
