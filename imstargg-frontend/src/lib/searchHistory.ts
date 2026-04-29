import { normalizeTag } from "@/lib/tag";

export type SearchEntryKind = "player" | "club";

export interface SearchEntry {
  kind: SearchEntryKind;
  tag: string;
  label?: string;
  addedAt: number;
}

const RECENTS_KEY = "imstargg.search.recents";
export const RECENTS_LIMIT = 10;

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readArray(key: string): SearchEntry[] {
  if (!isBrowser()) return [];
  const raw = window.localStorage.getItem(key);
  if (raw == null) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed as SearchEntry[];
    window.localStorage.removeItem(key);
    return [];
  } catch {
    window.localStorage.removeItem(key);
    return [];
  }
}

function writeArray(key: string, entries: SearchEntry[]): void {
  if (!isBrowser()) return;
  window.localStorage.setItem(key, JSON.stringify(entries));
}

export function getRecents(): SearchEntry[] {
  return readArray(RECENTS_KEY);
}

export function addRecent(input: Omit<SearchEntry, "addedAt">): void {
  if (!isBrowser()) return;
  const tag = normalizeTag(input.tag);
  const next: SearchEntry = { ...input, tag, addedAt: Date.now() };
  const filtered = getRecents().filter((e) => !(e.kind === next.kind && e.tag === next.tag));
  const merged = [next, ...filtered].slice(0, RECENTS_LIMIT);
  writeArray(RECENTS_KEY, merged);
}

export function clearRecents(): void {
  if (!isBrowser()) return;
  window.localStorage.removeItem(RECENTS_KEY);
}
