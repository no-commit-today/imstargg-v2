"use client";
import { useRouter } from "next/navigation";
import { ROTATION, SOLO_RANKED_MAPS, type RotationItem, type RankedMap } from "@/lib/data";

function SectionHeader({
  eyebrow,
  title,
  sub,
  accent,
}: {
  eyebrow: string;
  title: string;
  sub: string;
  accent?: string;
}) {
  return (
    <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 12 }}>
      <span
        style={{
          fontSize: 10,
          fontWeight: 800,
          letterSpacing: "0.1em",
          padding: "3px 8px",
          borderRadius: 4,
          background: accent || "var(--color-accent-red)",
          color: "#fff",
        }}
      >
        {eyebrow}
      </span>
      <h2
        style={{
          margin: 0,
          fontFamily: "var(--font-display)",
          fontSize: 20,
          fontWeight: 800,
          letterSpacing: "0.02em",
        }}
      >
        {title}
      </h2>
      <span style={{ fontSize: 12, color: "var(--color-neutral-500)" }}>{sub}</span>
    </div>
  );
}

function EventGrid({
  rotation,
  navigate,
}: {
  rotation: RotationItem[];
  navigate: (r: string) => void;
}) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 14 }}>
      {rotation.map((r) => (
        <div
          key={r.id}
          className="card"
          style={{
            overflow: "hidden",
            cursor: "pointer",
            transition: "transform 0.12s, box-shadow 0.12s",
          }}
          onClick={() => navigate(`/events/${r.id}`)}
          onMouseEnter={(e) => {
            e.currentTarget.style.transform = "translateY(-2px)";
            e.currentTarget.style.boxShadow = "0 8px 24px rgba(20,24,32,0.10)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.transform = "none";
            e.currentTarget.style.boxShadow = "";
          }}
        >
          <div
            style={{
              background: `linear-gradient(135deg, ${r.event.color}, ${r.event.color}dd)`,
              color: "#fff",
              padding: "20px 18px",
              position: "relative",
              overflow: "hidden",
            }}
          >
            <div
              style={{
                position: "absolute",
                right: -16,
                top: -16,
                fontSize: 100,
                opacity: 0.12,
                fontWeight: 900,
                fontFamily: "var(--font-display)",
              }}
            >
              {r.event.modeKo.slice(0, 1)}
            </div>
            <div
              style={{
                fontSize: 11,
                fontWeight: 700,
                letterSpacing: "0.08em",
                textTransform: "uppercase",
                opacity: 0.9,
              }}
            >
              {r.slot.replace(/_/g, " ")}
            </div>
            <div
              style={{
                fontFamily: "var(--font-display)",
                fontSize: 22,
                fontWeight: 800,
                marginTop: 4,
                textShadow: "0 1px 2px rgba(0,0,0,0.25)",
              }}
            >
              {r.event.modeKo}
            </div>
            <div style={{ fontSize: 13, marginTop: 4, opacity: 0.95 }}>{r.event.map}</div>
          </div>
          <div
            style={{
              padding: 14,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div>
              <div
                style={{
                  fontSize: 10,
                  color: "var(--color-neutral-500)",
                  fontWeight: 700,
                  letterSpacing: "0.06em",
                  textTransform: "uppercase",
                }}
              >
                남은 시간
              </div>
              <div
                className="num"
                style={{
                  fontSize: 18,
                  color: r.remaining.startsWith("00")
                    ? "var(--color-accent-red)"
                    : "var(--color-neutral-900)",
                }}
              >
                {r.remaining}
              </div>
            </div>
            <div style={{ textAlign: "right" }}>
              <div
                style={{
                  fontSize: 10,
                  color: "var(--color-neutral-500)",
                  fontWeight: 700,
                  letterSpacing: "0.06em",
                  textTransform: "uppercase",
                }}
              >
                시작
              </div>
              <div className="num" style={{ fontSize: 13, color: "var(--color-neutral-700)" }}>
                {r.startedAt}
              </div>
            </div>
          </div>
          <div
            style={{
              padding: "10px 14px",
              background: "var(--color-neutral-50)",
              borderTop: "1px solid var(--color-neutral-100)",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <span
              style={{ fontSize: 11, color: "var(--color-neutral-600)", fontWeight: 600 }}
            >
              브롤러 통계 보기
            </span>
            <span style={{ fontSize: 12, color: "var(--color-neutral-500)" }}>→</span>
          </div>
        </div>
      ))}
    </div>
  );
}

function RankedGrid({
  maps,
  navigate,
}: {
  maps: RankedMap[];
  navigate: (r: string) => void;
}) {
  return (
    <div className="card">
      <div
        style={{
          padding: 14,
          display: "grid",
          gridTemplateColumns: "repeat(2, 1fr)",
          gap: 12,
        }}
      >
        {maps.map((m) => (
          <div
            key={m.id}
            onClick={() => navigate(`/events/${m.id}`)}
            style={{
              display: "flex",
              gap: 14,
              padding: 14,
              border: "1px solid var(--color-neutral-200)",
              borderRadius: 10,
              background: "var(--color-neutral-50)",
              cursor: "pointer",
              transition: "background 0.12s, border-color 0.12s",
              alignItems: "center",
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = "#fff";
              e.currentTarget.style.borderColor = "var(--color-brand-yellow)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = "var(--color-neutral-50)";
              e.currentTarget.style.borderColor = "var(--color-neutral-200)";
            }}
          >
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 10,
                background: `linear-gradient(135deg, ${m.event.color}, ${m.event.color}aa)`,
                color: "#fff",
                fontFamily: "var(--font-display)",
                fontWeight: 900,
                fontSize: 28,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                textShadow: "0 1px 2px rgba(0,0,0,0.3)",
                flexShrink: 0,
              }}
            >
              {m.map.slice(0, 1)}
            </div>
            <div style={{ flex: 1 }}>
              <div
                style={{
                  fontSize: 11,
                  color: "var(--color-neutral-500)",
                  fontWeight: 700,
                  letterSpacing: "0.06em",
                  textTransform: "uppercase",
                }}
              >
                {m.event.modeKo}
              </div>
              <div style={{ fontSize: 16, fontWeight: 800, marginTop: 2 }}>{m.map}</div>
              <div
                style={{ fontSize: 11, color: "var(--color-neutral-500)", marginTop: 4 }}
              >
                브롤러 통계 보기 →
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function EventsPage() {
  const router = useRouter();
  const navigate = (r: string) => router.push(r);
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
          EVENTS · 진행 중인 모드
        </div>
        <h1
          style={{
            margin: 0,
            fontFamily: "var(--font-display)",
            fontSize: 32,
            letterSpacing: "0.02em",
          }}
        >
          이벤트 로테이션
        </h1>
      </div>

      <div style={{ marginBottom: 28 }}>
        <SectionHeader
          eyebrow="LIVE"
          title="현재 진행 중"
          sub={`${ROTATION.length}개 슬롯 · 24시간마다 갱신`}
        />
        <EventGrid rotation={ROTATION} navigate={navigate} />
      </div>

      <div>
        <SectionHeader
          eyebrow="RANKED"
          title="경쟁전 맵"
          sub={`${SOLO_RANKED_MAPS.length}개 맵 · 시즌 종료까지 유지`}
          accent="var(--color-brand-yellow-deep)"
        />
        <RankedGrid maps={SOLO_RANKED_MAPS} navigate={navigate} />
      </div>
    </div>
  );
}
