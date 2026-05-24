"use client";

import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import { rankingApi } from "@/api/endpoints/ranking";
import { CountryFlagPills } from "@/components/ranking/CountryFlagPills";
import { BrawlerRankingList } from "@/components/ranking/BrawlerRankingList";
import { parseCountry } from "@/domain/ranking/country";
import { toBrawlerRanking } from "@/domain/ranking/mappers";

interface Props {
  brawlerId: number;
}

export function BrawlerRankingTab({ brawlerId }: Props) {
  const searchParams = useSearchParams();
  const country = parseCountry(searchParams.get("country") ?? undefined);

  const { data, isLoading } = useQuery({
    queryKey: ["brawler-ranking", brawlerId, country],
    queryFn: async () => {
      const res = await rankingApi.brawlers(brawlerId, country, { cache: "no-store" });
      return res.content.map(toBrawlerRanking);
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <CountryFlagPills current={country} />
      </div>
      {isLoading ? (
        <div className="data-table">
          <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
            랭킹 불러오는 중…
          </div>
        </div>
      ) : (
        <BrawlerRankingList rankings={data ?? []} />
      )}
    </div>
  );
}
