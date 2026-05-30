import Script from "next/script";

export const GOOGLE_ADSENSE_CLIENT = "ca-pub-5084387143997125";
export const GOOGLE_ADSENSE_SCRIPT_SRC = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${GOOGLE_ADSENSE_CLIENT}`;

export function shouldLoadGoogleAdsense(nodeEnv: string | undefined): boolean {
  return nodeEnv === "production";
}

export function GoogleAdsenseScript() {
  if (!shouldLoadGoogleAdsense(process.env.NODE_ENV)) return null;

  return (
    <Script
      id="google-adsense"
      async
      strategy="afterInteractive"
      src={GOOGLE_ADSENSE_SCRIPT_SRC}
      crossOrigin="anonymous"
    />
  );
}
