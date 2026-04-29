import type { MetadataRoute } from "next";
import { env } from "@/config/env";
import { brawlerApi } from "@/api/endpoints/brawler";
import { eventApi } from "@/api/endpoints/event";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const base = env.NEXT_PUBLIC_SITE_URL;
  const now = new Date();

  let brawlerEntries: MetadataRoute.Sitemap = [];
  let rankingBrawlerEntries: MetadataRoute.Sitemap = [];
  try {
    const list = await brawlerApi.list({ next: { revalidate: 86400 } });
    brawlerEntries = list.content.map((b) => ({
      url: `${base}/brawlers/${b.id}`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.5,
    }));
    rankingBrawlerEntries = list.content.map((b) => ({
      url: `${base}/ranking/brawlers/${b.id}`,
      lastModified: now,
      changeFrequency: "hourly",
      priority: 0.6,
    }));
  } catch {
    brawlerEntries = [];
    rankingBrawlerEntries = [];
  }

  let eventEntries: MetadataRoute.Sitemap = [];
  try {
    const [rotation, soloRank] = await Promise.all([
      eventApi.rotation({ next: { revalidate: 3600 } }),
      eventApi.soloRank({ next: { revalidate: 3600 } }),
    ]);
    const ids = new Set<number>();
    for (const r of rotation.content) ids.add(r.event.id);
    for (const e of soloRank.content) ids.add(e.id);
    eventEntries = [...ids].map((id) => ({
      url: `${base}/events/${id}`,
      lastModified: now,
      changeFrequency: "daily",
      priority: 0.5,
    }));
  } catch {
    eventEntries = [];
  }

  return [
    { url: `${base}/`, lastModified: now, changeFrequency: "daily", priority: 1.0 },
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/ranking/players`, lastModified: now, changeFrequency: "hourly", priority: 0.7 },
    { url: `${base}/ranking/clubs`, lastModified: now, changeFrequency: "hourly", priority: 0.7 },
    { url: `${base}/ranking/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.6 },
    { url: `${base}/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.7 },
    { url: `${base}/events`, lastModified: now, changeFrequency: "daily", priority: 0.7 },
    ...brawlerEntries,
    ...rankingBrawlerEntries,
    ...eventEntries,
  ];
}
