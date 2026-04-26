"use client";
import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  CLS,
  BRAWLER_META,
  fmtNum,
  type ClassKey,
  type BrawlerMeta,
} from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";
import ClassIcon from "@/components/primitives/ClassIcon";

const STATS_TIER_RANGES = [
  { key: "TROPHY_1000_PLUS", group: "트로피", label: "트로피 1000+" },
  { key: "TROPHY_2000_PLUS", group: "트로피", label: "트로피 2000+" },
  { key: "TROPHY_3000_PLUS", group: "트로피", label: "트로피 3000+" },
  { key: "SOLO_RANK_MYTHIC_PLUS", group: "경쟁전", label: "경쟁전 신화+" },
  { key: "SOLO_RANK_LEGENDARY_PLUS", group: "경쟁전", label: "경쟁전 레전드+" },
  { key: "SOLO_RANK_MASTER_PLUS", group: "경쟁전", label: "경쟁전 마스터+" },
];

const STATS_DATE_RANGES = [
  { key: "THIS_WEEK", label: "이번 주" },
  { key: "SINCE_LAST_WEEK", label: "지난 주부터" },
  { key: "LAST_WEEK", label: "지난 주" },
  { key: "SINCE_TWO_WEEKS_AGO", label: "지난 2주" },
  { key: "TWO_WEEKS_AGO", label: "2주 전" },
  { key: "THIS_MONTH", label: "이번 달" },
  { key: "SINCE_LAST_MONTH", label: "지난 달부터" },
  { key: "LAST_MONTH", label: "지난 달" },
];

type SortKey = "winRate" | "pickRate" | "starRate" | "name" | "tier";
interface SortState {
  key: SortKey;
  dir: "asc" | "desc";
}

const selectStyle: React.CSSProperties = {
  padding: "7px 10px",
  fontSize: 13,
  border: "1px solid var(--color-neutral-200)",
  borderRadius: 6,
  background: "#fff",
  cursor: "pointer",
  fontWeight: 700,
  color: "var(--color-neutral-800)",
  letterSpacing: "0.01em",
};

function FilterLabel({ children }: { children: React.ReactNode }) {
  return (
    <span
      style={{
        fontSize: 11,
        fontWeight: 700,
        color: "var(--color-neutral-500)",
        letterSpacing: "0.06em",
        textTransform: "uppercase",
        minWidth: 30,
      }}
    >
      {children}
    </span>
  );
}

function RoleTab({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        gap: 8,
        padding: 0,
        background: active
          ? "linear-gradient(180deg, rgba(46,124,246,0.18), rgba(46,124,246,0.06))"
          : "#fff",
        color: active ? "var(--color-accent-blue-deep)" : "var(--color-neutral-700)",
        border: "none",
        borderRight: "1px solid var(--color-neutral-100)",
        cursor: "pointer",
        position: "relative",
        transition: "background 0.1s",
      }}
      onMouseEnter={(e) => {
        if (!active) e.currentTarget.style.background = "var(--color-neutral-50)";
      }}
      onMouseLeave={(e) => {
        if (!active) e.currentTarget.style.background = "#fff";
      }}
    >
      {children}
      {active && (
        <div
          style={{
            position: "absolute",
            bottom: 0,
            left: 0,
            right: 0,
            height: 3,
            background: "var(--color-accent-blue)",
          }}
        />
      )}
    </button>
  );
}

function TierBadge({ tier }: { tier: BrawlerMeta["tier"] }) {
  const styles = {
    S: { bg: "linear-gradient(180deg, #FFD84D, #E8A800)" },
    A: { bg: "linear-gradient(180deg, #FFA858, #E87617)" },
    B: { bg: "linear-gradient(180deg, #5FA0FF, #2E7CF6)" },
    C: { bg: "linear-gradient(180deg, #B5BCC9, #8A90A0)" },
    D: { bg: "linear-gradient(180deg, #FF7575, #E63838)" },
  }[tier];
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: 26,
        height: 28,
        borderRadius: 4,
        background: styles.bg,
        color: "#fff",
        fontFamily: "var(--font-display)",
        fontWeight: 900,
        fontSize: 14,
        textShadow: "0 1px 1px rgba(0,0,0,0.3)",
        boxShadow: "inset 0 -2px 0 rgba(0,0,0,0.18)",
      }}
    >
      {tier}
    </span>
  );
}

function SortHeader({
  sort,
  sortKey,
  onClick,
  align = "right",
  children,
}: {
  sort: SortState;
  sortKey: SortKey;
  onClick: () => void;
  align?: "left" | "right" | "center";
  children: React.ReactNode;
}) {
  const active = sort.key === sortKey;
  const arrow = active ? (sort.dir === "desc" ? "▼" : "▲") : "";
  return (
    <button
      onClick={onClick}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 4,
        justifyContent:
          align === "right" ? "flex-end" : align === "center" ? "center" : "flex-start",
        background: "transparent",
        border: "none",
        padding: 0,
        fontSize: 12,
        fontWeight: 700,
        color: active ? "var(--color-accent-blue-deep)" : "var(--color-neutral-500)",
        letterSpacing: "0.04em",
        cursor: "pointer",
        width: "100%",
      }}
    >
      <span>{children}</span>
      <span style={{ fontSize: 9, width: 9, opacity: active ? 1 : 0.25 }}>{arrow || "▼"}</span>
    </button>
  );
}

const COLS = "50px 1fr 60px 90px 110px 110px 130px";

export default function BrawlersListPage() {
  const router = useRouter();
  const [classFilter, setClassFilter] = useState<ClassKey | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const [tierRange, setTierRange] = useState("SOLO_RANK_LEGENDARY_PLUS");
  const [dateRange, setDateRange] = useState("SINCE_TWO_WEEKS_AGO");
  const [sort, setSort] = useState<SortState>({ key: "winRate", dir: "desc" });

  const meta = useMemo(() => {
    let list = [...BRAWLER_META];
    if (classFilter !== "ALL") list = list.filter((b) => b.cls === classFilter);
    if (search) list = list.filter((b) => b.name.includes(search));
    const dir = sort.dir === "asc" ? 1 : -1;
    const TIER_ORDER: Record<BrawlerMeta["tier"], number> = { S: 5, A: 4, B: 3, C: 2, D: 1 };
    list.sort((a, b) => {
      let cmp = 0;
      switch (sort.key) {
        case "winRate":
          cmp = a.winRate - b.winRate;
          break;
        case "pickRate":
          cmp = a.pickRate - b.pickRate;
          break;
        case "starRate":
          cmp = a.starRate - b.starRate;
          break;
        case "tier":
          cmp = TIER_ORDER[a.tier] - TIER_ORDER[b.tier];
          break;
        case "name":
          cmp = a.name.localeCompare(b.name);
          break;
      }
      return cmp * dir;
    });
    return list;
  }, [classFilter, search, sort]);

  const toggleSort = (key: SortKey) => {
    setSort((prev) => {
      if (prev.key !== key) return { key, dir: key === "name" ? "asc" : "desc" };
      return { key, dir: prev.dir === "desc" ? "asc" : "desc" };
    });
  };

  const totalBattles = meta.reduce((s, b) => s + b.battleCount, 0);

  return (
    <div className="main">
      <div
        style={{
          display: "flex",
          alignItems: "flex-end",
          justifyContent: "space-between",
          marginBottom: 18,
        }}
      >
        <div>
          <div
            style={{
              fontSize: 11,
              fontWeight: 700,
              color: "var(--color-neutral-500)",
              letterSpacing: "0.08em",
              textTransform: "uppercase",
              marginBottom: 6,
            }}
          >
            META · 브롤러 통계
          </div>
          <h1
            style={{
              margin: 0,
              fontFamily: "var(--font-display)",
              fontSize: 32,
              letterSpacing: "0.02em",
              display: "flex",
              alignItems: "baseline",
              gap: 10,
            }}
          >
            브롤러 메타
            <span
              style={{
                fontSize: 13,
                color: "var(--color-neutral-500)",
                fontFamily: "var(--font-body)",
                fontWeight: 600,
                letterSpacing: 0,
              }}
            >
              지난 7일 · {fmtNum(totalBattles)} 전투 분석
            </span>
          </h1>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 14 }}>
        <div
          style={{
            padding: 12,
            display: "flex",
            alignItems: "center",
            gap: 14,
            flexWrap: "wrap",
          }}
        >
          <FilterLabel>티어 구간</FilterLabel>
          <select
            value={tierRange}
            onChange={(e) => setTierRange(e.target.value)}
            style={selectStyle}
          >
            <optgroup label="트로피">
              {STATS_TIER_RANGES.filter((t) => t.group === "트로피").map((t) => (
                <option key={t.key} value={t.key}>
                  {t.label}
                </option>
              ))}
            </optgroup>
            <optgroup label="경쟁전">
              {STATS_TIER_RANGES.filter((t) => t.group === "경쟁전").map((t) => (
                <option key={t.key} value={t.key}>
                  {t.label}
                </option>
              ))}
            </optgroup>
          </select>

          <FilterLabel>기간</FilterLabel>
          <select
            value={dateRange}
            onChange={(e) => setDateRange(e.target.value)}
            style={selectStyle}
          >
            {STATS_DATE_RANGES.map((d) => (
              <option key={d.key} value={d.key}>
                {d.label}
              </option>
            ))}
          </select>

          <div style={{ flex: 1 }} />

          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="브롤러 검색"
            style={{
              padding: "6px 10px",
              fontSize: 13,
              border: "1px solid var(--color-neutral-200)",
              borderRadius: 6,
              outline: "none",
              width: 150,
            }}
          />
        </div>
      </div>

      <div className="card" style={{ marginBottom: 14, overflow: "hidden" }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: `repeat(${1 + Object.keys(CLS).length}, 1fr)`,
            height: 64,
          }}
        >
          <RoleTab active={classFilter === "ALL"} onClick={() => setClassFilter("ALL")}>
            <span
              style={{
                fontFamily: "var(--font-display)",
                fontWeight: 800,
                fontSize: 17,
                letterSpacing: "0.02em",
              }}
            >
              전체
            </span>
          </RoleTab>
          {(Object.entries(CLS) as [ClassKey, (typeof CLS)[ClassKey]][]).map(([k, c]) => (
            <RoleTab key={k} active={classFilter === k} onClick={() => setClassFilter(k)}>
              <ClassIcon cls={k} size={20} />
              <span
                style={{
                  fontFamily: "var(--font-display)",
                  fontWeight: 800,
                  fontSize: 17,
                  letterSpacing: "0.02em",
                }}
              >
                {c.name}
              </span>
            </RoleTab>
          ))}
        </div>
      </div>

      <div className="card" style={{ overflow: "hidden" }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: COLS,
            padding: "10px 16px",
            gap: 10,
            fontSize: 12,
            color: "var(--color-neutral-500)",
            fontWeight: 700,
            borderBottom: "1px solid var(--color-neutral-200)",
            background: "var(--color-neutral-50)",
          }}
        >
          <span>순위</span>
          <SortHeader sort={sort} sortKey="name" onClick={() => toggleSort("name")} align="left">
            브롤러
          </SortHeader>
          <SortHeader
            sort={sort}
            sortKey="tier"
            onClick={() => toggleSort("tier")}
            align="center"
          >
            티어
          </SortHeader>
          <span style={{ textAlign: "center" }}>역할</span>
          <SortHeader sort={sort} sortKey="winRate" onClick={() => toggleSort("winRate")}>
            승률
          </SortHeader>
          <SortHeader sort={sort} sortKey="pickRate" onClick={() => toggleSort("pickRate")}>
            픽률
          </SortHeader>
          <SortHeader sort={sort} sortKey="starRate" onClick={() => toggleSort("starRate")}>
            스타플레이어율
          </SortHeader>
        </div>
        {meta.map((b, i) => (
          <div
            key={b.id}
            onClick={() => router.push(`/brawlers/${b.id}`)}
            style={{
              display: "grid",
              gridTemplateColumns: COLS,
              padding: "8px 16px",
              gap: 10,
              alignItems: "center",
              borderBottom: i === meta.length - 1 ? "none" : "1px solid var(--color-neutral-100)",
              cursor: "pointer",
              minHeight: 56,
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.background = "var(--color-neutral-50)")
            }
            onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
          >
            <span
              className="num"
              style={{ color: "var(--color-neutral-500)", fontSize: 14, fontWeight: 600 }}
            >
              {i + 1}
            </span>

            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <BrawlerPortrait id={b.id} size={36} showFrame />
              <div style={{ fontWeight: 700, fontSize: 14 }}>{b.name}</div>
            </div>

            <div style={{ display: "flex", justifyContent: "center" }}>
              <TierBadge tier={b.tier} />
            </div>

            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: 4,
              }}
              title={CLS[b.cls].name}
            >
              <ClassIcon cls={b.cls} size={20} />
            </div>

            <span
              className="num"
              style={{
                textAlign: "right",
                fontSize: 14,
                fontWeight: 600,
                color:
                  b.winRate >= 0.55
                    ? "var(--color-accent-green)"
                    : b.winRate < 0.48
                      ? "var(--color-accent-red)"
                      : "var(--color-neutral-800)",
              }}
            >
              {(b.winRate * 100).toFixed(2)}%
            </span>

            <span
              className="num"
              style={{ textAlign: "right", fontSize: 14, color: "var(--color-neutral-700)" }}
            >
              {(b.pickRate * 100).toFixed(2)}%
            </span>

            <span
              className="num"
              style={{ textAlign: "right", fontSize: 14, color: "var(--color-neutral-700)" }}
            >
              {(b.starRate * 100).toFixed(2)}%
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
