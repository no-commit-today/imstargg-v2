const KNOWN: Record<string, string> = {
  GADGET: "가젯",
  HYPER_CHARGE: "하이퍼차지",
  STAR_POWER: "스타파워",
  MYTHIC_GEAR: "신화 기어",
};

export function buffyTypeLabel(buffyType: string): string {
  return KNOWN[buffyType] ?? buffyType;
}
