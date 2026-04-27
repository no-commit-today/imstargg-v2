import type { Metadata } from "next";
import { env } from "@/config/env";

const SITE = {
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  defaultOgImage: "/assets/logo-full.png",
  locale: "ko_KR",
} as const;

export const SITE_INFO = SITE;

type Input = {
  title: string;
  description: string;
  path: string;
  image?: string;
  noindex?: boolean;
};

export function buildMetadata(input: Input): Metadata {
  const url = `${SITE.url}${input.path}`;
  const image = input.image ?? SITE.defaultOgImage;
  return {
    title: input.title,
    description: input.description,
    alternates: { canonical: url },
    robots: input.noindex ? { index: false, follow: false } : undefined,
    openGraph: {
      title: input.title,
      description: input.description,
      url,
      siteName: SITE.name,
      images: [{ url: image, width: 1200, height: 630 }],
      locale: SITE.locale,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: input.title,
      description: input.description,
      images: [image],
    },
  };
}
