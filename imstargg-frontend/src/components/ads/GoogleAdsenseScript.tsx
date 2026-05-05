import Script from "next/script";
import { env } from "@/config/env";

export function normalizeGoogleAdsenseClient(
  client: string | undefined,
): string {
  return client?.trim() ?? "";
}

export function shouldLoadGoogleAdsense(
  nodeEnv: string | undefined,
  client: string | undefined,
): boolean {
  return nodeEnv === "production" && Boolean(client?.trim());
}

export function GoogleAdsenseScript() {
  const client = normalizeGoogleAdsenseClient(
    env.NEXT_PUBLIC_GOOGLE_ADSENSE_CLIENT,
  );
  if (!shouldLoadGoogleAdsense(process.env.NODE_ENV, client)) return null;

  return (
    <Script
      id="google-adsense"
      async
      strategy="afterInteractive"
      src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${client}`}
      crossOrigin="anonymous"
    />
  );
}
