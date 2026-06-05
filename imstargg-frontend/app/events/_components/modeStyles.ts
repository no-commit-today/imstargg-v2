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

/** 색이 없는 이벤트(mode=null 등)의 폴백 회색. 백엔드 DEFAULT_COLOR(#2A2E38)와 일치. */
const DEFAULT_STYLE: ModeStyle = { bg: "#2A2E38", fg: "#FFFFFF" };

/**
 * 백엔드가 제공하는 모드 색(`mode.color`)으로 카드 배경/텍스트 스타일을 만든다.
 * 색 데이터의 단일 출처는 백엔드(`battle_event_mode.color`)이며, 여기서는 명도로 텍스트색만 보강한다.
 */
export function getModeStyle(color: string | null | undefined): ModeStyle {
  if (!color) return DEFAULT_STYLE;
  return { bg: color, fg: pickFg(color) };
}
