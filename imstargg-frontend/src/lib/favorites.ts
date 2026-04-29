import { normalizeTag } from "@/lib/tag";
import type { SearchEntry, SearchEntryKind } from "@/lib/searchHistory";

const FAVORITES_KEY = "imstargg.search.favorites";
export const FAVORITES_LIMIT = 50;

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readArray(): SearchEntry[] {
  if (!isBrowser()) return [];
  const raw = window.localStorage.getItem(FAVORITES_KEY);
  if (raw == null) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed as SearchEntry[];
    window.localStorage.removeItem(FAVORITES_KEY);
    return [];
  } catch {
    window.localStorage.removeItem(FAVORITES_KEY);
    return [];
  }
}

function writeArray(entries: SearchEntry[]): void {
  if (!isBrowser()) return;
  try {
    window.localStorage.setItem(FAVORITES_KEY, JSON.stringify(entries));
  } catch {
    // QuotaExceededError or SecurityError — silently drop the write
  }
}

export function getFavorites(): SearchEntry[] {
  return readArray();
}

export function isFavorite(kind: SearchEntryKind, tag: string): boolean {
  const norm = normalizeTag(tag);
  return getFavorites().some((e) => e.kind === kind && e.tag === norm);
}

export function toggleFavorite(input: Omit<SearchEntry, "addedAt">): boolean {
  if (!isBrowser()) return false;
  const tag = normalizeTag(input.tag);
  const list = getFavorites();
  const idx = list.findIndex((e) => e.kind === input.kind && e.tag === tag);
  if (idx >= 0) {
    list.splice(idx, 1);
    writeArray(list);
    return false;
  }
  list.unshift({ ...input, tag, addedAt: Date.now() });
  const trimmed = list.slice(0, FAVORITES_LIMIT);
  writeArray(trimmed);
  return true;
}
