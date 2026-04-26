"use client";
import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  CLS,
  brawler,
  getEvent,
  fmtNum,
  genEventBrawlerStats,
  genPairStats,
  type BrawlerMeta,
  type EventBrawlerStat,
  type ResolvedEvent,
} from "@/lib/data";
import BrawlerPortrait from "@/components/primitives/BrawlerPortrait";

type DateRange = "RECENT_1_DAY" | "RECENT_3_DAYS" | "RECENT_7_DAYS" | "RECENT_14_DAYS";
type TierRange = "ALL" | "DIAMOND_PLUS" | "LEGENDARY_PLUS" | "MASTERS";

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
          display: "flex",
          gap: 4,
          padding: 3,
          background: "var(--color-neutral-100)",
          borderRadius: 6,
        }}
      >
        {options.map((o) => (
          <button
            key={o.v}
            onClick={() => onChange(o.v)}
            style={{
              padding: "4px 10px",
              fontSize: 12,
              fontWeight: 600,
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              background: value === o.v ? "#fff" : "transparent",
              color: value === o.v ? "var(--color-neutral-900)" : "var(--color-neutral-600)",
              boxShadow: value === o.v ? "0 1px 2px rgba(20,24,32,0.08)" : "none",
            }}
          >
            {o.l}
          </button>
        ))}
      </div>
    </div>
  );
}

function Bar({ pct, val, color }: { pct: number; val: string; color: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
      <div
        style={{
          flex: 1,
          height: 5,
          background: "var(--color-neutral-100)",
          borderRadius: 3,
          overflow: "hidden",
        }}
      >
        <div
          style={{ width: `${Math.min(100, pct)}%`, height: "100%", background: color }}
        />
      </div>
      <span
        className="num"
        style={{ width: 46, textAlign: "right", fontSize: 12, fontWeight: 700 }}
      >
        {val}
      </span>
    </div>
  );
}

function TierMini({ tier, score }: { tier: BrawlerMeta["tier"]; score: number }) {
  const c = { S: "#FFC60A", A: "#FF8A1F", B: "#2E7CF6", C: "#8A90A0", D: "#FF3A3A" }[tier] || "#8A90A0";
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
      <span
        style={{
          display: "inline-flex",
          alignItems: "center",
          justifyContent: "center",
          width: 24,
          height: 24,
          borderRadius: 5,
          background: c,
          color: "#fff",
          fontFamily: "var(--font-display)",
          fontWeight: 900,
          fontSize: 12,
          textShadow: "0 1px 1px rgba(0,0,0,0.2)",
        }}
      >
        {tier}
      </span>
      <span className="num" style={{ fontSize: 11, color: "var(--color-neutral-500)" }}>
        {score}점
      </span>
    </div>
  );
}

function EventHero({
  ev,
  isShowdown,
  navigate,
}: {
  ev: ResolvedEvent;
  isShowdown: boolean;
  navigate: (r: string) => void;
}) {
  const e = ev.event;
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
          onClick={() => navigate("/events")}
          style={{ cursor: "pointer", color: "var(--color-neutral-700)", fontWeight: 600 }}
        >
          이벤트
        </a>
        <span style={{ margin: "0 8px" }}>/</span>
        <span>{e.modeKo}</span>
        <span style={{ margin: "0 8px" }}>/</span>
        <span style={{ color: "var(--color-neutral-900)", fontWeight: 700 }}>{ev.map}</span>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "320px 1fr",
          background: `linear-gradient(135deg, ${e.color}10, ${e.color}05)`,
        }}
      >
        <div
          style={{
            background: `linear-gradient(135deg, ${e.color}, ${e.color}cc)`,
            color: "#fff",
            padding: "32px 26px",
            display: "flex",
            flexDirection: "column",
            gap: 10,
            position: "relative",
            overflow: "hidden",
            minHeight: 200,
          }}
        >
          <div
            style={{
              position: "absolute",
              right: -30,
              bottom: -50,
              fontSize: 220,
              fontFamily: "var(--font-display)",
              opacity: 0.10,
              fontWeight: 900,
              lineHeight: 1,
            }}
          >
            {ev.map.slice(0, 1)}
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            <span
              style={{
                background: "rgba(0,0,0,0.3)",
                padding: "4px 10px",
                borderRadius: 4,
                fontSize: 11,
                fontWeight: 700,
                letterSpacing: "0.06em",
                textTransform: "uppercase",
              }}
            >
              {e.mode}
            </span>
            {isShowdown && (
              <span
                style={{
                  background: "rgba(255,198,10,0.9)",
                  color: "#1a1f2b",
                  padding: "4px 10px",
                  borderRadius: 4,
                  fontSize: 11,
                  fontWeight: 800,
                  letterSpacing: "0.04em",
                }}
              >
                SHOWDOWN
              </span>
            )}
          </div>
          <div
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 14,
              fontWeight: 700,
              opacity: 0.85,
              marginTop: 4,
            }}
          >
            {e.modeKo}
          </div>
          <div
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 30,
              fontWeight: 800,
              letterSpacing: "0.02em",
              textShadow: "0 2px 4px rgba(0,0,0,0.25)",
              lineHeight: 1.1,
            }}
          >
            {ev.map}
          </div>
          <div
            style={{
              marginTop: "auto",
              fontSize: 11,
              opacity: 0.85,
              fontFamily: "var(--font-num)",
            }}
          >
            EVENT ID · {ev.id}
          </div>
        </div>

        <div
          style={{
            padding: "26px 28px",
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
          }}
        >
          <div
            style={{
              fontSize: 11,
              fontWeight: 700,
              color: "var(--color-neutral-500)",
              letterSpacing: "0.08em",
              textTransform: "uppercase",
              marginBottom: 10,
            }}
          >
            이 맵에서의 메타
          </div>
          <h1
            style={{
              margin: 0,
              fontFamily: "var(--font-display)",
              fontSize: 24,
              lineHeight: 1.25,
              color: "var(--color-neutral-900)",
            }}
          >
            {isShowdown
              ? "쇼다운에서 가장 좋은 평균 등수를 기록한 브롤러를 보여드려요."
              : "이 맵에서 가장 높은 승률·티어 점수를 기록한 브롤러를 보여드려요."}
          </h1>
          <div
            style={{
              marginTop: 14,
              padding: 12,
              background: "#fff",
              borderRadius: 8,
              border: "1px solid var(--color-neutral-200)",
              fontSize: 12,
              color: "var(--color-neutral-600)",
              lineHeight: 1.6,
            }}
          >
            <strong style={{ color: "var(--color-neutral-800)" }}>분석 방식</strong>
            {" — "}
            {isShowdown
              ? "/v1/statistics/brawler/rank API의 평균 등수(낮을수록 좋음)와 픽률을 기준으로 정렬했어요. 브롤러를 선택하면 페어 등수도 비교할 수 있어요."
              : "/v1/statistics/brawler/result API의 승률·픽률·티어 점수를 사용해요. 브롤러를 선택하면 같은 팀 시너지와 적군 매치업도 볼 수 있어요."}
          </div>
        </div>
      </div>
    </div>
  );
}

function BrawlerStatsTable({
  stats,
  isShowdown,
  selectedId,
  onSelect,
}: {
  stats: EventBrawlerStat[];
  isShowdown: boolean;
  selectedId: string | null;
  onSelect: (id: string | null) => void;
}) {
  return (
    <div className="card">
      <div className="card-head">
        <div className="card-title">
          {isShowdown ? "브롤러 등수 통계" : "브롤러 통계"}
        </div>
        <span style={{ fontSize: 12, color: "var(--color-neutral-500)" }}>
          행을 클릭하면 우측에 페어/적군 통계가 표시돼요
        </span>
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: isShowdown
            ? "40px 56px 1fr 1fr 1fr 90px"
            : "40px 56px 1fr 90px 1fr 1fr 80px 80px",
          padding: "10px 16px",
          gap: 10,
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
        <span>브롤러</span>
        {isShowdown ? (
          <>
            <span>평균 등수</span>
            <span>픽률</span>
            <span style={{ textAlign: "right" }}>전투 수</span>
          </>
        ) : (
          <>
            <span>티어</span>
            <span>승률</span>
            <span>픽률</span>
            <span>스타플레이어</span>
            <span style={{ textAlign: "right" }}>전투 수</span>
          </>
        )}
      </div>
      <div style={{ maxHeight: 720, overflowY: "auto" }}>
        {stats.slice(0, 30).map((s, i) => {
          const selected = selectedId === s.brawler.id;
          return (
            <div
              key={s.brawler.id}
              onClick={() => onSelect(selected ? null : s.brawler.id)}
              style={{
                display: "grid",
                gridTemplateColumns: isShowdown
                  ? "40px 56px 1fr 1fr 1fr 90px"
                  : "40px 56px 1fr 90px 1fr 1fr 80px 80px",
                padding: "10px 16px",
                gap: 10,
                alignItems: "center",
                borderBottom: "1px solid var(--color-neutral-100)",
                cursor: "pointer",
                background: selected ? "rgba(255,198,10,0.10)" : "transparent",
                borderLeft: selected
                  ? "3px solid var(--color-brand-yellow)"
                  : "3px solid transparent",
              }}
              onMouseEnter={(e) => {
                if (!selected) e.currentTarget.style.background = "var(--color-neutral-50)";
              }}
              onMouseLeave={(e) => {
                if (!selected) e.currentTarget.style.background = "transparent";
              }}
            >
              <span
                className="num"
                style={{
                  color: i < 3 ? "var(--color-brand-yellow-deep)" : "var(--color-neutral-400)",
                  fontWeight: 800,
                  fontSize: 14,
                }}
              >
                {i + 1}
              </span>
              <BrawlerPortrait id={s.brawler.id} size={42} showFrame />
              <div>
                <div style={{ fontSize: 13, fontWeight: 700 }}>{s.brawler.name}</div>
                <div style={{ fontSize: 10, color: "var(--color-neutral-500)" }}>
                  {CLS[s.brawler.cls].name}
                </div>
              </div>

              {isShowdown ? (
                <>
                  <div
                    className="num"
                    style={{
                      fontSize: 16,
                      fontWeight: 800,
                      color:
                        s.averageRank <= 4
                          ? "var(--color-accent-green)"
                          : s.averageRank <= 5.5
                            ? "var(--color-neutral-700)"
                            : "var(--color-accent-red)",
                    }}
                  >
                    {s.averageRank.toFixed(2)}
                  </div>
                  <Bar
                    pct={s.pickRate * 100 * 5}
                    val={(s.pickRate * 100).toFixed(1) + "%"}
                    color="var(--color-accent-blue)"
                  />
                  <span
                    className="num"
                    style={{
                      textAlign: "right",
                      fontSize: 12,
                      color: "var(--color-neutral-500)",
                    }}
                  >
                    {fmtNum(s.totalBattleCount)}
                  </span>
                </>
              ) : (
                <>
                  <TierMini tier={s.tier} score={s.tierScore} />
                  <Bar
                    pct={s.winRate * 100 * 1.6}
                    val={(s.winRate * 100).toFixed(1) + "%"}
                    color={
                      s.winRate >= 0.55
                        ? "var(--color-accent-green)"
                        : s.winRate >= 0.5
                          ? "var(--color-brand-yellow)"
                          : "var(--color-accent-red)"
                    }
                  />
                  <Bar
                    pct={s.pickRate * 100 * 5}
                    val={(s.pickRate * 100).toFixed(1) + "%"}
                    color="var(--color-accent-blue)"
                  />
                  <span className="num" style={{ fontSize: 12, color: "var(--color-neutral-700)" }}>
                    {(s.starPlayerRate * 100).toFixed(1)}%
                  </span>
                  <span
                    className="num"
                    style={{
                      textAlign: "right",
                      fontSize: 12,
                      color: "var(--color-neutral-500)",
                    }}
                  >
                    {fmtNum(s.totalBattleCount)}
                  </span>
                </>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function SidePanel({
  ev,
  isShowdown,
  selectedBrawlerId,
  onClear,
  navigate,
}: {
  ev: ResolvedEvent;
  isShowdown: boolean;
  selectedBrawlerId: string | null;
  onClear: () => void;
  navigate: (r: string) => void;
}) {
  const [tab, setTab] = useState<"pair" | "enemy">("pair");
  const data = useMemo(() => {
    if (!selectedBrawlerId) return { pair: [], enemy: [] };
    if (isShowdown) {
      return { pair: genPairStats(ev.id, selectedBrawlerId, true, false), enemy: [] };
    }
    return {
      pair: genPairStats(ev.id, selectedBrawlerId, false, false),
      enemy: genPairStats(ev.id, selectedBrawlerId, false, true),
    };
  }, [ev.id, selectedBrawlerId, isShowdown]);

  if (!selectedBrawlerId) {
    return (
      <div className="card" style={{ padding: 24, position: "sticky", top: 14 }}>
        <div
          style={{
            width: 48,
            height: 48,
            borderRadius: 10,
            background: "var(--color-neutral-100)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 24,
            marginBottom: 12,
          }}
        >
          👥
        </div>
        <div
          style={{
            fontFamily: "var(--font-display)",
            fontSize: 18,
            fontWeight: 800,
            marginBottom: 4,
          }}
        >
          브롤러를 선택해보세요
        </div>
        <div style={{ fontSize: 12, color: "var(--color-neutral-600)", lineHeight: 1.6 }}>
          좌측 표에서 브롤러를 클릭하면{" "}
          {isShowdown
            ? "쇼다운에서 함께 살아남았을 때 등수가 잘 나오는 페어"
            : "같은 팀 시너지와 잘 잡는/잡히는 적군"}
          을 보여드려요.
        </div>
        <div
          style={{
            marginTop: 16,
            padding: 10,
            background: "var(--color-neutral-50)",
            borderRadius: 6,
            fontSize: 11,
            color: "var(--color-neutral-500)",
            lineHeight: 1.5,
          }}
        >
          <strong style={{ color: "var(--color-neutral-700)" }}>API</strong>
          <br />
          {isShowdown
            ? "/v1/statistics/brawler/pair/rank"
            : "/v1/statistics/brawler/pair/result, /enemy/result"}
        </div>
      </div>
    );
  }

  const selected = brawler(selectedBrawlerId)!;
  const list = tab === "pair" ? data.pair : data.enemy;

  return (
    <div className="card" style={{ position: "sticky", top: 14 }}>
      <div
        style={{
          padding: "14px 16px",
          background: `linear-gradient(135deg, ${selected.c1}, ${selected.c2})`,
          color: "#fff",
          display: "flex",
          alignItems: "center",
          gap: 12,
          position: "relative",
        }}
      >
        <BrawlerPortrait id={selected.id} size={48} showFrame />
        <div style={{ flex: 1 }}>
          <div
            style={{
              fontSize: 10,
              opacity: 0.85,
              fontWeight: 700,
              letterSpacing: "0.08em",
              textTransform: "uppercase",
            }}
          >
            선택한 브롤러
          </div>
          <div
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 18,
              fontWeight: 800,
              textShadow: "0 1px 2px rgba(0,0,0,0.25)",
            }}
          >
            {selected.name}
          </div>
        </div>
        <button
          onClick={onClear}
          style={{
            background: "rgba(0,0,0,0.25)",
            border: "none",
            color: "#fff",
            width: 24,
            height: 24,
            borderRadius: 6,
            cursor: "pointer",
            fontSize: 14,
            lineHeight: 1,
          }}
          title="선택 해제"
        >
          ✕
        </button>
      </div>

      {!isShowdown && (
        <div className="tabs" style={{ padding: "0 12px" }}>
          <button
            className={`tab ${tab === "pair" ? "active" : ""}`}
            onClick={() => setTab("pair")}
          >
            🤝 페어
          </button>
          <button
            className={`tab ${tab === "enemy" ? "active" : ""}`}
            onClick={() => setTab("enemy")}
          >
            ⚔ 상대
          </button>
        </div>
      )}

      <div style={{ maxHeight: 560, overflowY: "auto" }}>
        {list.slice(0, 12).map((row, i) => {
          const other = (tab === "pair" ? row.pairBrawler : row.enemyBrawler)!;
          const metric = isShowdown ? row.averageRank : row.winRate;
          const metricLabel = isShowdown
            ? metric.toFixed(2)
            : (metric * 100).toFixed(1) + "%";
          const good = isShowdown ? metric <= 4.5 : metric >= 0.52;
          const bad = isShowdown ? metric >= 5.5 : metric <= 0.46;
          return (
            <div
              key={other.id}
              onClick={() => navigate(`/brawlers/${other.id}`)}
              style={{
                display: "grid",
                gridTemplateColumns: "24px 1fr 70px",
                gap: 8,
                alignItems: "center",
                padding: "8px 14px",
                borderBottom: "1px solid var(--color-neutral-100)",
                cursor: "pointer",
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.background = "var(--color-neutral-50)")
              }
              onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
            >
              <span
                className="num"
                style={{ color: "var(--color-neutral-400)", fontSize: 11 }}
              >
                {i + 1}
              </span>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <BrawlerPortrait id={other.id} size={28} showFrame />
                <div>
                  <div style={{ fontSize: 12, fontWeight: 700 }}>{other.name}</div>
                  <div
                    style={{
                      fontSize: 10,
                      color: "var(--color-neutral-500)",
                      fontFamily: "var(--font-num)",
                    }}
                  >
                    픽률 {(row.pickRate * 100).toFixed(1)}%
                  </div>
                </div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div
                  className="num"
                  style={{
                    fontSize: 14,
                    fontWeight: 800,
                    color: good
                      ? "var(--color-accent-green)"
                      : bad
                        ? "var(--color-accent-red)"
                        : "var(--color-neutral-700)",
                  }}
                >
                  {metricLabel}
                </div>
                <div
                  style={{
                    fontSize: 9,
                    color: "var(--color-neutral-500)",
                    fontWeight: 600,
                    letterSpacing: "0.04em",
                    textTransform: "uppercase",
                  }}
                >
                  {isShowdown ? "평균 등수" : tab === "pair" ? "팀 승률" : "내 승률"}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div
        style={{
          padding: "10px 14px",
          background: "var(--color-neutral-50)",
          borderTop: "1px solid var(--color-neutral-100)",
          fontSize: 10,
          color: "var(--color-neutral-500)",
          fontFamily: "var(--font-num)",
        }}
      >
        {isShowdown
          ? "/v1/statistics/brawler/pair/rank"
          : tab === "pair"
            ? "/v1/statistics/brawler/pair/result"
            : "/v1/statistics/brawler/enemy/result"}
      </div>
    </div>
  );
}

export default function EventDetailPage({ id }: { id: string }) {
  const router = useRouter();
  const navigate = (r: string) => router.push(r);
  const ev = getEvent(id);
  const isShowdown = (ev.event.mode || "").toLowerCase().includes("showdown");

  const [tierRange, setTierRange] = useState<TierRange>("DIAMOND_PLUS");
  const [dateRange, setDateRange] = useState<DateRange>("RECENT_7_DAYS");
  const [selectedBrawlerId, setSelectedBrawlerId] = useState<string | null>(null);

  const stats = useMemo(
    () => genEventBrawlerStats(ev.id, isShowdown),
    [ev.id, isShowdown, tierRange, dateRange],
  );

  return (
    <div className="main">
      <EventHero ev={ev} isShowdown={isShowdown} navigate={navigate} />

      <div
        className="card"
        style={{
          marginTop: 14,
          padding: "12px 16px",
          display: "flex",
          gap: 16,
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <FilterGroup<TierRange>
          label="티어"
          value={tierRange}
          onChange={setTierRange}
          options={[
            { v: "ALL", l: "전체" },
            { v: "DIAMOND_PLUS", l: "다이아+" },
            { v: "LEGENDARY_PLUS", l: "레전드+" },
            { v: "MASTERS", l: "마스터" },
          ]}
        />
        <div style={{ width: 1, height: 22, background: "var(--color-neutral-200)" }} />
        <FilterGroup<DateRange>
          label="기간"
          value={dateRange}
          onChange={setDateRange}
          options={[
            { v: "RECENT_1_DAY", l: "1일" },
            { v: "RECENT_3_DAYS", l: "3일" },
            { v: "RECENT_7_DAYS", l: "7일" },
            { v: "RECENT_14_DAYS", l: "14일" },
          ]}
        />
        <div style={{ marginLeft: "auto", fontSize: 11, color: "var(--color-neutral-500)" }}>
          총 전투 수 ·{" "}
          <span className="num" style={{ fontWeight: 700, color: "var(--color-neutral-800)" }}>
            {fmtNum(stats.reduce((a, b) => a + b.totalBattleCount, 0))}
          </span>
        </div>
      </div>

      <div
        style={{
          marginTop: 14,
          display: "grid",
          gridTemplateColumns: "1fr 380px",
          gap: 14,
          alignItems: "flex-start",
        }}
      >
        <BrawlerStatsTable
          stats={stats}
          isShowdown={isShowdown}
          selectedId={selectedBrawlerId}
          onSelect={setSelectedBrawlerId}
        />

        <SidePanel
          ev={ev}
          isShowdown={isShowdown}
          selectedBrawlerId={selectedBrawlerId}
          onClear={() => setSelectedBrawlerId(null)}
          navigate={navigate}
        />
      </div>
    </div>
  );
}
