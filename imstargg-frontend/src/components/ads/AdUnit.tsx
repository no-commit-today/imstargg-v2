"use client";

import { useEffect, useRef } from "react";
import Script from "next/script";
import {
  GOOGLE_ADSENSE_CLIENT,
  GOOGLE_ADSENSE_SCRIPT_SRC,
  shouldLoadGoogleAdsense,
} from "./GoogleAdsenseScript";

declare global {
  interface Window {
    adsbygoogle?: object[];
  }
}

type Props = {
  slot: string;
  className?: string;
  /** 테스트 주입용. 기본값은 production 에서만 true. */
  enabled?: boolean;
};

/**
 * AdSense 수동 광고 유닛. 광고는 이 컴포넌트를 통해서만 게재된다 —
 * 콘텐츠가 없는 화면(미갱신 플레이어/클럽, 404, 에러, 빈 검색, 법적 페이지)에는
 * 절대 배치하지 않는다 (AdSense "게시자 콘텐츠 없는 화면" 정책).
 * slot 이 빈 문자열이면 아무것도 렌더링하지 않으므로 콘솔 슬롯 발급 전에도 배포 가능.
 */
export function AdUnit({
  slot,
  className,
  enabled = shouldLoadGoogleAdsense(process.env.NODE_ENV),
}: Props) {
  const active = enabled && slot !== "";
  const pushed = useRef(false);

  useEffect(() => {
    if (!active || pushed.current) return;
    try {
      (window.adsbygoogle = window.adsbygoogle ?? []).push({});
      pushed.current = true;
    } catch {
      // 광고 스크립트 로드 실패는 광고 미표시로 끝난다 — 페이지 동작에 영향 없음
    }
  }, [active]);

  if (!active) return null;
  return (
    <div className={className}>
      <Script
        id="google-adsense"
        async
        strategy="afterInteractive"
        src={GOOGLE_ADSENSE_SCRIPT_SRC}
        crossOrigin="anonymous"
      />
      <ins
        className="adsbygoogle"
        style={{ display: "block" }}
        data-ad-client={GOOGLE_ADSENSE_CLIENT}
        data-ad-slot={slot}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </div>
  );
}
