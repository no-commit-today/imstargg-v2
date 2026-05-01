import type { Metadata } from "next";
import { env } from "@/config/env";

const SITE = {
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  defaultOgImage: "/assets/logo-full.webp",
  locale: "ko_KR",
} as const;

export const SITE_INFO = SITE;

type Input = {
  title: string;
  description: string;
  path: string;
  image?: string;
  noindex?: boolean;
  dynamicOg?: boolean;
};

export function buildMetadata(input: Input): Metadata {
  const url = `${SITE.url}${input.path}`;
  const meta: Metadata = {
    title: input.title,
    description: input.description,
    alternates: { canonical: url },
    robots: input.noindex ? { index: false, follow: false } : undefined,
    openGraph: {
      title: input.title,
      description: input.description,
      url,
      siteName: SITE.name,
      locale: SITE.locale,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: input.title,
      description: input.description,
    },
  };

  if (!input.dynamicOg) {
    const image = input.image ?? SITE.defaultOgImage;
    meta.openGraph!.images = [{ url: image, width: 1200, height: 630 }];
    meta.twitter!.images = [image];
  }
  return meta;
}
