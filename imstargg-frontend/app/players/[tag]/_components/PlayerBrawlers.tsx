"use client";
import { useMemo, useState } from "react";
import {
  CLS,
  RARITY,
  OWNED_BRAWLERS,
  PLAYER_STATS,
  BRAWLER_META,
  type ClassKey,
  type OwnedBrawler,
} from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";
import ClassIcon from "@/components/primitives/ClassIcon";
import PowerPill from "@/components/primitives/PowerPill";
import Trophy from "@/components/primitives/Trophy";

type Sort = "trophies" | "power" | "name";
type ClassFilter = ClassKey | "ALL";

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  color?: string;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        display: "inline-flex",
        alignItems: "center",
        padding: "5px 10px",
        fontSize: 12,
        fontWeight: 700,
        borderRadius: 6,
        border: "1px solid " + (active ? "var(--color-neutral-900)" : "var(--color-neutral-200)"),
        background: active ? "var(--color-neutral-900)" : "#fff",
        color: active ? "#fff" : "var(--color-neutral-700)",
        cursor: "pointer",
        transition: "all 0.1s",
      }}
    >
      {children}
    </button>
  );
}

function BoxStat({
  label,
  value,
  accent,
}: {
  label: string;
  value: React.ReactNode;
  accent?: string | null;
}) {
  return (
    <div
      style={{
        padding: "10px 8px",
        background: "var(--color-neutral-50)",
        borderRadius: 8,
        textAlign: "center",
      }}
    >
      <div className="num" style={{ fontSize: 16, color: accent || "var(--color-neutral-900)" }}>
        {value}
      </div>
      <div
        style={{
          fontSize: 10,
          color: "var(--color-neutral-500)",
          marginTop: 2,
          fontWeight: 700,
          letterSpacing: "0.04em",
        }}
      >
        {label}
      </div>
    </div>
  );
}

function BrawlerDetailModal({
  brawler,
  onClose,
}: {
  brawler: OwnedBrawler;
  onClose: () => void;
}) {
  const stats = PLAYER_STATS[brawler.id];
  const meta = BRAWLER_META.find((m) => m.id === brawler.id)!;
  return (
    <div
      onClick={onClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.5)",
        zIndex: 100,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 24,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: "#fff",
          borderRadius: 14,
          width: "100%",
          maxWidth: 520,
          overflow: "hidden",
          boxShadow: "0 24px 64px rgba(0,0,0,0.3)",
        }}
      >
        <div
          style={{
            padding: 24,
            background: `linear-gradient(135deg, ${brawler.c1}, ${brawler.c2})`,
            color: "#fff",
            display: "flex",
            alignItems: "center",
            gap: 16,
            position: "relative",
          }}
        >
          <BrawlerPortrait id={brawler.id} size={80} />
          <div>
            <div
              style={{
                fontSize: 24,
                fontWeight: 800,
                fontFamily: "var(--font-display)",
                letterSpacing: "0.02em",
              }}
            >
              {brawler.name}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 4 }}>
              <span
                style={{
                  background: "rgba(0,0,0,0.3)",
                  padding: "3px 8px",
                  borderRadius: 4,
                  fontSize: 12,
                  fontWeight: 700,
                }}
              >
                {CLS[brawler.cls].name}
              </span>
              <span
                style={{
                  background: "rgba(0,0,0,0.3)",
                  padding: "3px 8px",
                  borderRadius: 4,
                  fontSize: 12,
                  fontWeight: 700,
                }}
              >
                {RARITY[brawler.rarity].name}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              position: "absolute",
              top: 12,
              right: 12,
              background: "rgba(0,0,0,0.3)",
              color: "#fff",
              width: 30,
              height: 30,
              borderRadius: "50%",
              fontSize: 18,
            }}
          >
            ×
          </button>
        </div>

        <div style={{ padding: 20 }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(3, 1fr)",
              gap: 8,
              marginBottom: 16,
            }}
          >
            <BoxStat label="파워" value={`P${brawler.power}`} />
            <BoxStat label="트로피" value={brawler.trophies.toLocaleString()} />
            <BoxStat label="최고" value={brawler.highestTrophies.toLocaleString()} />
          </div>

          <div style={{ marginBottom: 16 }}>
            <div
              style={{
                fontSize: 11,
                fontWeight: 700,
                letterSpacing: "0.08em",
                textTransform: "uppercase",
                color: "var(--color-neutral-500)",
                marginBottom: 8,
              }}
            >
              내 성적 ({stats.battleCount}전)
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <div
                style={{
                  flex: 1,
                  height: 24,
                  borderRadius: 6,
                  background: "var(--color-neutral-100)",
                  overflow: "hidden",
                  display: "flex",
                }}
              >
                <div
                  style={{
                    width: `${stats.winRate * 100}%`,
                    background: "var(--color-accent-green)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "flex-end",
                    paddingRight: 8,
                    color: "#fff",
                    fontWeight: 700,
                    fontSize: 12,
                  }}
                >
                  {stats.winCount}승
                </div>
                <div
                  style={{
                    flex: 1,
                    display: "flex",
                    alignItems: "center",
                    paddingLeft: 8,
                    fontSize: 12,
                    color: "var(--color-accent-red-deep)",
                    fontWeight: 700,
                  }}
                >
                  {stats.lossCount}패
                </div>
              </div>
              <div className="num" style={{ fontSize: 20, fontWeight: 800 }}>
                {(stats.winRate * 100).toFixed(1)}%
              </div>
            </div>
          </div>

          <div style={{ marginBottom: 16 }}>
            <div
              style={{
                fontSize: 11,
                fontWeight: 700,
                letterSpacing: "0.08em",
                textTransform: "uppercase",
                color: "var(--color-neutral-500)",
                marginBottom: 8,
              }}
            >
              메타 (전체 유저 기준)
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <BoxStat
                label="티어"
                value={meta.tier}
                accent={
                  meta.tier === "S"
                    ? "var(--color-brand-yellow)"
                    : meta.tier === "A"
                      ? "var(--color-accent-orange)"
                      : null
                }
              />
              <BoxStat label="승률" value={(meta.winRate * 100).toFixed(1) + "%"} />
              <BoxStat label="픽률" value={(meta.pickRate * 100).toFixed(1) + "%"} />
            </div>
          </div>

          <div>
            <div
              style={{
                fontSize: 11,
                fontWeight: 700,
                letterSpacing: "0.08em",
                textTransform: "uppercase",
                color: "var(--color-neutral-500)",
                marginBottom: 8,
              }}
            >
              빌드
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              {[1, 2].map((i) => (
                <div
                  key={"sp" + i}
                  style={{
                    flex: 1,
                    padding: 10,
                    background: "var(--color-neutral-50)",
                    borderRadius: 8,
                    textAlign: "center",
                    opacity: i <= brawler.starPowers ? 1 : 0.3,
                  }}
                >
                  <img
                    src="/assets/starpower.webp"
                    style={{ width: 28, height: 28, margin: "0 auto" }}
                    alt=""
                  />
                  <div style={{ fontSize: 10, marginTop: 4 }}>스타파워 {i}</div>
                </div>
              ))}
              {[1, 2].map((i) => (
                <div
                  key={"gd" + i}
                  style={{
                    flex: 1,
                    padding: 10,
                    background: "var(--color-neutral-50)",
                    borderRadius: 8,
                    textAlign: "center",
                    opacity: i <= brawler.gadgets ? 1 : 0.3,
                  }}
                >
                  <img
                    src="/assets/gadget.webp"
                    style={{ width: 28, height: 28, margin: "0 auto" }}
                    alt=""
                  />
                  <div style={{ fontSize: 10, marginTop: 4 }}>가젯 {i}</div>
                </div>
              ))}
              <div
                style={{
                  flex: 1,
                  padding: 10,
                  background: "var(--color-neutral-50)",
                  borderRadius: 8,
                  textAlign: "center",
                  opacity: brawler.hasHypercharge ? 1 : 0.3,
                }}
              >
                <img
                  src="/assets/hypercharge.png"
                  style={{ width: 28, height: 28, margin: "0 auto" }}
                  alt=""
                />
                <div style={{ fontSize: 10, marginTop: 4 }}>하이퍼차지</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function PlayerBrawlers() {
  const [sort, setSort] = useState<Sort>("trophies");
  const [classFilter, setClassFilter] = useState<ClassFilter>("ALL");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<OwnedBrawler | null>(null);

  const filtered = useMemo(() => {
    let list = [...OWNED_BRAWLERS];
    if (classFilter !== "ALL") list = list.filter((b) => b.cls === classFilter);
    if (search) list = list.filter((b) => b.name.includes(search));
    if (sort === "trophies") list.sort((a, b) => b.trophies - a.trophies);
    else if (sort === "power") list.sort((a, b) => b.power - a.power || b.trophies - a.trophies);
    else if (sort === "name") list.sort((a, b) => a.name.localeCompare(b.name));
    return list;
  }, [sort, classFilter, search]);

  return (
    <div className="card">
      <div
        style={{
          padding: "12px 18px",
          borderBottom: "1px solid var(--color-neutral-200)",
          display: "flex",
          alignItems: "center",
          gap: 10,
          flexWrap: "wrap",
        }}
      >
        <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
          <FilterChip active={classFilter === "ALL"} onClick={() => setClassFilter("ALL")}>
            전체
          </FilterChip>
          {(Object.entries(CLS) as [ClassKey, (typeof CLS)[ClassKey]][]).map(([k, c]) => (
            <FilterChip
              key={k}
              active={classFilter === k}
              onClick={() => setClassFilter(k)}
              color={c.color}
            >
              <ClassIcon cls={k} size={14} />
              <span style={{ marginLeft: 4 }}>{c.name}</span>
            </FilterChip>
          ))}
        </div>
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
            width: 140,
          }}
        />
        <select
          value={sort}
          onChange={(e) => setSort(e.target.value as Sort)}
          style={{
            padding: "6px 10px",
            fontSize: 13,
            border: "1px solid var(--color-neutral-200)",
            borderRadius: 6,
            background: "#fff",
            cursor: "pointer",
          }}
        >
          <option value="trophies">트로피 순</option>
          <option value="power">파워 순</option>
          <option value="name">이름 순</option>
        </select>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "40px 1fr 80px 100px 1fr",
          padding: "10px 18px",
          fontSize: 11,
          color: "var(--color-neutral-500)",
          fontWeight: 700,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          borderBottom: "1px solid var(--color-neutral-100)",
        }}
      >
        <span>#</span>
        <span>브롤러</span>
        <span>파워</span>
        <span>트로피</span>
        <span>승률 (보유 빌드)</span>
      </div>
      {filtered.map((b, i) => {
        const stats = PLAYER_STATS[b.id];
        return (
          <div
            key={b.id}
            onClick={() => setSelected(b)}
            style={{
              display: "grid",
              gridTemplateColumns: "40px 1fr 80px 100px 1fr",
              padding: "10px 18px",
              borderBottom: "1px solid var(--color-neutral-100)",
              cursor: "pointer",
              alignItems: "center",
              transition: "background 0.1s",
            }}
            onMouseEnter={(e) => (e.currentTarget.style.background = "var(--color-neutral-50)")}
            onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
          >
            <span className="num" style={{ color: "var(--color-neutral-400)", fontSize: 13 }}>
              {i + 1}
            </span>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <BrawlerPortrait id={b.id} size={36} showFrame />
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{b.name}</div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 4,
                    fontSize: 11,
                    color: "var(--color-neutral-500)",
                  }}
                >
                  <ClassIcon cls={b.cls} size={12} />
                  <span>{CLS[b.cls].name}</span>
                </div>
              </div>
            </div>
            <PowerPill level={b.power} />
            <Trophy value={b.trophies} />
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <div
                style={{
                  flex: 1,
                  height: 6,
                  background: "var(--color-neutral-100)",
                  borderRadius: 3,
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    width: `${stats.winRate * 100}%`,
                    height: "100%",
                    background:
                      stats.winRate >= 0.55
                        ? "var(--color-accent-green)"
                        : stats.winRate >= 0.5
                          ? "var(--color-brand-yellow)"
                          : "var(--color-accent-red)",
                  }}
                />
              </div>
              <span className="num" style={{ fontSize: 12, width: 40, textAlign: "right" }}>
                {(stats.winRate * 100).toFixed(0)}%
              </span>
              <div style={{ display: "flex", gap: 2 }}>
                {b.starPowers > 0 && (
                  <img
                    src="/assets/starpower.webp"
                    style={{ width: 16, height: 16 }}
                    title={`스타파워 ${b.starPowers}`}
                    alt=""
                  />
                )}
                {b.gadgets > 0 && (
                  <img
                    src="/assets/gadget.webp"
                    style={{ width: 16, height: 16 }}
                    title={`가젯 ${b.gadgets}`}
                    alt=""
                  />
                )}
                {b.hasHypercharge && (
                  <img
                    src="/assets/hypercharge.png"
                    style={{ width: 16, height: 16 }}
                    title="하이퍼차지"
                    alt=""
                  />
                )}
              </div>
            </div>
          </div>
        );
      })}

      {selected && <BrawlerDetailModal brawler={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}
