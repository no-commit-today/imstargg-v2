import { useInfiniteQuery } from "@tanstack/react-query";
import { battleApi } from "@/api/endpoints/battle";
import type { BattleResponse, SliceResponse } from "@/api/types";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toBattle } from "@/domain/battle/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export const BATTLES_PAGE_SIZE = 25;

export interface PlayerBattlesOptions {
  /** RSC 가 프리페치한 첫 페이지 (원본 DTO — select 이전 형태) */
  initialPage?: SliceResponse<BattleResponse>;
  /** 서버 페치 시각(ms) — 신선도 계산용 */
  fetchedAt?: number;
}

export function usePlayerBattles(tag: string, options?: PlayerBattlesOptions) {
  return useInfiniteQuery({
    queryKey: queryKeys.player.battles(tag),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) =>
      battleApi.getPlayerBattles(
        tag,
        { offset: pageParam as number, limit: BATTLES_PAGE_SIZE },
        { signal },
      ),
    getNextPageParam: (lastPage, _pages, lastPageParam) =>
      lastPage.hasNext ? (lastPageParam as number) + BATTLES_PAGE_SIZE : undefined,
    select: (data) => ({
      pages: data.pages.map((p) => p.content.map(toBattle)),
      pageParams: data.pageParams,
    }),
    initialData: options?.initialPage
      ? { pages: [options.initialPage], pageParams: [0] }
      : undefined,
    initialDataUpdatedAt: options?.fetchedAt,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
