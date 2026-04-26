"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  BRAWLERS,
  CLS,
  RARITY,
  brawler,
  fmtNum,
  genRanking,
  type RankingType,
  type RankingEntry,
} from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";

function RankCell({ rank }: { rank: number }) {
  if (rank <= 3) {
    const styles = {
      1: { bg: "linear-gradient(135deg, #FFD84D, #FFC60A)", deep: "#A87900" },
      2: { bg: "linear-gradient(135deg, #D5DCE6, #9FAFC5)", deep: "#5C6376" },
      3: { bg: "linear-gradient(135deg, #E0A076, #C77545)", deep: "#7A4528" },
    }[rank as 1 | 2 | 3];
    return (
      <span
        style={{
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          width: 32,
          height: 32,
          borderRadius: 8,
          background: styles.bg,
          color: "#fff",
          fontFamily: "var(--font-display)",
          fontWeight: 900,
          fontSize: 16,
          textShadow: "0 1px 2px rgba(0,0,0,0.25)",
          boxShadow: `0 2px 6px ${styles.deep}55`,
        }}
      >
        {rank}
      </span>
    );
  }
  return (
    <span
      className="num"
      style={{
        fontSize: 15,
        fontWeight: 700,
        color: rank <= 10 ? "var(--color-brand-yellow-deep)" : "var(--color-neutral-600)",
      }}
    >
      {rank}
    </span>
  );
}

function DeltaCell({ delta }: { delta: number }) {
  if (delta === 0) {
    return <span style={{ color: "var(--color-neutral-400)", fontSize: 14 }}>—</span>;
  }
  const up = delta > 0;
  return (
    <span
      className="num"
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 2,
        fontSize: 12,
        fontWeight: 800,
        color: up ? "var(--color-accent-green)" : "var(--color-accent-red)",
      }}
    >
      {up ? "▲" : "▼"} {Math.abs(delta)}
    </span>
  );
}

function BrawlerRankHeader({ brawlerId }: { brawlerId: string }) {
  const b = brawler(brawlerId);
  if (!b) return null;
  return (
    <div
      className="card"
      style={{
        marginBottom: 14,
        padding: 18,
        display: "flex",
        alignItems: "center",
        gap: 16,
        background: `linear-gradient(135deg, ${b.c1}15, ${b.c2}15)`,
        borderColor: b.c1 + "33",
      }}
    >
      <BrawlerPortrait id={b.id} size={64} showFrame />
      <div style={{ flex: 1 }}>
        <div style={{ fontFamily: "var(--font-display)", fontSize: 22, fontWeight: 800 }}>
          {b.name}
        </div>
        <div style={{ fontSize: 12, color: "var(--color-neutral-600)", marginTop: 2 }}>
          {RARITY[b.rarity].name} · {CLS[b.cls].name}
        </div>
      </div>
      <div style={{ textAlign: "right" }}>
        <div
          style={{
            fontSize: 11,
            color: "var(--color-neutral-500)",
            fontWeight: 700,
            letterSpacing: "0.06em",
            textTransform: "uppercase",
          }}
        >
          최고 트로피 (글로벌)
        </div>
        <div className="num" style={{ fontSize: 24, fontWeight: 800, color: b.c2 }}>
          2,184
        </div>
      </div>
    </div>
  );
}

function RankTable({
  list,
  tab,
  navigate,
  brawlerFilter,
  startRank,
}: {
  list: RankingEntry[];
  tab: RankingType;
  navigate: (r: string) => void;
  brawlerFilter: string;
  startRank: number;
}) {
  const isClub = tab === "CLUB";
  const isBrawler = tab === "BRAWLER";
  const cols = isBrawler
    ? "60px 60px 1fr 1fr 120px"
    : isClub
      ? "60px 60px 1fr 100px 120px"
      : "60px 60px 1fr 1fr 120px";

  return (
    <div className="card">
      <div
        style={{
          display: "grid",
          gridTemplateColumns: cols,
          padding: "10px 16px",
          gap: 10,
          fontSize: 11,
          color: "var(--color-neutral-500)",
          fontWeight: 700,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          borderBottom: "1px solid var(--color-neutral-200)",
          background: "var(--color-neutral-50)",
        }}
      >
        <span>순위</span>
        <span>변동</span>
        <span>{isClub ? "클럽" : "플레이어"}</span>
        <span>{isClub ? "멤버" : isBrawler ? "브롤러" : "클럽"}</span>
        <span style={{ textAlign: "right" }}>트로피</span>
      </div>
      {list.map((entry, i) => {
        const rank = startRank + i;
        const baseBg =
          rank === 1
            ? "rgba(255,198,10,0.10)"
            : rank <= 3
              ? "rgba(255,198,10,0.06)"
              : rank <= 10
                ? "rgba(255,198,10,0.03)"
                : "transparent";
        return (
          <div
            key={entry.tag}
            onClick={() =>
              isClub
                ? navigate("/clubs/" + entry.tag)
                : navigate("/players/" + entry.tag.replace("#", ""))
            }
            style={{
              display: "grid",
              gridTemplateColumns: cols,
              padding: "10px 16px",
              gap: 10,
              alignItems: "center",
              borderBottom: "1px solid var(--color-neutral-100)",
              cursor: "pointer",
              background: baseBg,
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.background = "var(--color-neutral-50)")
            }
            onMouseLeave={(e) => (e.currentTarget.style.background = baseBg)}
          >
            <RankCell rank={rank} />
            <DeltaCell delta={entry.delta} />
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              {isClub ? (
                <div
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: 6,
                    background: "linear-gradient(135deg, #5D6376, #2A2E38)",
                    color: "#fff",
                    fontFamily: "var(--font-display)",
                    fontWeight: 900,
                    fontSize: 14,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {entry.name.slice(-1)}
                </div>
              ) : (
                <div
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: "50%",
                    background: `hsl(${(rank * 53) % 360}, 60%, 55%)`,
                    color: "#fff",
                    fontFamily: "var(--font-display)",
                    fontWeight: 800,
                    fontSize: 14,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {entry.name.slice(0, 1)}
                </div>
              )}
              <div>
                <div style={{ fontSize: 14, fontWeight: 700 }}>{entry.name}</div>
                <div className="num" style={{ fontSize: 11, color: "var(--color-neutral-500)" }}>
                  {entry.tag}
                </div>
              </div>
            </div>
            {isClub ? (
              <span className="num" style={{ fontSize: 13, color: "var(--color-neutral-700)" }}>
                {20 + (i % 11)}/30
              </span>
            ) : isBrawler ? (
              <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                <BrawlerPortrait id={brawlerFilter} size={22} showFrame />
                <span style={{ fontSize: 12, color: "var(--color-neutral-700)" }}>
                  {brawler(brawlerFilter)?.name}
                </span>
              </div>
            ) : (
              <span style={{ fontSize: 12, color: "var(--color-neutral-600)" }}>
                {entry.club ? (
                  <span style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
                    <span
                      style={{
                        width: 8,
                        height: 8,
                        borderRadius: 2,
                        background: "var(--color-neutral-400)",
                      }}
                    />
                    {entry.club}
                  </span>
                ) : (
                  <span style={{ color: "var(--color-neutral-400)" }}>—</span>
                )}
              </span>
            )}
            <div style={{ textAlign: "right" }}>
              <span
                className="num"
                style={{ fontSize: 15, fontWeight: 800, color: "var(--color-neutral-900)" }}
              >
                <img
                  src="/assets/trophy.png"
                  alt=""
                  style={{
                    width: 13,
                    height: 13,
                    display: "inline",
                    verticalAlign: "middle",
                    marginRight: 3,
                  }}
                />
                {entry.trophies.toLocaleString()}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default function RankingPage() {
  const router = useRouter();
  const navigate = (r: string) => router.push(r);
  const [tab, setTab] = useState<RankingType>("PLAYER");
  const [country, setCountry] = useState("GLOBAL");
  const [brawlerFilter, setBrawlerFilter] = useState("shelly");

  const list = genRanking(tab, 50);

  return (
    <div className="main">
      <div style={{ marginBottom: 18 }}>
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
          RANKING · 글로벌 TOP 200
        </div>
        <h1
          style={{
            margin: 0,
            fontFamily: "var(--font-display)",
            fontSize: 32,
            letterSpacing: "0.02em",
          }}
        >
          랭킹
        </h1>
      </div>

      <div className="card" style={{ marginBottom: 14 }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 12px",
          }}
        >
          <div className="tabs" style={{ flex: 1 }}>
            {(
              [
                { id: "PLAYER", label: "👤 플레이어" },
                { id: "CLUB", label: "🏛 클럽" },
                { id: "BRAWLER", label: "⚔ 브롤러별" },
              ] as { id: RankingType; label: string }[]
            ).map((t) => (
              <button
                key={t.id}
                className={`tab ${tab === t.id ? "active" : ""}`}
                onClick={() => setTab(t.id)}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div style={{ display: "flex", gap: 8, alignItems: "center", padding: "8px 0" }}>
            {tab === "BRAWLER" && (
              <select
                value={brawlerFilter}
                onChange={(e) => setBrawlerFilter(e.target.value)}
                style={{
                  padding: "6px 10px",
                  fontSize: 13,
                  border: "1px solid var(--color-neutral-200)",
                  borderRadius: 6,
                  background: "#fff",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                {BRAWLERS.slice(0, 30).map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            )}
            <select
              value={country}
              onChange={(e) => setCountry(e.target.value)}
              style={{
                padding: "6px 10px",
                fontSize: 13,
                border: "1px solid var(--color-neutral-200)",
                borderRadius: 6,
                background: "#fff",
                cursor: "pointer",
                fontWeight: 600,
              }}
            >
              <option value="GLOBAL">🌐 글로벌</option>
              <option value="KR">🇰🇷 한국</option>
              <option value="US">🇺🇸 미국</option>
              <option value="JP">🇯🇵 일본</option>
              <option value="DE">🇩🇪 독일</option>
              <option value="BR">🇧🇷 브라질</option>
            </select>
          </div>
        </div>
      </div>

      {tab === "BRAWLER" && <BrawlerRankHeader brawlerId={brawlerFilter} />}

      <RankTable
        list={list}
        tab={tab}
        navigate={navigate}
        brawlerFilter={brawlerFilter}
        startRank={1}
      />
      {/* fmtNum used implicitly via other helpers; keep import resolved */}
      <span style={{ display: "none" }}>{fmtNum(0)}</span>
    </div>
  );
}
