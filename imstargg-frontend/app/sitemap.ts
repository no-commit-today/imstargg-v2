import type { MetadataRoute } from "next";
import { env } from "@/config/env";
import { brawlerApi } from "@/api/endpoints/brawler";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const base = env.NEXT_PUBLIC_SITE_URL;
  const now = new Date();

  let brawlerEntries: MetadataRoute.Sitemap = [];
  try {
    const list = await brawlerApi.list({ next: { revalidate: 86400 } });
    brawlerEntries = list.content.map((b) => ({
      url: `${base}/brawlers/${b.id}`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.5,
    }));
  } catch {
    brawlerEntries = [];
  }

  return [
    { url: `${base}/`, lastModified: now, changeFrequency: "daily", priority: 1.0 },
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.7 },
    { url: `${base}/events`, lastModified: now, changeFrequency: "daily", priority: 0.7 },
    ...brawlerEntries,
  ];
}
