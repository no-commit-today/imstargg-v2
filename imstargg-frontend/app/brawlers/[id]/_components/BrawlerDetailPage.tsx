"use client";
import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  CLS,
  RARITY,
  BRAWLER_META,
  fmtNum,
  genBrawlerDailyStats,
  genBrawlerOverallPairStats,
  genBrawlerOverallEnemyStats,
  genBrawlerItemOwnership,
  type BrawlerMeta,
  type DailyStat,
  type OverallPairStat,
  type OverallEnemyStat,
  type ItemOwnership,
} from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";

function TierBadge({ tier, large }: { tier: BrawlerMeta["tier"]; large?: boolean }) {
  const c = { S: "#FFC60A", A: "#FF8A1F", B: "#2E7CF6", C: "#8A90A0", D: "#FF3A3A" }[tier];
  const sz = large ? 36 : 28;
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: sz,
        height: sz,
        borderRadius: 8,
        background: c,
        color: "#fff",
        fontFamily: "var(--font-display)",
        fontWeight: 900,
        fontSize: large ? 22 : 14,
        textShadow: "0 1px 2px rgba(0,0,0,0.25)",
      }}
    >
      {tier}
    </span>
  );
}

function FilterGroup<T extends string>({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: T;
  onChange: (v: T) => void;
  options: { v: T; l: string }[];
}) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <span
        style={{
          fontSize: 11,
          fontWeight: 700,
          color: "var(--color-neutral-500)",
          letterSpacing: "0.06em",
          textTransform: "uppercase",
        }}
      >
        {label}
      </span>
      <div
        style={{
          display: "inline-flex",
          background: "var(--color-neutral-100)",
          borderRadius: 6,
          padding: 2,
        }}
      >
        {options.map((o) => {
          const active = value === o.v;
          return (
            <button
              key={o.v}
              onClick={() => onChange(o.v)}
              style={{
                padding: "5px 10px",
                fontSize: 12,
                fontWeight: 700,
                cursor: "pointer",
                background: active ? "#fff" : "transparent",
                color: active ? "var(--color-neutral-900)" : "var(--color-neutral-500)",
                border: "none",
                borderRadius: 4,
                boxShadow: active ? "0 1px 2px rgba(0,0,0,0.08)" : "none",
              }}
            >
              {o.l}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function BigStat({
  label,
  value,
  sub,
  valueColor,
}: {
  label: string;
  value: React.ReactNode;
  sub?: string;
  valueColor?: string | null;
}) {
  return (
    <div>
      <div
        style={{
          fontSize: 10,
          fontWeight: 700,
          color: "var(--color-neutral-500)",
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          marginBottom: 4,
        }}
      >
        {label}
      </div>
      <div
        className="num"
        style={{
          fontSize: 26,
          fontWeight: 800,
          color: valueColor || "var(--color-neutral-900)",
          lineHeight: 1.1,
        }}
      >
        {value}
      </div>
      {sub && (
        <div style={{ fontSize: 11, color: "var(--color-neutral-500)", marginTop: 2 }}>{sub}</div>
      )}
    </div>
  );
}

function BarTiny({ pct, val, color }: { pct: number; val: string; color: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
      <div
        style={{
          flex: 1,
          height: 4,
          background: "var(--color-neutral-100)",
          borderRadius: 2,
          overflow: "hidden",
        }}
      >
        <div
          style={{ width: `${Math.min(100, pct)}%`, height: "100%", background: color }}
        />
      </div>
      <span
        className="num"
        style={{ width: 42, textAlign: "right", fontSize: 11, fontWeight: 700 }}
      >
        {val}
      </span>
    </div>
  );
}

type DateRange = "LAST_3_DAYS" | "LAST_7_DAYS" | "LAST_14_DAYS" | "LAST_30_DAYS";
type TierRangeKey = "HIGH" | "ALL";
type TrendMetric = "winRate" | "starPlayerRate" | "totalBattleCount";
type PairSort = "PICK_RATE" | "WIN_RATE" | "BATTLE_COUNT";
type EnemySort = "PICK_RATE" | "WIN_RATE_DESC" | "WIN_RATE_ASC" | "BATTLE_COUNT";

function BrawlerDetailHero({
  brawler: b,
  navigate,
  winRank,
  pickRank,
  totalCount,
  dateRange,
  setDateRange,
  tierRange,
  setTierRange,
}: {
  brawler: BrawlerMeta;
  navigate: (r: string) => void;
  winRank: number;
  pickRank: number;
  totalCount: number;
  dateRange: DateRange;
  setDateRange: (v: DateRange) => void;
  tierRange: TierRangeKey;
  setTierRange: (v: TierRangeKey) => void;
}) {
  return (
    <div className="card" style={{ overflow: "hidden" }}>
      <div
        style={{
          padding: "10px 18px",
          borderBottom: "1px solid var(--color-neutral-100)",
          fontSize: 12,
          color: "var(--color-neutral-500)",
        }}
      >
        <a
          onClick={() => navigate("/brawlers")}
          style={{ cursor: "pointer", color: "var(--color-neutral-700)", fontWeight: 600 }}
        >
          브롤러
        </a>
        <span style={{ margin: "0 8px" }}>/</span>
        <span>{CLS[b.cls].name}</span>
        <span style={{ margin: "0 8px" }}>/</span>
        <span style={{ color: "var(--color-neutral-900)", fontWeight: 700 }}>{b.name}</span>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "260px 1fr",
          background: `linear-gradient(135deg, ${b.c1}10, ${b.c2}10)`,
        }}
      >
        <div
          style={{
            background: `linear-gradient(135deg, ${b.c1}, ${b.c2})`,
            color: "#fff",
            padding: "28px 24px",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 10,
            position: "relative",
            overflow: "hidden",
          }}
        >
          <div
            style={{
              position: "absolute",
              top: -20,
              right: -30,
              fontSize: 180,
              fontFamily: "var(--font-display)",
              opacity: 0.07,
              fontWeight: 900,
              lineHeight: 1,
            }}
          >
            {b.name.slice(0, 1)}
          </div>
          <BrawlerPortrait id={b.id} size={140} />
          <div
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 28,
              fontWeight: 800,
              letterSpacing: "0.02em",
              textShadow: "0 2px 4px rgba(0,0,0,0.25)",
            }}
          >
            {b.name}
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            <span
              style={{
                background: "rgba(0,0,0,0.3)",
                padding: "4px 10px",
                borderRadius: 4,
                fontSize: 11,
                fontWeight: 700,
              }}
            >
              {CLS[b.cls].name}
            </span>
            <span
              style={{
                background: RARITY[b.rarity].color,
                color: "#fff",
                padding: "4px 10px",
                borderRadius: 4,
                fontSize: 11,
                fontWeight: 700,
                textShadow: "0 1px 1px rgba(0,0,0,0.3)",
              }}
            >
              {RARITY[b.rarity].name}
            </span>
          </div>
        </div>

        <div
          style={{
            padding: "20px 28px",
            display: "flex",
            flexDirection: "column",
            justifyContent: "space-between",
            gap: 16,
          }}
        >
          <div style={{ display: "flex", gap: 16, alignItems: "center", flexWrap: "wrap" }}>
            <FilterGroup<DateRange>
              label="기간"
              value={dateRange}
              onChange={setDateRange}
              options={[
                { v: "LAST_3_DAYS", l: "3일" },
                { v: "LAST_7_DAYS", l: "7일" },
                { v: "LAST_14_DAYS", l: "14일" },
                { v: "LAST_30_DAYS", l: "30일" },
              ]}
            />
            <FilterGroup<TierRangeKey>
              label="티어"
              value={tierRange}
              onChange={setTierRange}
              options={[
                { v: "HIGH", l: "레전드+" },
                { v: "ALL", l: "전체" },
              ]}
            />
          </div>

          <div>
            <div
              style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16 }}
            >
              <BigStat
                label="티어 점수"
                value={<TierBadge tier={b.tier} large />}
                sub={`전체 ${b.tier} 티어`}
              />
              <BigStat
                label="승률"
                value={`${(b.winRate * 100).toFixed(1)}%`}
                sub={`전체 ${winRank}/${totalCount}위`}
                valueColor={b.winRate >= 0.53 ? "var(--color-accent-green)" : null}
              />
              <BigStat
                label="픽률"
                value={`${(b.pickRate * 100).toFixed(1)}%`}
                sub={`전체 ${pickRank}/${totalCount}위`}
              />
              <BigStat
                label="별의 플레이어 비율"
                value={`${(b.starRate * 100).toFixed(1)}%`}
              />
            </div>
            <div style={{ marginTop: 12, fontSize: 12, color: "var(--color-neutral-500)" }}>
              총{" "}
              <span className="num" style={{ color: "var(--color-neutral-900)" }}>
                {fmtNum(b.battleCount)}
              </span>
              회의 전투 데이터를 기반으로 집계되었습니다.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function DailyTrendCard({ daily, brawler: b }: { daily: DailyStat[]; brawler: BrawlerMeta }) {
  const [metric, setMetric] = useState<TrendMetric>("winRate");
  const W = 1180,
    H = 220,
    P = { l: 50, r: 20, t: 20, b: 30 };
  const chartW = W - P.l - P.r,
    chartH = H - P.t - P.b;

  const values = daily.map((d) => d[metric]);
  const min = Math.min(...values),
    max = Math.max(...values);
  const span = Math.max(0.01, max - min);
  const yMin = min - span * 0.2,
    yMax = max + span * 0.2;

  const battles = daily.map((d) => d.totalBattleCount);
  const battleMax = Math.max(...battles);

  const x = (i: number) => P.l + (i / (daily.length - 1)) * chartW;
  const y = (v: number) => P.t + (1 - (v - yMin) / (yMax - yMin)) * chartH;

  const pathLine = daily.map((d, i) => `${i === 0 ? "M" : "L"} ${x(i)} ${y(d[metric])}`).join(" ");
  const pathArea = `${pathLine} L ${x(daily.length - 1)} ${P.t + chartH} L ${x(0)} ${P.t + chartH} Z`;

  const fmtY = (v: number) =>
    metric === "totalBattleCount" ? fmtNum(v) : `${(v * 100).toFixed(1)}%`;

  const yTicks = Array.from({ length: 5 }, (_, i) => yMin + (yMax - yMin) * (i / 4));
  const xLabels = daily.filter((_, i) => i % 5 === 0 || i === daily.length - 1);

  const first = values[0],
    last = values[values.length - 1];
  const delta = last - first;
  const deltaPct = first ? (delta / first) * 100 : 0;

  return (
    <div className="card">
      <div className="card-head" style={{ alignItems: "center" }}>
        <div className="card-title">통계 추이 (지난 30일)</div>
        <div style={{ display: "flex", gap: 6, marginLeft: "auto" }}>
          {(
            [
              { v: "winRate", l: "승률" },
              { v: "starPlayerRate", l: "별의 플레이어" },
              { v: "totalBattleCount", l: "전투 수" },
            ] as { v: TrendMetric; l: string }[]
          ).map((m) => (
            <button
              key={m.v}
              onClick={() => setMetric(m.v)}
              style={{
                padding: "5px 12px",
                fontSize: 12,
                fontWeight: 700,
                cursor: "pointer",
                background:
                  metric === m.v ? "var(--color-neutral-900)" : "var(--color-neutral-100)",
                color: metric === m.v ? "#fff" : "var(--color-neutral-700)",
                border: "none",
                borderRadius: 5,
              }}
            >
              {m.l}
            </button>
          ))}
        </div>
      </div>
      <div style={{ padding: "14px 18px" }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: 16, marginBottom: 4 }}>
          <span className="num" style={{ fontSize: 28, fontWeight: 800 }}>
            {fmtY(last)}
          </span>
          <span
            className="num"
            style={{
              fontSize: 13,
              fontWeight: 700,
              color: delta >= 0 ? "var(--color-accent-green)" : "var(--color-accent-red)",
            }}
          >
            {delta >= 0 ? "▲" : "▼"}{" "}
            {metric === "totalBattleCount"
              ? `${Math.abs(deltaPct).toFixed(1)}%`
              : `${(Math.abs(delta) * 100).toFixed(2)}%p`}
          </span>
          <span style={{ fontSize: 11, color: "var(--color-neutral-500)" }}>30일 전 대비</span>
        </div>

        <svg width="100%" viewBox={`0 0 ${W} ${H}`} style={{ display: "block" }}>
          {yTicks.map((t, i) => (
            <g key={i}>
              <line
                x1={P.l}
                y1={y(t)}
                x2={W - P.r}
                y2={y(t)}
                stroke="var(--color-neutral-100)"
                strokeWidth={1}
              />
              <text
                x={P.l - 8}
                y={y(t) + 4}
                textAnchor="end"
                fontSize={11}
                fill="var(--color-neutral-500)"
                fontFamily="var(--font-num)"
              >
                {fmtY(t)}
              </text>
            </g>
          ))}

          {metric !== "totalBattleCount" &&
            daily.map((d, i) => {
              const bw = (chartW / daily.length) * 0.5;
              const bh = (d.totalBattleCount / battleMax) * chartH * 0.35;
              return (
                <rect
                  key={i}
                  x={x(i) - bw / 2}
                  y={P.t + chartH - bh}
                  width={bw}
                  height={bh}
                  fill="var(--color-neutral-100)"
                />
              );
            })}

          <path d={pathArea} fill={`${b.c2}15`} />
          <path
            d={pathLine}
            fill="none"
            stroke={b.c2}
            strokeWidth={2.5}
            strokeLinejoin="round"
            strokeLinecap="round"
          />

          {daily.map((d, i) => (
            <circle
              key={i}
              cx={x(i)}
              cy={y(d[metric])}
              r={2.5}
              fill="#fff"
              stroke={b.c2}
              strokeWidth={1.5}
            />
          ))}

          {xLabels.map((d, i) => {
            const idx = daily.indexOf(d);
            const date = new Date(d.date);
            return (
              <text
                key={i}
                x={x(idx)}
                y={H - 8}
                textAnchor="middle"
                fontSize={11}
                fill="var(--color-neutral-500)"
                fontFamily="var(--font-num)"
              >
                {`${date.getMonth() + 1}/${date.getDate()}`}
              </text>
            );
          })}
        </svg>

        <div
          style={{
            display: "flex",
            gap: 18,
            marginTop: 8,
            fontSize: 11,
            color: "var(--color-neutral-500)",
          }}
        >
          <span>
            <span
              style={{
                display: "inline-block",
                width: 12,
                height: 2,
                background: b.c2,
                verticalAlign: "middle",
                marginRight: 4,
              }}
            />
            {metric === "winRate"
              ? "승률"
              : metric === "starPlayerRate"
                ? "별의 플레이어 비율"
                : "전투 수"}
          </span>
          {metric !== "totalBattleCount" && (
            <span>
              <span
                style={{
                  display: "inline-block",
                  width: 8,
                  height: 10,
                  background: "var(--color-neutral-200)",
                  verticalAlign: "middle",
                  marginRight: 4,
                }}
              />
              일별 전투 수
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

function PairStatsCard({
  brawler: b,
  pairs,
  navigate,
}: {
  brawler: BrawlerMeta;
  pairs: OverallPairStat[];
  navigate: (r: string) => void;
}) {
  const [sortBy, setSortBy] = useState<PairSort>("PICK_RATE");
  const sorted = useMemo(() => {
    const arr = [...pairs];
    if (sortBy === "WIN_RATE") arr.sort((a, x) => x.winRate - a.winRate);
    else if (sortBy === "BATTLE_COUNT") arr.sort((a, x) => x.totalBattleCount - a.totalBattleCount);
    else arr.sort((a, x) => x.pickRate - a.pickRate);
    return arr.slice(0, 12);
  }, [pairs, sortBy]);

  return (
    <div className="card">
      <div className="card-head" style={{ alignItems: "center" }}>
        <div>
          <div className="card-title">페어 브롤러 통계</div>
          <div style={{ fontSize: 11, color: "var(--color-neutral-500)", marginTop: 2 }}>
            {b.name}와(과) 같은 팀일 때
          </div>
        </div>
        <div style={{ display: "flex", gap: 4, marginLeft: "auto" }}>
          {(
            [
              { v: "PICK_RATE", l: "픽률" },
              { v: "WIN_RATE", l: "승률" },
              { v: "BATTLE_COUNT", l: "전투 수" },
            ] as { v: PairSort; l: string }[]
          ).map((s) => (
            <button
              key={s.v}
              onClick={() => setSortBy(s.v)}
              style={{
                padding: "5px 10px",
                fontSize: 11,
                fontWeight: 700,
                cursor: "pointer",
                background:
                  sortBy === s.v ? "var(--color-neutral-900)" : "var(--color-neutral-100)",
                color: sortBy === s.v ? "#fff" : "var(--color-neutral-700)",
                border: "none",
                borderRadius: 4,
              }}
            >
              {s.l}
            </button>
          ))}
        </div>
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "32px 44px 1fr 1fr 1fr 70px",
          padding: "10px 14px",
          gap: 8,
          fontSize: 11,
          color: "var(--color-neutral-500)",
          fontWeight: 700,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          borderBottom: "1px solid var(--color-neutral-100)",
          background: "var(--color-neutral-50)",
        }}
      >
        <span>#</span>
        <span></span>
        <span>페어</span>
        <span>승률</span>
        <span>픽률</span>
        <span style={{ textAlign: "right" }}>전투 수</span>
      </div>
      <div style={{ maxHeight: 480, overflowY: "auto" }}>
        {sorted.map((p, i) => (
          <div
            key={p.pairBrawler.id}
            onClick={() => navigate(`/brawlers/${p.pairBrawler.id}`)}
            style={{
              display: "grid",
              gridTemplateColumns: "32px 44px 1fr 1fr 1fr 70px",
              padding: "8px 14px",
              gap: 8,
              alignItems: "center",
              borderBottom: "1px solid var(--color-neutral-100)",
              cursor: "pointer",
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.background = "var(--color-neutral-50)")
            }
            onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
          >
            <span className="num" style={{ fontSize: 12, color: "var(--color-neutral-400)" }}>
              {i + 1}
            </span>
            <BrawlerPortrait id={p.pairBrawler.id} size={34} showFrame />
            <div>
              <div style={{ fontSize: 12, fontWeight: 700 }}>{p.pairBrawler.name}</div>
              <div style={{ fontSize: 10, color: "var(--color-neutral-500)" }}>
                {CLS[p.pairBrawler.cls].name}
              </div>
            </div>
            <BarTiny
              pct={p.winRate * 100 * 1.6}
              val={(p.winRate * 100).toFixed(1) + "%"}
              color={
                p.winRate >= 0.55
                  ? "var(--color-accent-green)"
                  : p.winRate >= 0.5
                    ? "var(--color-brand-yellow)"
                    : "var(--color-accent-red)"
              }
            />
            <BarTiny
              pct={p.pickRate * 100 * 8}
              val={(p.pickRate * 100).toFixed(1) + "%"}
              color="var(--color-accent-blue)"
            />
            <span
              className="num"
              style={{
                textAlign: "right",
                fontSize: 11,
                color: "var(--color-neutral-500)",
              }}
            >
              {fmtNum(p.totalBattleCount)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function EnemyStatsCard({
  brawler: b,
  enemies,
  navigate,
}: {
  brawler: BrawlerMeta;
  enemies: OverallEnemyStat[];
  navigate: (r: string) => void;
}) {
  const [sortBy, setSortBy] = useState<EnemySort>("PICK_RATE");
  const sorted = useMemo(() => {
    const arr = [...enemies];
    if (sortBy === "WIN_RATE_DESC") arr.sort((a, x) => x.winRate - a.winRate);
    else if (sortBy === "WIN_RATE_ASC") arr.sort((a, x) => a.winRate - x.winRate);
    else if (sortBy === "BATTLE_COUNT")
      arr.sort((a, x) => x.totalBattleCount - a.totalBattleCount);
    else arr.sort((a, x) => x.pickRate - a.pickRate);
    return arr.slice(0, 12);
  }, [enemies, sortBy]);

  return (
    <div className="card">
      <div className="card-head" style={{ alignItems: "center" }}>
        <div>
          <div className="card-title">적군 브롤러 통계</div>
          <div style={{ fontSize: 11, color: "var(--color-neutral-500)", marginTop: 2 }}>
            {b.name}와(과) 상대했을 때
          </div>
        </div>
        <div style={{ display: "flex", gap: 4, marginLeft: "auto" }}>
          {(
            [
              { v: "PICK_RATE", l: "픽률" },
              { v: "WIN_RATE_DESC", l: "카운터" },
              { v: "WIN_RATE_ASC", l: "천적" },
            ] as { v: EnemySort; l: string }[]
          ).map((s) => (
            <button
              key={s.v}
              onClick={() => setSortBy(s.v)}
              style={{
                padding: "5px 10px",
                fontSize: 11,
                fontWeight: 700,
                cursor: "pointer",
                background:
                  sortBy === s.v ? "var(--color-neutral-900)" : "var(--color-neutral-100)",
                color: sortBy === s.v ? "#fff" : "var(--color-neutral-700)",
                border: "none",
                borderRadius: 4,
              }}
            >
              {s.l}
            </button>
          ))}
        </div>
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "32px 44px 1fr 1fr 1fr 70px",
          padding: "10px 14px",
          gap: 8,
          fontSize: 11,
          color: "var(--color-neutral-500)",
          fontWeight: 700,
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          borderBottom: "1px solid var(--color-neutral-100)",
          background: "var(--color-neutral-50)",
        }}
      >
        <span>#</span>
        <span></span>
        <span>적군</span>
        <span>승률</span>
        <span>픽률</span>
        <span style={{ textAlign: "right" }}>전투 수</span>
      </div>
      <div style={{ maxHeight: 480, overflowY: "auto" }}>
        {sorted.map((p, i) => (
          <div
            key={p.enemyBrawler.id}
            onClick={() => navigate(`/brawlers/${p.enemyBrawler.id}`)}
            style={{
              display: "grid",
              gridTemplateColumns: "32px 44px 1fr 1fr 1fr 70px",
              padding: "8px 14px",
              gap: 8,
              alignItems: "center",
              borderBottom: "1px solid var(--color-neutral-100)",
              cursor: "pointer",
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.background = "var(--color-neutral-50)")
            }
            onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
          >
            <span className="num" style={{ fontSize: 12, color: "var(--color-neutral-400)" }}>
              {i + 1}
            </span>
            <BrawlerPortrait id={p.enemyBrawler.id} size={34} showFrame />
            <div>
              <div style={{ fontSize: 12, fontWeight: 700 }}>{p.enemyBrawler.name}</div>
              <div style={{ fontSize: 10, color: "var(--color-neutral-500)" }}>
                {CLS[p.enemyBrawler.cls].name}
              </div>
            </div>
            <BarTiny
              pct={p.winRate * 100 * 1.6}
              val={(p.winRate * 100).toFixed(1) + "%"}
              color={
                p.winRate >= 0.55
                  ? "var(--color-accent-green)"
                  : p.winRate >= 0.5
                    ? "var(--color-brand-yellow)"
                    : "var(--color-accent-red)"
              }
            />
            <BarTiny
              pct={p.pickRate * 100 * 8}
              val={(p.pickRate * 100).toFixed(1) + "%"}
              color="var(--color-accent-blue)"
            />
            <span
              className="num"
              style={{
                textAlign: "right",
                fontSize: 11,
                color: "var(--color-neutral-500)",
              }}
            >
              {fmtNum(p.totalBattleCount)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ItemOwnershipCard({
  brawler: b,
  ownership,
}: {
  brawler: BrawlerMeta;
  ownership: ItemOwnership;
}) {
  const groups = [
    { key: "starPowers", label: "스타파워", icon: "/assets/starpower.webp", color: "#FFC60A" },
    { key: "gadgets", label: "가젯", icon: "/assets/gadget.webp", color: "#2BD576" },
    { key: "gears", label: "기어", icon: "/assets/gear.webp", color: "#2E7CF6" },
    { key: "hyperCharges", label: "하이퍼차지", icon: null, color: "#FF3A3A" },
  ] as const;

  return (
    <div className="card">
      <div className="card-head" style={{ alignItems: "center" }}>
        <div>
          <div className="card-title">아이템 보유율</div>
          <div style={{ fontSize: 11, color: "var(--color-neutral-500)", marginTop: 2 }}>
            {b.name}을(를) 보유한 플레이어들의 아이템 장착 통계 · 전체 표본{" "}
            <span className="num">{fmtNum(ownership.totalCount)}</span>명
          </div>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 0 }}>
        {groups.map((g, gi) => {
          const items = ownership[g.key];
          return (
            <div
              key={g.key}
              style={{
                padding: "14px 16px",
                borderRight:
                  gi < groups.length - 1 ? "1px solid var(--color-neutral-100)" : "none",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 6,
                  marginBottom: 12,
                }}
              >
                {g.icon && <img src={g.icon} alt="" style={{ width: 18, height: 18 }} />}
                {!g.icon && (
                  <span
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      justifyContent: "center",
                      width: 18,
                      height: 18,
                      borderRadius: 4,
                      background: g.color,
                      color: "#fff",
                      fontSize: 10,
                      fontWeight: 800,
                    }}
                  >
                    HC
                  </span>
                )}
                <span style={{ fontSize: 13, fontWeight: 700 }}>{g.label}</span>
                <span
                  className="num"
                  style={{
                    marginLeft: "auto",
                    fontSize: 11,
                    color: "var(--color-neutral-500)",
                  }}
                >
                  {items.length}
                </span>
              </div>
              {items.length === 0 ? (
                <div
                  style={{
                    fontSize: 11,
                    color: "var(--color-neutral-400)",
                    padding: "12px 0",
                  }}
                >
                  데이터 없음
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                  {items.map((it) => (
                    <div key={it.itemId}>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "baseline",
                          fontSize: 12,
                          marginBottom: 3,
                        }}
                      >
                        <span style={{ fontWeight: 600, color: "var(--color-neutral-800)" }}>
                          {it.itemName}
                        </span>
                        <span
                          className="num"
                          style={{ fontWeight: 800, color: "var(--color-neutral-900)" }}
                        >
                          {(it.ownershipRate * 100).toFixed(1)}%
                        </span>
                      </div>
                      <div
                        style={{
                          height: 6,
                          background: "var(--color-neutral-100)",
                          borderRadius: 3,
                          overflow: "hidden",
                        }}
                      >
                        <div
                          style={{
                            width: `${it.ownershipRate * 100}%`,
                            height: "100%",
                            background: g.color,
                          }}
                        />
                      </div>
                      <div
                        style={{
                          fontSize: 10,
                          color: "var(--color-neutral-500)",
                          marginTop: 2,
                        }}
                      >
                        <span className="num">{fmtNum(it.ownerCount)}</span> /{" "}
                        <span className="num">{fmtNum(it.totalCount)}</span>명 보유
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function BrawlerDetailPage({ id }: { id: string }) {
  const router = useRouter();
  const navigate = (r: string) => router.push(r);
  const all = BRAWLER_META;
  const b = all.find((x) => x.id === id) || all[0];

  const sortedByWin = useMemo(() => [...all].sort((a, x) => x.winRate - a.winRate), [all]);
  const sortedByPick = useMemo(() => [...all].sort((a, x) => x.pickRate - a.pickRate), [all]);
  const winRank = sortedByWin.findIndex((x) => x.id === b.id) + 1;
  const pickRank = sortedByPick.findIndex((x) => x.id === b.id) + 1;

  const daily = useMemo(() => genBrawlerDailyStats(b.id, 30), [b.id]);
  const pairs = useMemo(() => genBrawlerOverallPairStats(b.id), [b.id]);
  const enemies = useMemo(() => genBrawlerOverallEnemyStats(b.id), [b.id]);
  const ownership = useMemo(() => genBrawlerItemOwnership(b.id), [b.id]);

  const [dateRange, setDateRange] = useState<DateRange>("LAST_7_DAYS");
  const [tierRange, setTierRange] = useState<TierRangeKey>("HIGH");

  return (
    <div className="main">
      <BrawlerDetailHero
        brawler={b}
        navigate={navigate}
        winRank={winRank}
        pickRank={pickRank}
        totalCount={all.length}
        dateRange={dateRange}
        setDateRange={setDateRange}
        tierRange={tierRange}
        setTierRange={setTierRange}
      />

      <div style={{ marginTop: 14 }}>
        <DailyTrendCard daily={daily} brawler={b} />
      </div>

      <div
        style={{
          marginTop: 14,
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 14,
          alignItems: "flex-start",
        }}
      >
        <PairStatsCard brawler={b} pairs={pairs} navigate={navigate} />
        <EnemyStatsCard brawler={b} enemies={enemies} navigate={navigate} />
      </div>

      <div style={{ marginTop: 14 }}>
        <ItemOwnershipCard brawler={b} ownership={ownership} />
      </div>
    </div>
  );
}
