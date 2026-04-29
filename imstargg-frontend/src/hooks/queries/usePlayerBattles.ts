import { useInfiniteQuery } from "@tanstack/react-query";
import { battleApi } from "@/api/endpoints/battle";
import { toBattle } from "@/domain/battle/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export const BATTLES_PAGE_SIZE = 25;

export function usePlayerBattles(tag: string) {
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
  });
}
