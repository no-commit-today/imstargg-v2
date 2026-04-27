"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { fmtNum } from "@/lib/data";

const RECENT = [
  { tag: "#TEAMNINE", name: "팀나인", trophies: 1284820, members: 28 },
  { tag: "#BRAWLKR", name: "브롤코리아", trophies: 1102400, members: 30 },
  { tag: "#KOPRIDE", name: "KO PRIDE", trophies: 984300, members: 25 },
  { tag: "#STARGUILD", name: "스타길드", trophies: 847200, members: 30 },
];

export default function ClubSearchPage() {
  const router = useRouter();
  const [tag, setTag] = useState("");

  const submit = (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    const cleaned = tag.trim().replace(/^#/, "").toUpperCase();
    if (cleaned) router.push(`/clubs/${cleaned}`);
  };

  return (
    <div className="main" style={{ maxWidth: 640, paddingTop: 56 }}>
      <div style={{ marginBottom: 22 }}>
        <div
          style={{
            fontSize: 11,
            fontWeight: 700,
            color: "var(--color-neutral-500)",
            letterSpacing: "0.12em",
            textTransform: "uppercase",
            marginBottom: 8,
          }}
        >
          CLUBS
        </div>
        <h1
          style={{
            margin: 0,
            fontFamily: "var(--font-display)",
            fontSize: 28,
            letterSpacing: "0.02em",
            color: "var(--color-neutral-900)",
          }}
        >
          클럽 검색
        </h1>
        <p
          style={{
            margin: "6px 0 0",
            color: "var(--color-neutral-500)",
            fontSize: 13,
          }}
        >
          클럽 태그로 멤버 · 트로피 · 활동 내역을 조회하세요.
        </p>
      </div>

      <form onSubmit={submit} style={{ marginBottom: 28 }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            background: "#fff",
            border: "1px solid var(--color-neutral-300)",
            borderRadius: 10,
            padding: "4px 4px 4px 14px",
            transition: "border-color 0.15s, box-shadow 0.15s",
          }}
        >
          <span
            style={{
              fontFamily: "var(--font-mono)",
              color: "var(--color-neutral-400)",
              fontSize: 16,
              fontWeight: 700,
            }}
          >
            #
          </span>
          <input
            autoFocus
            value={tag}
            onChange={(e) => setTag(e.target.value)}
            placeholder="클럽 태그 입력  (예: TEAMNINE)"
            style={{
              flex: 1,
              border: "none",
              outline: "none",
              fontSize: 15,
              fontFamily: "var(--font-mono)",
              fontWeight: 500,
              background: "transparent",
              padding: "10px 0",
              color: "var(--color-neutral-900)",
              letterSpacing: "0.02em",
            }}
          />
          <button
            type="submit"
            className="btn btn-primary"
            style={{ padding: "8px 18px", fontSize: 13 }}
          >
            검색
          </button>
        </div>
      </form>

      <div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: 8,
          }}
        >
          <div
            style={{
              fontSize: 11,
              fontWeight: 700,
              color: "var(--color-neutral-500)",
              letterSpacing: "0.12em",
              textTransform: "uppercase",
            }}
          >
            최근 검색
          </div>
          <button
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
        <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          {RECENT.map((c) => (
            <div
              key={c.tag}
              onClick={() => router.push(`/clubs/${c.tag.replace("#", "")}`)}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "10px 14px",
                background: "#fff",
                border: "1px solid var(--color-neutral-200)",
                borderRadius: 8,
                cursor: "pointer",
                transition: "all 0.12s",
              }}
              onMouseEnter={(e) =>
                (e.currentTarget.style.borderColor = "var(--color-neutral-900)")
              }
              onMouseLeave={(e) =>
                (e.currentTarget.style.borderColor = "var(--color-neutral-200)")
              }
            >
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 6,
                  background: "linear-gradient(135deg, #FFC60A, #FF8A1F)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "#fff",
                  fontFamily: "var(--font-display)",
                  fontSize: 14,
                  fontWeight: 800,
                  textShadow: "0 1px 1px rgba(0,0,0,0.25)",
                }}
              >
                {c.name.slice(0, 1)}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{c.name}</div>
                <div
                  style={{
                    fontFamily: "var(--font-mono)",
                    fontSize: 11,
                    color: "var(--color-neutral-500)",
                  }}
                >
                  {c.tag}
                </div>
              </div>
              <div style={{ textAlign: "right" }}>
                <div
                  className="num"
                  style={{ fontSize: 13, fontWeight: 700, color: "var(--color-neutral-800)" }}
                >
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
                  {fmtNum(c.trophies)}
                </div>
                <div style={{ fontSize: 10, color: "var(--color-neutral-500)", marginTop: 1 }}>
                  {c.members}/30 멤버
                </div>
              </div>
              <span style={{ color: "var(--color-neutral-400)", fontSize: 14 }}>→</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
