"use client";
import { useState } from "react";
import { BATTLES, fmtMinAgo, fmtDuration, type Battle, type BattlePlayer } from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";

type Filter = "ALL" | "WIN" | "LOSS";

function Team({ players, side }: { players: BattlePlayer[]; side: "left" | "right" }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
      {players.map((p, i) => (
        <div
          key={i}
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            padding: "4px 8px",
            background: p.isMe ? "var(--color-brand-yellow-soft)" : "transparent",
            borderRadius: 6,
            border: p.isMe
              ? "1px solid var(--color-brand-yellow-deep)"
              : "1px solid transparent",
            flexDirection: side === "right" ? "row-reverse" : "row",
          }}
        >
          <BrawlerPortrait id={p.brawler} size={28} />
          <div
            style={{
              flex: 1,
              minWidth: 0,
              textAlign: side === "right" ? "right" : "left",
            }}
          >
            <div
              style={{
                fontSize: 12,
                fontWeight: p.isMe ? 800 : 600,
                color: p.isMe ? "var(--color-brand-yellow-deep)" : "var(--color-neutral-900)",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {p.name}
            </div>
            <div
              className="num"
              style={{ fontSize: 10, color: "var(--color-neutral-500)" }}
            >
              {p.trophies}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function BattleCard({ battle: b }: { battle: Battle }) {
  const isShowdown = b.event.mode === "soloShowdown";
  const resultColor =
    b.rank != null
      ? b.rank <= 4
        ? "var(--color-accent-green)"
        : b.rank <= 7
          ? "var(--color-neutral-400)"
          : "var(--color-accent-red)"
      : b.result === "win"
        ? "var(--color-accent-green)"
        : b.result === "draw"
          ? "var(--color-neutral-400)"
          : "var(--color-accent-red)";
  const resultLabel =
    b.rank != null
      ? `#${b.rank}`
      : b.result === "win"
        ? "승리"
        : b.result === "draw"
          ? "무승부"
          : "패배";

  return (
    <div className="card" style={{ borderLeft: `4px solid ${resultColor}` }}>
      <div
        style={{
          padding: "12px 18px",
          display: "flex",
          alignItems: "center",
          gap: 14,
          borderBottom: "1px solid var(--color-neutral-100)",
        }}
      >
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: 8,
            background: b.event.color + "22",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          <div
            style={{
              width: 24,
              height: 24,
              background: b.event.color,
              borderRadius: 4,
            }}
          />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 6,
              fontSize: 14,
              fontWeight: 700,
            }}
          >
            <span>{b.event.modeKo}</span>
            {b.event.mode === "soloRanked" && (
              <span className="pill yellow" style={{ fontSize: 10 }}>
                RANKED
              </span>
            )}
          </div>
          <div style={{ fontSize: 12, color: "var(--color-neutral-500)" }}>
            {b.event.map} · {fmtMinAgo(b.battleTime)} · {fmtDuration(b.duration)}
          </div>
        </div>
        <div style={{ textAlign: "right" }}>
          <div
            className="num"
            style={{ fontSize: 16, color: resultColor, fontWeight: 800 }}
          >
            {resultLabel}
          </div>
          <div
            className="num"
            style={{
              fontSize: 12,
              color:
                b.trophyChange > 0
                  ? "var(--color-accent-green)"
                  : b.trophyChange < 0
                    ? "var(--color-accent-red)"
                    : "var(--color-neutral-400)",
            }}
          >
            {b.trophyChange > 0 ? "+" : ""}
            {b.trophyChange}{" "}
            <img
              src="/assets/trophy.png"
              style={{ width: 11, height: 11, display: "inline", verticalAlign: "middle" }}
              alt=""
            />
          </div>
        </div>
      </div>

      <div style={{ padding: "12px 18px" }}>
        {isShowdown ? (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(5, 1fr)",
              gap: 8,
            }}
          >
            {b.teams.flat().map((p, i) => (
              <div
                key={i}
                style={{
                  padding: 8,
                  background: p.isMe
                    ? "var(--color-brand-yellow-soft)"
                    : "var(--color-neutral-50)",
                  borderRadius: 6,
                  textAlign: "center",
                  border: p.isMe
                    ? "1.5px solid var(--color-brand-yellow-deep)"
                    : "1px solid transparent",
                }}
              >
                <BrawlerPortrait id={p.brawler} size={28} />
                <div
                  style={{
                    fontSize: 10,
                    marginTop: 4,
                    fontWeight: 700,
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                  }}
                >
                  {p.name}
                </div>
                <div
                  className="num"
                  style={{ fontSize: 10, color: "var(--color-neutral-500)" }}
                >
                  {p.trophies}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr auto 1fr",
              gap: 12,
              alignItems: "center",
            }}
          >
            <Team players={b.teams[0]} side="left" />
            <div
              style={{
                fontFamily: "var(--font-display)",
                fontSize: 18,
                color: "var(--color-neutral-400)",
              }}
            >
              VS
            </div>
            <Team players={b.teams[1]} side="right" />
          </div>
        )}
      </div>
    </div>
  );
}

export default function PlayerBattles() {
  const [filter, setFilter] = useState<Filter>("ALL");
  const [showCount, setShowCount] = useState(15);
  const battles = BATTLES;
  const filtered =
    filter === "ALL"
      ? battles
      : filter === "WIN"
        ? battles.filter((b) => b.result === "win" || (b.rank != null && b.rank <= 4))
        : battles.filter((b) => b.result === "loss" || (b.rank != null && b.rank > 4));
  const visible = filtered.slice(0, showCount);

  const last20 = battles.slice(0, 20);
  const lastWins = last20.filter(
    (b) => b.result === "win" || (b.rank != null && b.rank <= 4),
  ).length;

  const trophyChanges = battles.slice(0, 30).slice().reverse();
  const points = trophyChanges.reduce<{ i: number; cumul: number }[]>((acc, b, i) => {
    const prev = i === 0 ? 0 : acc[i - 1].cumul;
    acc.push({ i, cumul: prev + b.trophyChange });
    return acc;
  }, []);
  const cumul = points.length ? points[points.length - 1].cumul : 0;
  const maxC = Math.max(...points.map((p) => p.cumul), 0);
  const minC = Math.min(...points.map((p) => p.cumul), 0);
  const denom = Math.max(Math.abs(maxC), Math.abs(minC), 1);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      <div className="card">
        <div className="card-head">
          <div className="card-title">트로피 변동 (최근 30전)</div>
          <span
            className="num"
            style={{
              fontSize: 13,
              color: cumul >= 0 ? "var(--color-accent-green)" : "var(--color-accent-red)",
            }}
          >
            {cumul >= 0 ? "+" : ""}
            {cumul}
          </span>
        </div>
        <div style={{ padding: "8px 18px 16px" }}>
          <svg viewBox="0 0 600 80" style={{ width: "100%", height: 80 }} preserveAspectRatio="none">
            <line
              x1="0"
              y1={40 - (0 / denom) * 30}
              x2="600"
              y2={40 - (0 / denom) * 30}
              stroke="#D8DCE5"
              strokeDasharray="3,3"
            />
            <polyline
              fill="none"
              stroke="var(--color-brand-yellow)"
              strokeWidth="2"
              strokeLinejoin="round"
              points={points
                .map(
                  (p) =>
                    `${(p.i / (points.length - 1)) * 600},${40 - (p.cumul / denom) * 30}`,
                )
                .join(" ")}
            />
            {points.map((p) => (
              <circle
                key={p.i}
                cx={(p.i / (points.length - 1)) * 600}
                cy={40 - (p.cumul / denom) * 30}
                r="2"
                fill="var(--color-brand-yellow-deep)"
              />
            ))}
          </svg>
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <div className="card-title">최근 폼 (마지막 20전)</div>
          <span style={{ fontSize: 13, color: "var(--color-neutral-500)" }}>
            <span className="num win-text">{lastWins}승</span>
            <span style={{ margin: "0 6px", color: "var(--color-neutral-300)" }}>·</span>
            <span className="num loss-text">{20 - lastWins}패</span>
            <span style={{ margin: "0 6px", color: "var(--color-neutral-300)" }}>·</span>
            <span className="num">{Math.round((lastWins / 20) * 100)}%</span>
          </span>
        </div>
        <div className="card-body">
          <div style={{ display: "flex", gap: 4 }}>
            {last20.map((b) => (
              <div
                key={b.id}
                title={`${b.event.modeKo} · ${b.result || `#${b.rank}`}`}
                style={{
                  flex: 1,
                  height: 32,
                  borderRadius: 4,
                  background:
                    b.rank != null
                      ? b.rank <= 4
                        ? "var(--color-accent-green)"
                        : b.rank <= 7
                          ? "var(--color-neutral-400)"
                          : "var(--color-accent-red)"
                      : b.result === "win"
                        ? "var(--color-accent-green)"
                        : b.result === "draw"
                          ? "var(--color-neutral-400)"
                          : "var(--color-accent-red)",
                  color: "#fff",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 10,
                  fontWeight: 800,
                  fontFamily: "var(--font-num)",
                }}
              >
                {b.rank != null
                  ? `#${b.rank}`
                  : b.result === "win"
                    ? "W"
                    : b.result === "draw"
                      ? "D"
                      : "L"}
              </div>
            ))}
          </div>
        </div>
      </div>

      <div style={{ display: "flex", gap: 6 }}>
        {(
          [
            ["ALL", "전체"],
            ["WIN", "승리"],
            ["LOSS", "패배"],
          ] as [Filter, string][]
        ).map(([k, l]) => (
          <button
            key={k}
            onClick={() => setFilter(k)}
            className="btn"
            style={{
              background: filter === k ? "var(--color-neutral-900)" : "#fff",
              color: filter === k ? "#fff" : "var(--color-neutral-700)",
              borderColor:
                filter === k ? "var(--color-neutral-900)" : "var(--color-neutral-200)",
            }}
          >
            {l}
          </button>
        ))}
      </div>

      {visible.map((b) => (
        <BattleCard key={b.id} battle={b} />
      ))}

      {showCount < filtered.length && (
        <button
          onClick={() => setShowCount((c) => c + 15)}
          className="btn"
          style={{ alignSelf: "center", marginTop: 8 }}
        >
          더 보기 ({filtered.length - showCount}개 남음)
        </button>
      )}
    </div>
  );
}
