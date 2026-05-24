export interface ModeStyle {
  key: string;
  gradient: string;
}

const DEFAULT_STYLE: ModeStyle = {
  key: "default",
  gradient: "linear-gradient(135deg, #2A2E38 0%, #14161B 100%)",
};

const MAP: Record<string, ModeStyle> = {
  젬그랩: { key: "gemGrab", gradient: "linear-gradient(135deg, #9146FF 0%, #6A28D9 100%)" },
  브롤볼: { key: "brawlBall", gradient: "linear-gradient(135deg, #3DE0FF 0%, #2E7CF6 100%)" },
  하이스트: { key: "heist", gradient: "linear-gradient(135deg, #FFC60A 0%, #FF8A1F 100%)" },
  핫존: { key: "hotZone", gradient: "linear-gradient(135deg, #FF8A1F 0%, #FF3A3A 100%)" },
  녹아웃: { key: "knockout", gradient: "linear-gradient(135deg, #FF3A3A 0%, #C9141A 100%)" },
  현상금사냥: { key: "bounty", gradient: "linear-gradient(135deg, #FFD447 0%, #E8A900 100%)" },
  솔로쇼다운: { key: "soloShowdown", gradient: "linear-gradient(135deg, #2BD576 0%, #0E6E3C 100%)" },
  듀오쇼다운: { key: "duoShowdown", gradient: "linear-gradient(135deg, #2BD576 0%, #1D4FB3 100%)" },
  듀얼: { key: "duels", gradient: "linear-gradient(135deg, #FF5DAA 0%, #9146FF 100%)" },
  전멸전: { key: "wipeout", gradient: "linear-gradient(135deg, #FF3A3A 0%, #1E2128 100%)" },
  발리브롤: { key: "volleyBrawl", gradient: "linear-gradient(135deg, #3DE0FF 0%, #2BD576 100%)" },
  공성전: { key: "siege", gradient: "linear-gradient(135deg, #8A90A0 0%, #2A2E38 100%)" },
  바스켓브롤: { key: "basketBrawl", gradient: "linear-gradient(135deg, #FF8A1F 0%, #C9141A 100%)" },
  트로피쟁탈전: { key: "trophyThieves", gradient: "linear-gradient(135deg, #FFC60A 0%, #FF5DAA 100%)" },
  보스전: { key: "bossFight", gradient: "linear-gradient(135deg, #C9141A 0%, #0B0C10 100%)" },
  빅게임: { key: "bigGame", gradient: "linear-gradient(135deg, #B83BFF 0%, #9146FF 100%)" },
  로보럼블: { key: "roboRumble", gradient: "linear-gradient(135deg, #58D7E8 0%, #2A2E38 100%)" },
};

function normalize(name: string): string {
  return name.replace(/\s+/g, "").trim();
}

export function getModeStyle(name: string | null | undefined): ModeStyle {
  if (!name) return DEFAULT_STYLE;
  return MAP[normalize(name)] ?? DEFAULT_STYLE;
}
