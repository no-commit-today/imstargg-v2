export const COUNTRY_CODES = ["global", "kr", "cn", "jp", "de", "us"] as const;
export type Country = (typeof COUNTRY_CODES)[number];

export interface CountryMeta {
  flag: string;
  label: string;
  short: string;
}

export const COUNTRY_META: Record<Country, CountryMeta> = {
  global: { flag: "🌐", label: "글로벌", short: "GLOBAL" },
  kr: { flag: "🇰🇷", label: "한국", short: "KR" },
  cn: { flag: "🇨🇳", label: "중국", short: "CN" },
  jp: { flag: "🇯🇵", label: "일본", short: "JP" },
  de: { flag: "🇩🇪", label: "독일", short: "DE" },
  us: { flag: "🇺🇸", label: "미국", short: "US" },
};

export const COUNTRY_LABELS: Record<Country, string> = Object.fromEntries(
  COUNTRY_CODES.map((c) => [c, COUNTRY_META[c].label]),
) as Record<Country, string>;

export function parseCountry(value: unknown): Country {
  if (typeof value !== "string") return "global";
  const normalized = value.trim().toLowerCase();
  return (COUNTRY_CODES as readonly string[]).includes(normalized)
    ? (normalized as Country)
    : "global";
}
