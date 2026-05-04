"use client";

import type {
  PlayerBrawler,
  PlayerBrawlerBuffies,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";
import ItemSlot from "@/components/primitives/ItemSlot";
import type { ItemKind } from "@/lib/itemPlaceholder";

const SIZE = {
  item: 20, // gadget / starPower / gear
  buffy: 18, // buffy (작은 보조 아이템)
  hc: 22, // HyperCharge (단독 강조 슬롯)
} as const;

function SlotGroup({
  items,
  kind,
  label,
}: {
  items: PlayerBrawlerOwnedItem[];
  kind: ItemKind;
  label: string;
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
          size={SIZE.item}
          ariaLabel={`${label} ${it.item.name} ${it.owned ? "보유" : "미보유"}`}
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
      size={SIZE.hc}
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
        size={SIZE.buffy}
        ariaLabel={`버피 가젯 ${buffies.gadget.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.starPower.item.imageUrl}
        owned={buffies.starPower.owned}
        size={SIZE.buffy}
        ariaLabel={`버피 스타파워 ${buffies.starPower.owned ? "보유" : "미보유"}`}
      />
      <ItemSlot
        kind="buffy"
        imageUrl={buffies.hyperCharge.item.imageUrl}
        owned={buffies.hyperCharge.owned}
        size={SIZE.buffy}
        ariaLabel={`버피 하이퍼차지 ${buffies.hyperCharge.owned ? "보유" : "미보유"}`}
      />
    </div>
  );
}

export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
      <SlotGroup items={pb.gadgets} kind="gadget" label="가젯" />
      <SlotGroup items={pb.starPowers} kind="starPower" label="스타파워" />
      <SlotGroup items={pb.gears} kind="gear" label="기어" />
      {pb.buffies && <BuffyGroup buffies={pb.buffies} />}
      {pb.hyperCharge && <HyperChargeSlot hc={pb.hyperCharge} />}
    </div>
  );
}
