"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Trophy from "@/components/primitives/Trophy";

const RECENT = [
  { tag: "#9R20YR2YQ", name: "StarHunter", trophies: 48820 },
  { tag: "#PR0G4M3R", name: "ProGamer22", trophies: 41200 },
  { tag: "#KING2024", name: "KingOfBrawl", trophies: 52100 },
];

export default function HomeScreen() {
  const router = useRouter();
  const [tag, setTag] = useState("");
  const [focused, setFocused] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setFocused(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, []);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = tag.trim().replace(/^#/, "").toUpperCase();
    if (cleaned) router.push(`/players/${cleaned}`);
  };

  return (
    <div
      style={{
        minHeight: "calc(100vh - 60px)",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "flex-start",
        paddingTop: 60,
        position: "relative",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          height: 480,
          backgroundImage: "url(/assets/lobby_bg.webp)",
          backgroundSize: "cover",
          backgroundPosition: "center",
          opacity: 0.08,
          zIndex: 0,
          maskImage: "linear-gradient(to bottom, black 30%, transparent 100%)",
          WebkitMaskImage: "linear-gradient(to bottom, black 30%, transparent 100%)",
        }}
      />

      <div
        style={{
          position: "relative",
          zIndex: 1,
          width: "100%",
          maxWidth: 720,
          textAlign: "center",
          padding: "0 24px",
        }}
      >
        <div
          style={{ display: "flex", justifyContent: "center", alignItems: "center", marginBottom: 18 }}
        >
          <img src="/assets/BS_KO_KR.webp" alt="브롤스타즈" style={{ height: 96 }} />
        </div>
        <h1
          style={{
            fontFamily: "var(--font-display)",
            fontSize: 56,
            letterSpacing: "0.04em",
            color: "var(--color-neutral-900)",
            margin: "0 0 8px 0",
            lineHeight: 1,
          }}
        >
          IMSTAR<span style={{ color: "var(--color-brand-yellow)" }}>GG</span>
        </h1>
        <p
          style={{
            color: "var(--color-neutral-500)",
            fontSize: 16,
            margin: "0 0 36px 0",
            fontWeight: 500,
          }}
        >
          브롤스타즈 전적 · 통계 · 메타 검색
        </p>

        <form onSubmit={submit} style={{ marginBottom: 16 }}>
          <div ref={wrapRef} style={{ position: "relative" }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                background: "#fff",
                border: "2px solid var(--color-neutral-900)",
                borderRadius: focused && RECENT.length ? "14px 14px 0 0" : 14,
                padding: "8px 8px 8px 20px",
                boxShadow: focused
                  ? "0 4px 0 rgba(0,0,0,0.08), 0 16px 40px rgba(20,22,27,0.16)"
                  : "0 4px 0 rgba(0,0,0,0.08), 0 12px 32px rgba(20,22,27,0.12)",
                transition: "all 0.15s",
                position: "relative",
                zIndex: 2,
              }}
            >
              <span style={{ color: "var(--color-neutral-400)", fontSize: 20 }}>🔍</span>
              <input
                autoFocus
                value={tag}
                onChange={(e) => setTag(e.target.value)}
                onFocus={() => setFocused(true)}
                placeholder="플레이어 태그를 입력하세요  (예: #9R20YR2YQ)"
                style={{
                  flex: 1,
                  border: "none",
                  outline: "none",
                  fontSize: 18,
                  fontFamily: "var(--font-mono)",
                  fontWeight: 500,
                  background: "transparent",
                  padding: "12px 0",
                  color: "var(--color-neutral-900)",
                }}
              />
              <button type="submit" className="btn btn-primary" style={{ padding: "12px 22px", fontSize: 14 }}>
                검색
              </button>
            </div>

            {focused && RECENT.length > 0 && (
              <div
                style={{
                  position: "absolute",
                  top: "calc(100% - 2px)",
                  left: 0,
                  right: 0,
                  background: "#fff",
                  border: "2px solid var(--color-neutral-900)",
                  borderTop: "1px solid var(--color-neutral-200)",
                  borderRadius: "0 0 14px 14px",
                  boxShadow: "0 12px 32px rgba(20,22,27,0.16)",
                  overflow: "hidden",
                  textAlign: "left",
                  zIndex: 1,
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "10px 18px 6px",
                  }}
                >
                  <div
                    style={{
                      fontSize: 10,
                      fontWeight: 700,
                      color: "var(--color-neutral-500)",
                      letterSpacing: "0.12em",
                      textTransform: "uppercase",
                    }}
                  >
                    최근 검색
                  </div>
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    style={{
                      background: "none",
                      border: "none",
                      color: "var(--color-neutral-500)",
                      fontSize: 11,
                      cursor: "pointer",
                      padding: 0,
                    }}
                  >
                    전체 삭제
                  </button>
                </div>
                {RECENT.map((p, i) => (
                  <div
                    key={p.tag}
                    onMouseDown={(e) => {
                      e.preventDefault();
                      router.push(`/players/${p.tag.replace("#", "")}`);
                    }}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "10px 18px",
                      cursor: "pointer",
                      borderTop: i === 0 ? "none" : "1px solid var(--color-neutral-100)",
                      transition: "background 0.12s",
                    }}
                    onMouseEnter={(e) =>
                      (e.currentTarget.style.background = "var(--color-neutral-50)")
                    }
                    onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                  >
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: "50%",
                        background: "linear-gradient(135deg, #FFC60A, #FF8A1F)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        color: "#fff",
                        fontFamily: "var(--font-display)",
                        fontSize: 14,
                      }}
                    >
                      {p.name[0]}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 700, fontSize: 14 }}>{p.name}</div>
                      <div
                        style={{
                          fontFamily: "var(--font-mono)",
                          fontSize: 11,
                          color: "var(--color-neutral-500)",
                        }}
                      >
                        {p.tag}
                      </div>
                    </div>
                    <Trophy value={p.trophies} size={13} />
                    <span style={{ color: "var(--color-neutral-400)", fontSize: 14 }}>→</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </form>

        <p style={{ color: "var(--color-neutral-500)", fontSize: 12 }}>
          게임 내 프로필에서 태그를 확인할 수 있어요
        </p>
      </div>

      <div
        style={{
          marginTop: "auto",
          padding: "40px 0 20px",
          color: "var(--color-neutral-400)",
          fontSize: 11,
          position: "relative",
          zIndex: 1,
        }}
      >
        ImStarGG는 Supercell과 무관한 비공식 팬 사이트입니다.
      </div>
    </div>
  );
}
