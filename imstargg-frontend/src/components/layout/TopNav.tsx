"use client";
import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useRouter, usePathname } from "next/navigation";
import { formatMinutesAgo } from "@/lib/format";

const RECENT_KEY = "imstargg.recentSearches";
const RECENT_MAX = 8;

interface RecentSearch {
  tag: string;
  ts: number;
}

function loadRecents(): RecentSearch[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(RECENT_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}
function saveRecents(list: RecentSearch[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(list));
  } catch {}
}
function pushRecent(tag: string) {
  const cleaned = tag.trim().replace(/^#/, "").toUpperCase();
  if (!cleaned) return;
  const list = loadRecents().filter((r) => r.tag !== cleaned);
  list.unshift({ tag: cleaned, ts: Date.now() });
  saveRecents(list.slice(0, RECENT_MAX));
}
function removeRecent(tag: string) {
  saveRecents(loadRecents().filter((r) => r.tag !== tag));
}
function clearRecents() {
  saveRecents([]);
}

// External store wiring for recents — lets useSyncExternalStore drive renders
// without setState-in-effect. We bump a version number on writes.
const recentsListeners = new Set<() => void>();
let recentsVersion = 0;
function notifyRecents() {
  recentsVersion++;
  recentsListeners.forEach((fn) => fn());
}
function subscribeRecentsStore(fn: () => void) {
  recentsListeners.add(fn);
  return () => {
    recentsListeners.delete(fn);
  };
}
function getRecentsSnapshot(): number {
  return recentsVersion;
}
function getRecentsServerSnapshot(): number {
  return 0;
}

const syncedPathnames = new Set<string>();

export default function TopNav() {
  const router = useRouter();
  const pathname = usePathname() || "/";
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  // Subscribe to the recents external store (version-keyed); read fresh list each render.
  useSyncExternalStore(subscribeRecentsStore, getRecentsSnapshot, getRecentsServerSnapshot);
  const recents: RecentSearch[] = typeof window === "undefined" ? [] : loadRecents();

  // "now" — lazy initialized once on the client for relative-time labels.
  const [now] = useState<number>(() => (typeof window === "undefined" ? 0 : Date.now()));

  // Sync pathname-driven recent push without triggering a setState.
  // Module-level dedup so the same path isn't pushed twice on re-renders.
  if (
    pathname.startsWith("/players/") &&
    !syncedPathnames.has(pathname) &&
    typeof window !== "undefined"
  ) {
    syncedPathnames.add(pathname);
    const tag = decodeURIComponent(pathname.slice("/players/".length));
    pushRecent(tag);
    notifyRecents();
  }

  useEffect(() => {
    if (!open) return;
    const onDoc = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [open]);

  const goToTag = useCallback(
    (tag: string) => {
      const cleaned = tag.trim().replace(/^#/, "");
      if (!cleaned) return;
      pushRecent(cleaned);
      notifyRecents();
      router.push(`/players/${encodeURIComponent(cleaned)}`);
      setSearch("");
      setOpen(false);
    },
    [router],
  );

  const submit = (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    goToTag(search);
  };

  const isActive = (prefix: string) => pathname === prefix || pathname.startsWith(prefix + "/");

  const q = search.trim().replace(/^#/, "").toUpperCase();
  const filteredRecents = q ? recents.filter((r) => r.tag.includes(q)) : recents;

  return (
    <nav className="topnav">
      <div className="topnav-inner">
        <a
          className="topnav-logo"
          onClick={() => router.push("/")}
          style={{ cursor: "pointer" }}
          title="ImStarGG"
        >
          <img src="/assets/logo.png" alt="ImStarGG" style={{ height: 32 }} />
        </a>
        <div className="topnav-links">
          <a
            className={`topnav-link ${isActive("/brawlers") ? "active" : ""}`}
            onClick={() => router.push("/brawlers")}
            style={{ cursor: "pointer" }}
          >
            브롤러
          </a>
          <a
            className={`topnav-link ${isActive("/events") ? "active" : ""}`}
            onClick={() => router.push("/events")}
            style={{ cursor: "pointer" }}
          >
            이벤트
          </a>
          <a
            className={`topnav-link ${isActive("/ranking") ? "active" : ""}`}
            onClick={() => router.push("/ranking")}
            style={{ cursor: "pointer" }}
          >
            랭킹
          </a>
          <a
            className={`topnav-link ${isActive("/clubs") ? "active" : ""}`}
            onClick={() => router.push("/clubs")}
            style={{ cursor: "pointer" }}
          >
            클럽
          </a>
        </div>
        <div className="topnav-spacer" />

        <div ref={wrapRef} style={{ position: "relative" }}>
          <form className="topnav-search" onSubmit={submit}>
            <span style={{ color: "#8A90A0", fontSize: 13 }}>🔍</span>
            <input
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setOpen(true);
              }}
              onFocus={() => setOpen(true)}
              placeholder="#PLAYERTAG"
            />
          </form>

          {open && filteredRecents.length > 0 && (
            <div
              style={{
                position: "absolute",
                top: "calc(100% + 6px)",
                right: 0,
                width: 280,
                background: "#fff",
                border: "1px solid var(--color-neutral-200)",
                borderRadius: 10,
                boxShadow: "0 8px 28px rgba(20,24,32,0.12), 0 2px 6px rgba(20,24,32,0.06)",
                overflow: "hidden",
                zIndex: 100,
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "8px 12px",
                  fontSize: 11,
                  fontWeight: 700,
                  color: "var(--color-neutral-500)",
                  letterSpacing: "0.06em",
                  textTransform: "uppercase",
                  borderBottom: "1px solid var(--color-neutral-100)",
                  background: "var(--color-neutral-50)",
                }}
              >
                <span>최근 검색</span>
                <button
                  onClick={() => {
                    clearRecents();
                    notifyRecents();
                  }}
                  style={{
                    background: "transparent",
                    border: "none",
                    padding: 0,
                    color: "var(--color-neutral-500)",
                    fontSize: 11,
                    fontWeight: 600,
                    cursor: "pointer",
                    letterSpacing: 0,
                    textTransform: "none",
                  }}
                  title="전체 삭제"
                >
                  전체 삭제
                </button>
              </div>
              {filteredRecents.map((r) => (
                <div
                  key={r.tag}
                  onClick={() => goToTag(r.tag)}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    padding: "10px 12px",
                    cursor: "pointer",
                    borderBottom: "1px solid var(--color-neutral-100)",
                  }}
                  onMouseEnter={(e) =>
                    (e.currentTarget.style.background = "var(--color-neutral-50)")
                  }
                  onMouseLeave={(e) => (e.currentTarget.style.background = "#fff")}
                >
                  <span style={{ color: "#8A90A0", fontSize: 14 }}>🕘</span>
                  <span
                    style={{
                      flex: 1,
                      fontFamily: "var(--font-num)",
                      fontWeight: 700,
                      fontSize: 13,
                      color: "var(--color-neutral-800)",
                      letterSpacing: "0.02em",
                    }}
                  >
                    #{r.tag}
                  </span>
                  <span style={{ fontSize: 11, color: "var(--color-neutral-400)" }}>
                    {now ? formatMinutesAgo(Math.max(0, Math.floor((now - r.ts) / 60000))) : ""}
                  </span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      removeRecent(r.tag);
                      notifyRecents();
                    }}
                    style={{
                      background: "transparent",
                      border: "none",
                      padding: 4,
                      color: "var(--color-neutral-400)",
                      cursor: "pointer",
                      fontSize: 14,
                      lineHeight: 1,
                      borderRadius: 4,
                    }}
                    title="삭제"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}

          {open && filteredRecents.length === 0 && (
            <div
              style={{
                position: "absolute",
                top: "calc(100% + 6px)",
                right: 0,
                width: 280,
                background: "#fff",
                border: "1px solid var(--color-neutral-200)",
                borderRadius: 10,
                boxShadow: "0 8px 28px rgba(20,24,32,0.12)",
                padding: "18px 14px",
                fontSize: 12,
                color: "var(--color-neutral-500)",
                textAlign: "center",
                zIndex: 100,
              }}
            >
              {q ? (
                <>일치하는 최근 검색이 없어요.</>
              ) : (
                <>
                  아직 검색한 플레이어가 없어요.
                  <br />
                  상단에 #태그를 입력해 검색해보세요.
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
