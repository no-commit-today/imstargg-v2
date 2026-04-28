export const COUNTRY_CODES = ["global", "kr", "cn", "jp", "de", "us"] as const;
export type Country = (typeof COUNTRY_CODES)[number];

export const COUNTRY_LABELS: Record<Country, string> = {
  global: "글로벌",
  kr: "한국",
  cn: "중국",
  jp: "일본",
  de: "독일",
  us: "미국",
};

export function parseCountry(value: unknown): Country {
  if (typeof value !== "string") return "global";
  const normalized = value.trim().toLowerCase();
  return (COUNTRY_CODES as readonly string[]).includes(normalized)
    ? (normalized as Country)
    : "global";
}
