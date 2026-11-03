export interface ModeStyle {
  /** 단색 배경 (hex) */
  bg: string;
  /** 배경 대비 텍스트/아이콘 색 */
  fg: string;
}

/** hex 배경의 상대 명도로 흰색/짙은색 텍스트를 고른다. */
function pickFg(hex: string): string {
  const m = hex.replace("#", "");
  const r = parseInt(m.slice(0, 2), 16);
  const g = parseInt(m.slice(2, 4), 16);
  const b = parseInt(m.slice(4, 6), 16);
  // 상대 휘도 (sRGB 근사)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.6 ? "#14161B" : "#FFFFFF";
}

/**
 * 모드(modeId) → 이벤트 카드 배경색. 통계를 모으는 모드만 둔다.
 * 백엔드 `StatsEventMode` 에 모드를 추가할 때 여기에도 함께 추가한다.
 * 아레나(48)는 색이 정해지지 않아 넣지 않았다(기본 회색). 색을 정하면 한 줄 추가한다.
 */
const MODE_COLORS: Record<number, string> = {
  0: "#8F43EA", // 젬그랩
  2: "#C763CD", // 하이스트
  3: "#FFC60A", // 바운티
  5: "#909FDB", // 브롤볼
  17: "#FF8A1F", // 핫존
  20: "#E8893A", // 녹아웃
  22: "#62C1F4", // 농구
  45: "#D2D2FB", // 하키
  6: "#95D449", // 솔로 쇼다운
  9: "#95D449", // 듀오 쇼다운
  38: "#95D449", // 트리오 쇼다운
};

/** 색이 정해지지 않은 모드(mode=null, 통계 미지원 모드 등)의 폴백 회색. */
const DEFAULT_STYLE: ModeStyle = { bg: "#2A2E38", fg: "#FFFFFF" };

/**
 * 모드 id(`mode.id`)로 카드 배경/텍스트 스타일을 만든다. 배경색은 `MODE_COLORS` 에서 찾고,
 * 명도로 텍스트색만 보강한다.
 * 젬그랩의 modeId 는 0 이므로 존재 검사에 truthiness 를 쓰지 않는다.
 */
export function getModeStyle(modeId: number | null | undefined): ModeStyle {
  if (modeId == null) return DEFAULT_STYLE;
  const color: string | undefined = MODE_COLORS[modeId];
  if (color === undefined) return DEFAULT_STYLE;
  return { bg: color, fg: pickFg(color) };
}
