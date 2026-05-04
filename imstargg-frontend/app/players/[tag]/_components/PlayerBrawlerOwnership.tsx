"use client";

import type {
  PlayerBrawler,
  PlayerBrawlerBuffies,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";
import ItemSlot from "@/components/primitives/ItemSlot";
import type { ItemKind } from "@/lib/itemPlaceholder";

function SlotGroup({
  items,
  kind,
  ariaPrefix,
}: {
  items: PlayerBrawlerOwnedItem[];
  kind: ItemKind;
  ariaPrefix: string;
}) {
  if (items.length === 0) return null;
  return (
    <div className="flex gap-1">
      {items.map((it) => (
        <ItemSlot
          key={it.item.id}
          kind={kind}
          imageUrl={it.item.imageUrl}
          owned={it.owned}
          gearRarity={kind === "gear" ? (it.item.rarity ?? undefined) : undefined}
          size={20}
          ariaLabel={`${ariaPrefix} ${it.item.name} ${it.owned ? "보유" : "미보유"}`}
        />
      ))}
    </div>
  );
}

function HyperChargeSlot({ hc }: { hc: PlayerBrawlerOwnedItem }) {
  return (
    <ItemSlot
      kind="hyperCharge"
      imageUrl={hc.item.imageUrl}
      owned={hc.owned}
      size={22}
      ariaLabel={`하이퍼차지 ${hc.owned ? "보유" : "미보유"}`}
    />
  );
}

function BuffyGroup({ buffies }: { buffies: PlayerBrawlerBuffies }) {
  return (
    <div className="flex gap-1">
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.gadget.item.imageUrl}
        owned={buffies.gadget.owned}
        size={18}
        ariaLabel={`버피 가젯 ${buffies.gadget.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.starPower.item.imageUrl}
        owned={buffies.starPower.owned}
        size={18}
        ariaLabel={`버피 스타파워 ${buffies.starPower.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.hyperCharge.item.imageUrl}
        owned={buffies.hyperCharge.owned}
        size={18}
        ariaLabel={`버피 하이퍼차지 ${buffies.hyperCharge.owned ? "보유" : "미보유"}`}
      />
    </div>
  );
}

export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
      <SlotGroup items={pb.gadgets} kind="gadget" ariaPrefix="가젯" />
      <SlotGroup items={pb.starPowers} kind="starPower" ariaPrefix="스타파워" />
      <SlotGroup items={pb.gears} kind="gear" ariaPrefix="기어" />
      {pb.buffies && <BuffyGroup buffies={pb.buffies} />}
      {pb.hyperCharge && <HyperChargeSlot hc={pb.hyperCharge} />}
    </div>
  );
}
