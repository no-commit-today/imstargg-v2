"use client";
import { useRouter } from "next/navigation";
import RenewalButton from "@/components/RenewalButton";

const ROLES = [
  { key: "PRESIDENT", ko: "회장", color: "#FFC60A", count: 1 },
  { key: "VICE_PRESIDENT", ko: "부회장", color: "#B83BFF", count: 4 },
  { key: "SENIOR", ko: "시니어", color: "#2E7CF6", count: 10 },
  { key: "MEMBER", ko: "멤버", color: "#8A90A0", count: 13 },
];

const NAMES = [
  "StarHunter",
  "GoldKing",
  "RoboGamer",
  "QuickDraw",
  "TankMaster",
  "PiperPro",
  "LeonOG",
  "SpikeStar",
  "GeneGenius",
  "MortKing",
  "EdgarPro",
  "PrimeKing",
  "RosaQueen",
  "JessieMain",
  "PennyArcade",
  "PamCare",
  "PocoMusic",
  "ByronHeal",
  "BoArcher",
  "MrPenguin",
];

interface Member {
  tag: string;
  name: string;
  trophies: number;
  roleKey: string;
  roleKo: string;
  roleColor: string;
  nameColor: string;
  isMe: boolean;
}

function Stat({
  label,
  value,
  icon,
}: {
  label: string;
  value: string;
  icon?: string;
}) {
  return (
    <div>
      <div
        style={{
          fontSize: 10,
          fontWeight: 700,
          color: "rgba(255,255,255,0.6)",
          letterSpacing: "0.06em",
          textTransform: "uppercase",
          marginBottom: 2,
        }}
      >
        {label}
      </div>
      <div
        className="num"
        style={{
          fontSize: 18,
          fontWeight: 800,
          color: "#fff",
          display: "flex",
          alignItems: "center",
          gap: 4,
        }}
      >
        {icon && (
          <img src={`/assets/${icon}`} alt="" style={{ width: 16, height: 16 }} />
        )}
        {value}
      </div>
    </div>
  );
}

export default function ClubDetailPage({ tag }: { tag: string }) {
  const router = useRouter();
  const navigate = (r: string) => router.push(r);
  const club = {
    tag: "#" + tag.replace("#", ""),
    name: "팀나인",
    description:
      "🔥 신규 멤버 환영! 트로피 30000+ 우대. 일일 클럽전 참여 필수, 친목 위주 클럽입니다. 디스코드 가입은 필수예요!",
    type: "INVITE_ONLY",
    requiredTrophies: 30000,
    trophies: 1284820,
    badgeId: 8000001,
  };

  const seed = (i: number) => Math.sin(tag.charCodeAt(0) + i * 7) * 10000;
  const r = (i: number) => seed(i) - Math.floor(seed(i));
  let cursor = 0;
  const members: Member[] = [];
  ROLES.forEach((role) => {
    for (let i = 0; i < role.count; i++) {
      members.push({
        tag: "#M" + (cursor + 1).toString().padStart(5, "0"),
        name: NAMES[i % NAMES.length] + (cursor < 5 ? "" : (cursor + 1).toString()),
        trophies: 95000 - cursor * 1800 - Math.floor(r(cursor) * 600),
        roleKey: role.key,
        roleKo: role.ko,
        roleColor: role.color,
        nameColor: cursor < 3 ? "#FFC60A" : "#fff",
        isMe: cursor === 4,
      });
      cursor++;
    }
  });

  return (
    <div className="main">
      <div className="card" style={{ overflow: "hidden", marginBottom: 14 }}>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "180px 1fr 240px",
            background: "linear-gradient(135deg, #2A2E38, #1A1D24)",
            color: "#fff",
          }}
        >
          <div
            style={{
              padding: 28,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              justifyContent: "center",
              gap: 8,
              borderRight: "1px solid rgba(255,255,255,0.06)",
            }}
          >
            <div
              style={{
                width: 110,
                height: 110,
                borderRadius: 16,
                background: "linear-gradient(135deg, #FFC60A, #FF8A1F)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                boxShadow: "0 6px 20px rgba(255,198,10,0.4)",
                position: "relative",
              }}
            >
              <img
                src="/assets/club_league.png"
                alt=""
                style={{ width: 70, height: 70 }}
              />
            </div>
            <div className="num" style={{ fontSize: 12, color: "rgba(255,255,255,0.7)" }}>
              {club.tag}
            </div>
          </div>

          <div style={{ padding: "24px 28px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6 }}>
              <span
                style={{
                  fontSize: 11,
                  fontWeight: 700,
                  letterSpacing: "0.08em",
                  textTransform: "uppercase",
                  color: "rgba(255,255,255,0.6)",
                }}
              >
                CLUB
              </span>
              <span
                style={{
                  background: "rgba(255,198,10,0.2)",
                  color: "var(--color-brand-yellow)",
                  padding: "2px 8px",
                  borderRadius: 4,
                  fontSize: 11,
                  fontWeight: 700,
                  letterSpacing: "0.04em",
                }}
              >
                가입 신청 필요
              </span>
            </div>
            <h1
              style={{
                margin: 0,
                fontFamily: "var(--font-display)",
                fontSize: 32,
                letterSpacing: "0.02em",
              }}
            >
              {club.name}
            </h1>
            <p
              style={{
                margin: "12px 0 0",
                color: "rgba(255,255,255,0.85)",
                fontSize: 13,
                lineHeight: 1.6,
                maxWidth: 560,
              }}
            >
              {club.description}
            </p>

            <div style={{ display: "flex", gap: 24, marginTop: 18 }}>
              <Stat label="총 트로피" value={club.trophies.toLocaleString()} icon="trophy.png" />
              <Stat label="멤버" value={`${members.length}/30`} />
              <Stat label="필요 트로피" value={club.requiredTrophies.toLocaleString()} />
              <Stat
                label="평균"
                value={Math.floor(
                  members.reduce((s, m) => s + m.trophies, 0) / members.length,
                ).toLocaleString()}
              />
            </div>
          </div>

          <div
            style={{
              padding: 24,
              display: "flex",
              flexDirection: "column",
              gap: 10,
              justifyContent: "center",
              borderLeft: "1px solid rgba(255,255,255,0.06)",
            }}
          >
            <div
              style={{
                padding: 12,
                background: "rgba(255,255,255,0.06)",
                borderRadius: 8,
                fontSize: 11,
                color: "rgba(255,255,255,0.7)",
                lineHeight: 1.6,
              }}
            >
              <strong style={{ color: "#fff", display: "block", marginBottom: 4 }}>
                📊 글로벌 랭킹
              </strong>
              상위{" "}
              <span
                className="num"
                style={{ color: "var(--color-brand-yellow)", fontWeight: 800, fontSize: 16 }}
              >
                #142
              </span>
              위
            </div>
            <div
              style={{
                padding: 12,
                background: "rgba(255,255,255,0.06)",
                borderRadius: 8,
                fontSize: 11,
                color: "rgba(255,255,255,0.7)",
                lineHeight: 1.6,
              }}
            >
              <strong style={{ color: "#fff", display: "block", marginBottom: 4 }}>
                🏛 클럽 타입
              </strong>
              <span style={{ color: "var(--color-brand-yellow)", fontWeight: 700 }}>
                가입 신청 필요
              </span>
            </div>

            <div style={{ marginTop: 6 }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: 6,
                }}
              >
                <span
                  style={{
                    fontSize: 10,
                    fontWeight: 700,
                    color: "rgba(255,255,255,0.6)",
                    letterSpacing: "0.06em",
                    textTransform: "uppercase",
                  }}
                >
                  마지막 갱신
                </span>
                <span className="num" style={{ fontSize: 11, color: "rgba(255,255,255,0.85)" }}>
                  5분 전
                </span>
              </div>
              <RenewalButton size="md" />
            </div>
          </div>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 320px", gap: 14 }}>
        <div className="card">
          <div className="card-head">
            <div className="card-title">멤버 ({members.length})</div>
            <div style={{ display: "flex", gap: 4 }}>
              {ROLES.map((r) => (
                <span
                  key={r.key}
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 4,
                    padding: "3px 8px",
                    borderRadius: 4,
                    fontSize: 11,
                    fontWeight: 700,
                    background: r.color + "22",
                    color: r.color,
                  }}
                >
                  <span
                    style={{ width: 6, height: 6, borderRadius: "50%", background: r.color }}
                  />
                  {r.ko} {r.count}
                </span>
              ))}
            </div>
          </div>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "50px 1fr 100px 120px 60px",
              padding: "8px 16px",
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
            <span>이름</span>
            <span>역할</span>
            <span style={{ textAlign: "right" }}>트로피</span>
            <span></span>
          </div>
          {members.map((m, i) => (
            <div
              key={m.tag}
              onClick={() => navigate("/players/" + m.tag.replace("#", ""))}
              style={{
                display: "grid",
                gridTemplateColumns: "50px 1fr 100px 120px 60px",
                padding: "8px 16px",
                gap: 10,
                alignItems: "center",
                borderBottom: "1px solid var(--color-neutral-100)",
                cursor: "pointer",
                background: m.isMe ? "rgba(255,198,10,0.08)" : "transparent",
              }}
              onMouseEnter={(e) => {
                if (!m.isMe) e.currentTarget.style.background = "var(--color-neutral-50)";
              }}
              onMouseLeave={(e) => {
                if (!m.isMe) e.currentTarget.style.background = "transparent";
              }}
            >
              <span className="num" style={{ color: "var(--color-neutral-400)", fontSize: 13 }}>
                {i + 1}
              </span>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <div
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: "50%",
                    background: `hsl(${(i * 47) % 360}, 60%, 55%)`,
                    color: "#fff",
                    fontFamily: "var(--font-display)",
                    fontWeight: 800,
                    fontSize: 12,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {m.name.slice(0, 1)}
                </div>
                <div>
                  <div
                    style={{
                      fontSize: 13,
                      fontWeight: 700,
                      color:
                        m.nameColor === "#FFC60A"
                          ? "var(--color-brand-yellow-deep)"
                          : "var(--color-neutral-900)",
                    }}
                  >
                    {m.name}
                    {m.isMe && (
                      <span className="pill yellow" style={{ marginLeft: 6, fontSize: 9 }}>
                        나
                      </span>
                    )}
                  </div>
                  <div className="num" style={{ fontSize: 10, color: "var(--color-neutral-500)" }}>
                    {m.tag}
                  </div>
                </div>
              </div>
              <span
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: 4,
                  padding: "3px 8px",
                  borderRadius: 4,
                  fontSize: 11,
                  fontWeight: 700,
                  background: m.roleColor + "22",
                  color: m.roleColor,
                  width: "fit-content",
                }}
              >
                <span
                  style={{
                    width: 6,
                    height: 6,
                    borderRadius: "50%",
                    background: m.roleColor,
                  }}
                />
                {m.roleKo}
              </span>
              <div style={{ textAlign: "right" }}>
                <span className="num" style={{ fontSize: 13, fontWeight: 800 }}>
                  <img
                    src="/assets/trophy.png"
                    alt=""
                    style={{
                      width: 12,
                      height: 12,
                      display: "inline",
                      verticalAlign: "middle",
                      marginRight: 3,
                    }}
                  />
                  {m.trophies.toLocaleString()}
                </span>
              </div>
              <button
                className="btn"
                style={{ fontSize: 10, padding: "4px 8px" }}
                onClick={(e) => {
                  e.stopPropagation();
                  navigate("/players/" + m.tag.replace("#", ""));
                }}
              >
                전적 →
              </button>
            </div>
          ))}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <div className="card">
            <div className="card-head">
              <div className="card-title">트로피 분포</div>
            </div>
            <div style={{ padding: 14, display: "flex", flexDirection: "column", gap: 6 }}>
              {(
                [
                  ["90k+", members.filter((m) => m.trophies >= 90000).length],
                  [
                    "80–90k",
                    members.filter((m) => m.trophies >= 80000 && m.trophies < 90000).length,
                  ],
                  [
                    "70–80k",
                    members.filter((m) => m.trophies >= 70000 && m.trophies < 80000).length,
                  ],
                  [
                    "60–70k",
                    members.filter((m) => m.trophies >= 60000 && m.trophies < 70000).length,
                  ],
                  ["<60k", members.filter((m) => m.trophies < 60000).length],
                ] as [string, number][]
              ).map(([label, n], i) => {
                const pct = (n / members.length) * 100;
                return (
                  <div key={label} style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <span
                      style={{ fontSize: 11, color: "var(--color-neutral-500)", width: 50 }}
                    >
                      {label}
                    </span>
                    <div
                      style={{
                        flex: 1,
                        height: 12,
                        background: "var(--color-neutral-100)",
                        borderRadius: 3,
                        overflow: "hidden",
                      }}
                    >
                      <div
                        style={{
                          width: `${pct}%`,
                          height: "100%",
                          background:
                            i === 0 ? "var(--color-brand-yellow)" : "var(--color-neutral-400)",
                        }}
                      />
                    </div>
                    <span
                      className="num"
                      style={{
                        fontSize: 11,
                        color: "var(--color-neutral-700)",
                        width: 28,
                        textAlign: "right",
                      }}
                    >
                      {n}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
