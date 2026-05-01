import type { Brawler } from "@/domain/brawler/types";
import type { ItemOwnership, ItemOwnershipItem } from "@/domain/statistics/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";
import { TrophyRangeSelector } from "@app/_components/statistics/TrophyRangeSelector";

type Props = {
  brawler: Brawler;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

function findItemName(brawler: Brawler, key: keyof Brawler, itemId: number): string {
  const list = brawler[key] as Array<{ id: number; name: string }>;
  return list.find((x) => x.id === itemId)?.name ?? `#${itemId}`;
}

function ItemRow({
  name,
  rate,
}: {
  name: string;
  rate: number;
}) {
  const pct = Math.round(rate * 100);
  return (
    <div className="flex items-center gap-2 py-1.5">
      <span className="flex-1 truncate text-sm">{name}</span>
      <div className="h-1.5 w-24 overflow-hidden rounded-full bg-[#ECEEF3] sm:w-32">
        <div className="h-full bg-[#FFC60A]" style={{ width: `${pct}%` }} />
      </div>
      <span className="w-12 text-right text-xs font-bold tabular-nums">{pct}%</span>
    </div>
  );
}

function CategoryCard({
  title,
  items,
  resolveName,
}: {
  title: string;
  items: ItemOwnershipItem[];
  resolveName: (itemId: number) => string;
}) {
  if (items.length === 0) return null;
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white p-3">
      <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-[#5C6271]">
        {title}
      </h4>
      {items.map((it) => (
        <ItemRow key={it.itemId} name={resolveName(it.itemId)} rate={it.ownershipRate} />
      ))}
    </div>
  );
}

export function ItemOwnershipSection({ brawler, ownership, trophy }: Props) {
  const isAllEmpty =
    ownership.gears.length === 0 &&
    ownership.gadgets.length === 0 &&
    ownership.starPowers.length === 0 &&
    ownership.hyperCharges.length === 0 &&
    ownership.buffies.length === 0;

  return (
    <section className="space-y-3">
      <div className="flex items-end justify-between">
        <h3 className="text-sm font-bold">아이템 보유율</h3>
        <TrophyRangeSelector trophy={trophy} />
      </div>
      {isAllEmpty ? (
        <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
          보유율 데이터가 없습니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <CategoryCard
            title="기어"
            items={ownership.gears}
            resolveName={(id) => findItemName(brawler, "gears", id)}
          />
          <CategoryCard
            title="가젯"
            items={ownership.gadgets}
            resolveName={(id) => findItemName(brawler, "gadgets", id)}
          />
          <CategoryCard
            title="스타파워"
            items={ownership.starPowers}
            resolveName={(id) => findItemName(brawler, "starPowers", id)}
          />
          <CategoryCard
            title="하이퍼차지"
            items={ownership.hyperCharges}
            resolveName={(id) =>
              brawler.hyperCharge?.id === id ? brawler.hyperCharge.name : `#${id}`
            }
          />
          {ownership.buffies.length > 0 && (
            <div className="rounded-lg border border-[#D8DCE5] bg-white p-3">
              <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-[#5C6271]">버피</h4>
              {ownership.buffies.map((b) => (
                <ItemRow key={b.buffyType} name={buffyTypeLabel(b.buffyType)} rate={b.ownershipRate} />
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
