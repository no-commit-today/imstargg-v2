import { useInfiniteQuery } from "@tanstack/react-query";
import { playerApi } from "@/api/endpoints/player";
import { clubApi } from "@/api/endpoints/club";
import type { PlayerResponse, ClubResponse, SliceResponse } from "@/api/types";
import { QUERY_STALE_TIME } from "@/config/cache";
import { toPlayer } from "@/domain/player/mappers";
import { toClub } from "@/domain/club/mappers";
import { queryKeys } from "@/hooks/queries/queryKeys";

export const SEARCH_PAGE_SIZE = 20;

export interface SearchOptions<TDto> {
  /** RSC 가 프리페치한 첫 페이지 (원본 DTO — select 이전 형태) */
  initialPage?: SliceResponse<TDto>;
  /** 서버 페치 시각(ms) — 신선도 계산용 */
  fetchedAt?: number;
}

export function usePlayerSearch(
  query: string,
  options?: SearchOptions<PlayerResponse>,
) {
  return useInfiniteQuery({
    queryKey: queryKeys.player.search(query),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) =>
      playerApi.search(query, pageParam as number, SEARCH_PAGE_SIZE, { signal }),
    getNextPageParam: (lastPage, _pages, lastPageParam) =>
      lastPage.hasNext ? (lastPageParam as number) + SEARCH_PAGE_SIZE : undefined,
    select: (data) => ({
      pages: data.pages.map((p) => p.content.map(toPlayer)),
      pageParams: data.pageParams,
    }),
    initialData: options?.initialPage
      ? { pages: [options.initialPage], pageParams: [0] }
      : undefined,
    initialDataUpdatedAt: options?.fetchedAt,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}

export function useClubSearch(
  query: string,
  options?: SearchOptions<ClubResponse>,
) {
  return useInfiniteQuery({
    queryKey: queryKeys.club.search(query),
    initialPageParam: 0,
    queryFn: ({ pageParam, signal }) =>
      clubApi.search(query, pageParam as number, SEARCH_PAGE_SIZE, { signal }),
    getNextPageParam: (lastPage, _pages, lastPageParam) =>
      lastPage.hasNext ? (lastPageParam as number) + SEARCH_PAGE_SIZE : undefined,
    select: (data) => ({
      pages: data.pages.map((p) => p.content.map(toClub)),
      pageParams: data.pageParams,
    }),
    initialData: options?.initialPage
      ? { pages: [options.initialPage], pageParams: [0] }
      : undefined,
    initialDataUpdatedAt: options?.fetchedAt,
    staleTime: QUERY_STALE_TIME.SHORT,
  });
}
