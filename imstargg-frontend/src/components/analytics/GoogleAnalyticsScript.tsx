import { GoogleAnalytics } from "@next/third-parties/google";

export const GOOGLE_ANALYTICS_ID = "G-SWX7PCDRXW";

export function shouldLoadGoogleAnalytics(nodeEnv: string | undefined): boolean {
  return nodeEnv === "production";
}

export function GoogleAnalyticsScript() {
  if (!shouldLoadGoogleAnalytics(process.env.NODE_ENV)) return null;

  return <GoogleAnalytics gaId={GOOGLE_ANALYTICS_ID} />;
}
