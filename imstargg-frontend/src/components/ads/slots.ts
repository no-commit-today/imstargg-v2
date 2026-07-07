/**
 * AdSense 수동 광고 유닛 슬롯 ID.
 *
 * 빈 문자열이면 해당 위치의 AdUnit 은 아무것도 렌더링하지 않는다 —
 * AdSense 콘솔에서 슬롯 생성 후 값을 채워 배포한다 (운영자 액션).
 */
export const AD_SLOTS = {
  home: "",
  playerRanking: "",
  clubRanking: "",
  brawlerMeta: "",
  brawlerDetail: "",
  events: "",
  eventDetail: "",
  playerDetail: "",
  clubDetail: "",
} as const;
