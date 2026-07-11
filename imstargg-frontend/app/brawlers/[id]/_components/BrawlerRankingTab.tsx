"use client";

import { useSearchParams } from "next/navigation";
import { CountrySelect } from "@/components/ranking/CountrySelect";
import { BrawlerRankingList } from "@/components/ranking/BrawlerRankingList";
import { parseCountry } from "@/domain/ranking/country";
import { useBrawlerRanking } from "@/hooks/queries/useBrawlerRanking";

interface Props {
  brawlerId: number;
}

export function BrawlerRankingTab({ brawlerId }: Props) {
  const searchParams = useSearchParams();
  const country = parseCountry(searchParams.get("country") ?? undefined);

  const { data, isLoading } = useBrawlerRanking(brawlerId, country);

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <CountrySelect current={country} />
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
