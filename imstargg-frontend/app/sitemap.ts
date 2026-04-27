import type { MetadataRoute } from "next";
import { env } from "@/config/env";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const base = env.NEXT_PUBLIC_SITE_URL;
  const now = new Date();
  return [
    { url: `${base}/`, lastModified: now, changeFrequency: "daily", priority: 1.0 },
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.7 },
    { url: `${base}/events`, lastModified: now, changeFrequency: "daily", priority: 0.7 },
  ];
}
