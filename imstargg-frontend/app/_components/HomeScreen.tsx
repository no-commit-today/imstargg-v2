"use client";

import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";

export default function HomeScreen() {
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
          style={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            marginBottom: 18,
          }}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
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

        <div className="flex justify-center mb-4">
          <PlayerSearchInput />
        </div>

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
