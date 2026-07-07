/**
 * AdSense 설정 상수. 광고 게재는 AdUnit(수동 유닛)을 통해서만 한다 —
 * 전역 스크립트 로드(자동 광고)는 빈 화면 광고 게재로 이어져 정책 위반이었음.
 */
export const GOOGLE_ADSENSE_CLIENT = "ca-pub-5084387143997125";
export const GOOGLE_ADSENSE_SCRIPT_SRC = `https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${GOOGLE_ADSENSE_CLIENT}`;

export function shouldLoadGoogleAdsense(nodeEnv: string | undefined): boolean {
  return nodeEnv === "production";
}
