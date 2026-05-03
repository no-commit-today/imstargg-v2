"use client";

import type {
  PlayerBrawler,
  PlayerBrawlerBuffies,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";

function DotRow({
  label,
  items,
}: {
  label: string;
  items: PlayerBrawlerOwnedItem[];
}) {
  if (items.length === 0) return null;
  const ownedCount = items.filter((i) => i.owned).length;
  return (
    <div
      className="flex items-center gap-2 text-[11px] text-muted-foreground"
      aria-label={`${label} ${ownedCount}/${items.length}`}
    >
      <span className="w-12 shrink-0 font-semibold">{label}</span>
      <div className="flex gap-1">
        {items.map((it) => (
          <span
            key={it.item.id}
            className={
              it.owned
                ? "h-2 w-2 rounded-full bg-foreground"
                : "h-2 w-2 rounded-full border border-[#D8DCE5] bg-[#ECEEF3]"
            }
          />
        ))}
      </div>
      <span className="ml-auto tabular-nums">
        {ownedCount}/{items.length}
      </span>
    </div>
  );
}

function HyperChargeRow({
  hc,
}: {
  hc: PlayerBrawlerOwnedItem | null;
}) {
  if (!hc) return null;
  const ownedLabel = hc.owned ? "보유" : "미보유";
  return (
    <div
      className="flex items-center gap-2 text-[11px] text-muted-foreground"
      aria-label={`하이퍼차지 ${ownedLabel}`}
    >
      <span className="w-12 shrink-0 font-semibold">하이퍼차지</span>
      <span
        className={
          hc.owned
            ? "rounded bg-[#FFC60A]/20 px-1.5 py-0.5 text-[10px] font-bold text-[#14161B]"
            : "rounded border border-[#D8DCE5] px-1.5 py-0.5 text-[10px] font-bold text-[#8A90A0]"
        }
      >
        {hc.owned ? "✓" : "—"}
      </span>
    </div>
  );
}

function BuffyRow({ buffies }: { buffies: PlayerBrawlerBuffies }) {
  const cell = (label: string, owned: boolean) => (
    <span
      key={label}
      aria-label={`버피 ${label} ${owned ? "보유" : "미보유"}`}
      className={
        owned
          ? "rounded bg-[#14161B] px-1.5 py-0.5 text-[10px] font-bold text-white"
          : "rounded border border-[#D8DCE5] px-1.5 py-0.5 text-[10px] font-bold text-[#8A90A0]"
      }
    >
      {label}
    </span>
  );
  return (
    <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
      <span className="w-12 shrink-0 font-semibold">버피</span>
      <div className="flex gap-1">
        {cell("가젯", buffies.gadget.owned)}
        {cell("스타파워", buffies.starPower.owned)}
        {cell("하이퍼차지", buffies.hyperCharge.owned)}
      </div>
    </div>
  );
}

export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-col gap-1.5">
      <DotRow label="가젯" items={pb.gadgets} />
      <DotRow label="스타파워" items={pb.starPowers} />
      <DotRow label="기어" items={pb.gears} />
      <HyperChargeRow hc={pb.hyperCharge} />
      {pb.buffies && <BuffyRow buffies={pb.buffies} />}
    </div>
  );
}
