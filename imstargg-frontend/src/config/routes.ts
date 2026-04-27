import { toUrlPath } from "@/lib/tag";

export const routes = {
  home: () => "/",
  ranking: () => "/ranking",
  brawlers: () => "/brawlers",
  brawler: (id: string | number) => `/brawlers/${id}`,
  events: () => "/events",
  event: (id: string | number) => `/events/${id}`,
  player: (tag: string) => `/players/${toUrlPath(tag)}`,
  club: (tag: string) => `/clubs/${toUrlPath(tag)}`,
} as const;
