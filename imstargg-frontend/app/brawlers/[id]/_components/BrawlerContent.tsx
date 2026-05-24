"use client";

import type { BrawlerWithItems } from "@/domain/brawler/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import type { ItemOwnership } from "@/domain/statistics/types";
import { useBrawler } from "@/hooks/queries/useBrawler";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";
import { BrawlerRankingTab } from "./BrawlerRankingTab";

type Props = {
  initialDetail: BrawlerWithItems;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

export function BrawlerContent({ initialDetail, ownership, trophy }: Props) {
  const { data: detail = initialDetail } = useBrawler(initialDetail.brawler.id, {
    initialData: initialDetail,
  });
  return (
    <section className="py-6 space-y-5">
      <BrawlerHero brawler={detail.brawler} />
      <Tabs defaultValue="overview" className="w-full">
        <TabsList
          variant="line"
          className="h-11 w-full justify-start gap-4 border-b border-[#D8DCE5] px-0"
        >
          <TabsTrigger
            value="overview"
            className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]"
          >
            상세
          </TabsTrigger>
          <TabsTrigger
            value="ranking"
            className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]"
          >
            랭킹
          </TabsTrigger>
        </TabsList>
        <TabsContent value="overview">
          <BrawlerItems items={detail.items} ownership={ownership} trophy={trophy} />
        </TabsContent>
        <TabsContent value="ranking">
          <BrawlerRankingTab brawlerId={detail.brawler.id} />
        </TabsContent>
      </Tabs>
    </section>
  );
}
