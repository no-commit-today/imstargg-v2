"use client";
import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";
import type { FormEvent } from "react";
import { Menu, Search, X } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { formatMinutesAgo } from "@/lib/format";

const RECENT_KEY = "imstargg.recentSearches";
const RECENT_MAX = 8;

interface RecentSearch {
  tag: string;
  ts: number;
}

interface NavItem {
  href: string;
  label: string;
}

const NAV_ITEMS: NavItem[] = [
  { href: "/brawlers", label: "브롤러" },
  { href: "/events", label: "이벤트" },
  { href: "/ranking", label: "랭킹" },
  { href: "/clubs", label: "클럽" },
];

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

interface TopNavSearchProps {
  className?: string;
  emptyVariant?: "compact" | "desktop";
  now: number;
  onSubmitTag: (tag: string) => void;
  recents: RecentSearch[];
  search: string;
  setSearch: (value: string) => void;
  showRecents: boolean;
  setShowRecents: (value: boolean) => void;
}

const TopNavSearch = forwardRef<HTMLDivElement, TopNavSearchProps>(
  function TopNavSearch(
    {
      className = "",
      emptyVariant = "desktop",
      now,
      onSubmitTag,
      recents,
      search,
      setSearch,
      showRecents,
      setShowRecents,
    },
    ref,
  ) {
    const q = search.trim().replace(/^#/, "").toUpperCase();
    const filteredRecents = q
      ? recents.filter((r) => r.tag.includes(q))
      : recents;

    const submit = (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      onSubmitTag(search);
    };

    return (
      <div ref={ref} className={`topnav-search-wrap ${className}`}>
        <form className="topnav-search" onSubmit={submit}>
          <Search aria-hidden="true" />
          <input
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setShowRecents(true);
            }}
            onFocus={() => setShowRecents(true)}
            placeholder="#PLAYERTAG"
          />
          <button
            type="submit"
            className="topnav-search-submit"
            aria-label="플레이어 검색"
          >
            <Search aria-hidden="true" />
          </button>
        </form>

        {showRecents && filteredRecents.length > 0 && (
          <div className="topnav-recents">
            <div className="topnav-recents-head">
              <span>최근 검색</span>
              <button
                type="button"
                onClick={() => {
                  clearRecents();
                  notifyRecents();
                }}
                title="전체 삭제"
              >
                전체 삭제
              </button>
            </div>
            {filteredRecents.map((r) => (
              <div key={r.tag} className="topnav-recent-row">
                <button
                  type="button"
                  className="topnav-recent-main"
                  onClick={() => onSubmitTag(r.tag)}
                >
                  <Search aria-hidden="true" />
                  <span>#{r.tag}</span>
                  <time>
                    {now
                      ? formatMinutesAgo(
                          Math.max(0, Math.floor((now - r.ts) / 60000)),
                        )
                      : ""}
                  </time>
                </button>
                <button
                  type="button"
                  className="topnav-recent-delete"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeRecent(r.tag);
                    notifyRecents();
                  }}
                  title="삭제"
                  aria-label={`${r.tag} 삭제`}
                >
                  <X aria-hidden="true" />
                </button>
              </div>
            ))}
          </div>
        )}

        {showRecents && filteredRecents.length === 0 && (
          <div className="topnav-recents topnav-recents-empty">
            {q || emptyVariant === "compact" ? (
              "일치하는 최근 검색이 없어요."
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
    );
  },
);

export default function TopNav() {
  const router = useRouter();
  const pathname = usePathname() || "/";
  const [search, setSearch] = useState("");
  const [desktopRecentsOpen, setDesktopRecentsOpen] = useState(false);
  const [drawerSearch, setDrawerSearch] = useState("");
  const [drawerRecentsOpen, setDrawerRecentsOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const desktopSearchRef = useRef<HTMLDivElement>(null);

  useSyncExternalStore(
    subscribeRecentsStore,
    getRecentsSnapshot,
    getRecentsServerSnapshot,
  );
  const recents: RecentSearch[] =
    typeof window === "undefined" ? [] : loadRecents();
  const [now] = useState<number>(() =>
    typeof window === "undefined" ? 0 : Date.now(),
  );

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
    if (!desktopRecentsOpen) return;
    const onDoc = (e: MouseEvent) => {
      if (
        desktopSearchRef.current &&
        !desktopSearchRef.current.contains(e.target as Node)
      ) {
        setDesktopRecentsOpen(false);
      }
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, [desktopRecentsOpen]);

  const goToTag = useCallback(
    (tag: string, source: "desktop" | "drawer" = "desktop") => {
      const cleaned = tag.trim().replace(/^#/, "");
      if (!cleaned) return;
      pushRecent(cleaned);
      notifyRecents();
      router.push(`/players/${encodeURIComponent(cleaned)}`);
      if (source === "drawer") {
        setDrawerSearch("");
        setDrawerRecentsOpen(false);
        setDrawerOpen(false);
      } else {
        setSearch("");
        setDesktopRecentsOpen(false);
      }
    },
    [router],
  );

  const navigate = (href: string) => {
    router.push(href);
    setDrawerOpen(false);
  };

  const isActive = (prefix: string) =>
    pathname === prefix || pathname.startsWith(prefix + "/");

  return (
    <nav className="topnav">
      <div className="topnav-inner">
        <button
          type="button"
          className="topnav-logo"
          onClick={() => router.push("/")}
          title="ImStarGG"
        >
          <img src="/assets/logo.webp" alt="ImStarGG" />
        </button>
        <div className="topnav-links">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.href}
              type="button"
              className={`topnav-link ${isActive(item.href) ? "active" : ""}`}
              onClick={() => router.push(item.href)}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="topnav-spacer" />

        <TopNavSearch
          ref={desktopSearchRef}
          className="topnav-desktop-search"
          now={now}
          onSubmitTag={(tag) => goToTag(tag, "desktop")}
          recents={recents}
          search={search}
          setSearch={setSearch}
          showRecents={desktopRecentsOpen}
          setShowRecents={setDesktopRecentsOpen}
        />

        <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
          <button
            type="button"
            className="topnav-menu-button"
            aria-label="모바일 메뉴 열기"
            onClick={() => setDrawerOpen(true)}
          >
            <Menu aria-hidden="true" />
          </button>
          <SheetContent
            side="right"
            className="topnav-drawer"
            showCloseButton={false}
          >
            <SheetHeader className="topnav-drawer-header">
              <div>
                <SheetTitle>메뉴</SheetTitle>
                <SheetDescription className="sr-only">
                  주요 페이지와 플레이어 검색
                </SheetDescription>
              </div>
              <button
                type="button"
                className="topnav-drawer-close"
                aria-label="모바일 메뉴 닫기"
                onClick={() => setDrawerOpen(false)}
              >
                <X aria-hidden="true" />
              </button>
            </SheetHeader>
            <TopNavSearch
              className="topnav-search-drawer"
              emptyVariant="compact"
              now={now}
              onSubmitTag={(tag) => goToTag(tag, "drawer")}
              recents={recents}
              search={drawerSearch}
              setSearch={setDrawerSearch}
              showRecents={drawerRecentsOpen}
              setShowRecents={setDrawerRecentsOpen}
            />
            <div className="topnav-drawer-links">
              {NAV_ITEMS.map((item) => (
                <button
                  key={item.href}
                  type="button"
                  className={isActive(item.href) ? "active" : ""}
                  aria-label={`${item.label} 페이지로 이동`}
                  onClick={() => navigate(item.href)}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </nav>
  );
}
