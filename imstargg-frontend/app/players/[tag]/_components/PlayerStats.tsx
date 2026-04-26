"use client";
import { useMemo, useState } from "react";
import {
  CLS,
  OWNED_BRAWLERS,
  PLAYER_STATS,
  rollupByClass,
  type ClassKey,
  type ClassRollup,
  type OwnedBrawler,
  type PlayerStat,
} from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";
import ClassIcon from "@/components/primitives/ClassIcon";

type View = "cards" | "table" | "roles";

interface Row {
  b: OwnedBrawler;
  s: PlayerStat;
}

function DonutCard({
  cls,
  label,
  color,
  winRate,
  battles,
  brawlers,
}: {
  cls: ClassKey;
  label: string;
  color: string;
  winRate: number;
  battles: number;
  brawlers: number;
}) {
  const r = 36;
  const c = 2 * Math.PI * r;
  const filled = winRate * c;
  return (
    <div
      style={{
        padding: 14,
        background: "var(--color-neutral-50)",
        borderRadius: 10,
        textAlign: "center",
      }}
    >
      <div style={{ display: "flex", justifyContent: "center", position: "relative" }}>
        <svg width="90" height="90" viewBox="0 0 90 90">
          <circle cx="45" cy="45" r={r} fill="none" stroke="var(--color-neutral-200)" strokeWidth="8" />
          <circle
            cx="45"
            cy="45"
            r={r}
            fill="none"
            stroke={color}
            strokeWidth="8"
            strokeDasharray={`${filled} ${c}`}
            strokeDashoffset={c / 4}
            transform="rotate(-90 45 45)"
            strokeLinecap="round"
          />
        </svg>
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <div className="num" style={{ fontSize: 18, lineHeight: 1 }}>
            {(winRate * 100).toFixed(0)}
            <span style={{ fontSize: 11, color: "var(--color-neutral-500)" }}>%</span>
          </div>
        </div>
      </div>
      <div
        style={{
          marginTop: 8,
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          gap: 4,
        }}
      >
        <ClassIcon cls={cls} size={14} />
        <span style={{ fontSize: 12, fontWeight: 700 }}>{label}</span>
      </div>
      <div style={{ fontSize: 10, color: "var(--color-neutral-500)", marginTop: 2 }}>
        {battles}전 · {brawlers}브롤러
      </div>
    </div>
  );
}

function RoleDonuts({ rollup }: { rollup: Record<ClassKey, ClassRollup> }) {
  const entries = (Object.entries(rollup) as [ClassKey, ClassRollup][]).filter(
    ([, v]) => v.battles > 0,
  );
  const totalAll = Object.values(rollup).reduce((a, r) => a + r.battles, 0);
  return (
    <div className="card">
      <div className="card-head">
        <div className="card-title">역할(클래스)별 성적</div>
      </div>
      <div className="card-body">
        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12 }}>
          {entries.map(([k, v]) => {
            const total = v.battles;
            const wr = v.wins / total;
            const cls = CLS[k];
            return (
              <DonutCard
                key={k}
                cls={k}
                label={cls.name}
                color={cls.color}
                winRate={wr}
                battles={total}
                brawlers={v.brawlers}
              />
            );
          })}
        </div>
        <div
          style={{
            marginTop: 20,
            paddingTop: 16,
            borderTop: "1px solid var(--color-neutral-100)",
          }}
        >
          <div
            style={{
              fontSize: 11,
              fontWeight: 700,
              letterSpacing: "0.06em",
              textTransform: "uppercase",
              color: "var(--color-neutral-500)",
              marginBottom: 10,
            }}
          >
            플레이 비중
          </div>
          <div
            style={{
              display: "flex",
              height: 28,
              borderRadius: 6,
              overflow: "hidden",
              border: "1px solid var(--color-neutral-200)",
            }}
          >
            {entries.map(([k, v]) => {
              const pct = (v.battles / totalAll) * 100;
              const cls = CLS[k];
              return (
                <div
                  key={k}
                  title={`${cls.name} ${pct.toFixed(1)}%`}
                  style={{
                    width: pct + "%",
                    background: cls.color,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "#fff",
                    fontSize: 10,
                    fontWeight: 800,
                  }}
                >
                  {pct >= 8 && pct.toFixed(0) + "%"}
                </div>
              );
            })}
          </div>
          <div
            style={{
              display: "flex",
              flexWrap: "wrap",
              gap: 12,
              marginTop: 10,
              fontSize: 11,
            }}
          >
            {entries.map(([k]) => {
              const cls = CLS[k];
              return (
                <span key={k} style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
                  <span
                    style={{
                      width: 10,
                      height: 10,
                      background: cls.color,
                      borderRadius: 2,
                    }}
                  />
                  {cls.name}
                </span>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

function StatsCards({ data }: { data: Row[] }) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: 10 }}>
      {data.slice(0, 16).map(({ b, s }) => (
        <div
          key={b.id}
          className="card"
          style={{ padding: 14, display: "flex", alignItems: "center", gap: 12 }}
        >
          <BrawlerPortrait id={b.id} size={48} showFrame />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <span style={{ fontWeight: 700, fontSize: 14 }}>{b.name}</span>
              <ClassIcon cls={b.cls} size={12} />
            </div>
            <div
              style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 6 }}
            >
              <div
                style={{
                  flex: 1,
                  height: 8,
                  background: "var(--color-neutral-100)",
                  borderRadius: 4,
                  overflow: "hidden",
                  display: "flex",
                }}
              >
                <div
                  style={{
                    width: `${s.winRate * 100}%`,
                    background: "var(--color-accent-green)",
                  }}
                />
              </div>
              <span className="num" style={{ fontSize: 12, width: 38, textAlign: "right" }}>
                {(s.winRate * 100).toFixed(0)}%
              </span>
            </div>
            <div style={{ fontSize: 11, color: "var(--color-neutral-500)", marginTop: 4 }}>
              <span className="win-text num">{s.winCount}승</span> ·{" "}
              <span className="loss-text num">{s.lossCount}패</span> ·{" "}
              <span className="num">{s.battleCount}전</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function StatsTable({ data }: { data: Row[] }) {
  return (
    <div className="card">
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "32px 1fr 70px 70px 70px 110px 70px",
          padding: "10px 16px",
          fontSize: 11,
          color: "var(--color-neutral-500)",
          fontWeight: 700,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          borderBottom: "1px solid var(--color-neutral-200)",
        }}
      >
        <span>#</span>
        <span>브롤러</span>
        <span style={{ textAlign: "right" }}>전</span>
        <span style={{ textAlign: "right" }}>승</span>
        <span style={{ textAlign: "right" }}>패</span>
        <span>승률</span>
        <span style={{ textAlign: "right" }}>+/-</span>
      </div>
      {data.map(({ b, s }, i) => (
        <div
          key={b.id}
          style={{
            display: "grid",
            gridTemplateColumns: "32px 1fr 70px 70px 70px 110px 70px",
            padding: "10px 16px",
            borderBottom: "1px solid var(--color-neutral-100)",
            alignItems: "center",
            fontSize: 13,
          }}
        >
          <span className="num" style={{ color: "var(--color-neutral-400)" }}>
            {i + 1}
          </span>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <BrawlerPortrait id={b.id} size={28} />
            <span style={{ fontWeight: 700 }}>{b.name}</span>
          </div>
          <span className="num" style={{ textAlign: "right" }}>
            {s.battleCount}
          </span>
          <span className="num win-text" style={{ textAlign: "right" }}>
            {s.winCount}
          </span>
          <span className="num loss-text" style={{ textAlign: "right" }}>
            {s.lossCount}
          </span>
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
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
                  width: `${s.winRate * 100}%`,
                  height: "100%",
                  background:
                    s.winRate >= 0.55
                      ? "var(--color-accent-green)"
                      : s.winRate >= 0.5
                        ? "var(--color-brand-yellow)"
                        : "var(--color-accent-red)",
                }}
              />
            </div>
            <span className="num" style={{ fontSize: 11, width: 32, textAlign: "right" }}>
              {(s.winRate * 100).toFixed(0)}%
            </span>
          </div>
          <span
            className="num"
            style={{
              textAlign: "right",
              color:
                s.avgTrophyChange >= 0 ? "var(--color-accent-green)" : "var(--color-accent-red)",
            }}
          >
            {s.avgTrophyChange >= 0 ? "+" : ""}
            {s.avgTrophyChange.toFixed(1)}
          </span>
        </div>
      ))}
    </div>
  );
}

export default function PlayerStats() {
  const [view, setView] = useState<View>("cards");
  const allStats = useMemo<Row[]>(
    () =>
      OWNED_BRAWLERS.map((b) => ({ b, s: PLAYER_STATS[b.id] })).sort(
        (a, b) => b.s.battleCount - a.s.battleCount,
      ),
    [],
  );
  const rollup = useMemo(() => rollupByClass(), []);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <div style={{ display: "flex", gap: 6 }}>
        {(
          [
            ["cards", "카드"],
            ["table", "테이블"],
            ["roles", "역할별"],
          ] as [View, string][]
        ).map(([k, l]) => (
          <button
            key={k}
            onClick={() => setView(k)}
            className="btn"
            style={{
              background: view === k ? "var(--color-neutral-900)" : "#fff",
              color: view === k ? "#fff" : "var(--color-neutral-700)",
              borderColor:
                view === k ? "var(--color-neutral-900)" : "var(--color-neutral-200)",
            }}
          >
            {l}
          </button>
        ))}
      </div>

      {view === "roles" && <RoleDonuts rollup={rollup} />}
      {view === "cards" && <StatsCards data={allStats} />}
      {view === "table" && <StatsTable data={allStats} />}
    </div>
  );
}
