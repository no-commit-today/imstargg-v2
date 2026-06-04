export interface ModeStyle {
  key: string;
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

function style(key: string, bg: string): ModeStyle {
  return { key, bg, fg: pickFg(bg) };
}

const DEFAULT_STYLE: ModeStyle = style("default", "#2A2E38");

// 이슈 #49 지정 색상(단색). 미지정 모드는 기존 톤의 대표색을 단색화.
const MAP: Record<string, ModeStyle> = {
  젬그랩: style("gemGrab", "#8F43EA"), // 143,67,234
  브롤볼: style("brawlBall", "#909FDB"), // 144,159,219
  하이스트: style("heist", "#C763CD"), // 199,99,205
  녹아웃: style("knockout", "#E8893A"), // 232,137,58
  솔로쇼다운: style("soloShowdown", "#95D449"), // 149,212,73
  듀오쇼다운: style("duoShowdown", "#95D449"), // 149,212,73
  바스켓브롤: style("basketBrawl", "#62C1F4"), // 98,193,244
  브롤하키: style("brawlHockey", "#D2D2FB"), // 210,210,251
  // --- 이슈 미지정: 기존 대표색 단색화 ---
  핫존: style("hotZone", "#FF8A1F"),
  바운티: style("bounty", "#FFC60A"),
  듀얼: style("duels", "#FF5DAA"),
  전멸전: style("wipeout", "#FF3A3A"),
  발리브롤: style("volleyBrawl", "#3DE0FF"),
  공성전: style("siege", "#5C6271"),
  트로피쟁탈전: style("trophyThieves", "#FFC60A"),
  보스전: style("bossFight", "#C9141A"),
  빅게임: style("bigGame", "#B83BFF"),
  로보럼블: style("roboRumble", "#58D7E8"),
};

function normalize(name: string): string {
  return name.replace(/\s+/g, "").trim();
}

export function getModeStyle(name: string | null | undefined): ModeStyle {
  if (!name) return DEFAULT_STYLE;
  return MAP[normalize(name)] ?? DEFAULT_STYLE;
}
