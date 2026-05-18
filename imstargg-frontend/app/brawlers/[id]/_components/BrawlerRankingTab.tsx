"use client";

import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "next/navigation";
import { rankingApi } from "@/api/endpoints/ranking";
import { CountrySelector } from "@app/ranking/_components/CountrySelector";
import { BrawlerRankingList } from "@app/ranking/_components/BrawlerRankingList";
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
      <div className="flex items-center justify-end">
        <CountrySelector current={country} />
      </div>
      {isLoading ? (
        <p style={{ color: "#5C6271" }}>로딩 중…</p>
      ) : (
        <BrawlerRankingList rankings={data ?? []} />
      )}
    </div>
  );
}
