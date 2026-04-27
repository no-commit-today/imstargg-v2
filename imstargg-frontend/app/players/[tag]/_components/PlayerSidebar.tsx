"use client";
import { useEffect, useState } from "react";
import {
  PLAYER,
  PLAYER_STATS,
  OWNED_BRAWLERS,
  BRAWLERS,
  trophyTier,
} from "@/lib/data";
import {
  getRenewalState,
  subscribeRenewal,
  triggerRenewalError,
  type RenewalState,
} from "@/lib/renewal";
import { toast } from "sonner";
import { RenewalButton } from "@/components/shared/RenewalButton";

function StatRow({
  icon,
  label,
  value,
  sub,
  accent,
  subColor,
}: {
  icon?: "trophy" | "ranked";
  label: string;
  value: string;
  sub?: string;
  accent?: "yellow" | "red";
  subColor?: "green";
}) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        padding: "10px 18px",
        borderTop: "1px solid var(--color-neutral-100)",
      }}
    >
      {icon === "trophy" && (
        <img src="/assets/trophy.png" style={{ width: 16, height: 16, marginRight: 8 }} alt="" />
      )}
      {icon === "ranked" && (
        <img src="/assets/ranked.png" style={{ width: 16, height: 16, marginRight: 8 }} alt="" />
      )}
      <span style={{ flex: 1, fontSize: 13, color: "var(--color-neutral-700)" }}>{label}</span>
      <div style={{ textAlign: "right" }}>
        <div
          className="num"
          style={{
            fontSize: 14,
            color:
              accent === "yellow"
                ? "var(--color-brand-yellow-deep)"
                : accent === "red"
                  ? "var(--color-accent-red-deep)"
                  : "var(--color-neutral-900)",
          }}
        >
          {value}
        </div>
        {sub && (
          <div
            style={{
              fontSize: 11,
              fontFamily: "var(--font-num)",
              color: subColor === "green" ? "var(--color-accent-green)" : "var(--color-neutral-400)",
              fontWeight: 600,
            }}
          >
            {sub}
          </div>
        )}
      </div>
    </div>
  );
}

function RenewedAtLabel() {
  const [state, setState] = useState<RenewalState>(getRenewalState());
  useEffect(() => subscribeRenewal(setState), []);
  return <span>{state.lastRenewedAt}</span>;
}

export default function PlayerSidebar() {
  const player = PLAYER;
  const tier = trophyTier(player.trophies);
  const totals = (() => {
    let w = 0,
      l = 0;
    Object.values(PLAYER_STATS).forEach((s) => {
      w += s.winCount;
      l += s.lossCount;
    });
    return { winRate: w / (w + l), totalBattles: w + l };
  })();
  const totalBattles = Object.values(PLAYER_STATS).reduce((a, s) => a + s.battleCount, 0);

  return (
    <aside style={{ width: 320, flexShrink: 0 }}>
      <div className="card" style={{ overflow: "hidden" }}>
        <div
          style={{
            height: 100,
            background: "linear-gradient(135deg, #FFC60A 0%, #FF8A1F 100%)",
            position: "relative",
            overflow: "hidden",
          }}
        >
          <img
            src="/assets/star.webp"
            style={{
              position: "absolute",
              right: -30,
              top: -20,
              width: 160,
              opacity: 0.3,
              transform: "rotate(15deg)",
            }}
            alt=""
          />
          <div
            style={{
              position: "absolute",
              top: 12,
              left: 14,
              display: "flex",
              alignItems: "center",
              gap: 6,
              background: "rgba(0,0,0,0.4)",
              padding: "4px 10px",
              borderRadius: 999,
              color: "#fff",
              fontSize: 11,
              fontWeight: 700,
              backdropFilter: "blur(4px)",
            }}
          >
            <img src={`/assets/${tier.img}`} style={{ width: 16, height: 16 }} alt="" />
            <span>{tier.name}</span>
          </div>
        </div>

        <div style={{ padding: "0 18px 18px", position: "relative" }}>
          <div
            style={{
              width: 88,
              height: 88,
              borderRadius: 18,
              background: "linear-gradient(135deg, #FFE082, #FFC60A)",
              border: "4px solid #fff",
              marginTop: -52,
              marginBottom: 12,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontFamily: "var(--font-display)",
              fontSize: 38,
              color: "var(--color-neutral-900)",
              boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
            }}
          >
            {player.name[0]}
          </div>

          <div
            style={{
              fontSize: 22,
              fontWeight: 800,
              color: player.nameColor,
              fontFamily: "var(--font-body)",
            }}
          >
            {player.name}
          </div>
          <div
            style={{
              fontFamily: "var(--font-mono)",
              fontSize: 12,
              color: "var(--color-neutral-500)",
              marginTop: 2,
              display: "flex",
              alignItems: "center",
              gap: 6,
            }}
          >
            <span>{player.tag}</span>
            <button
              onClick={() => {
                navigator.clipboard?.writeText(player.tag);
                toast.success("태그가 복사되었어요", { duration: 1500 });
              }}
              style={{
                background: "none",
                border: "none",
                color: "var(--color-neutral-400)",
                cursor: "pointer",
                fontSize: 12,
              }}
              title="태그 복사"
            >
              📋
            </button>
          </div>

          <div
            style={{
              marginTop: 12,
              display: "flex",
              alignItems: "center",
              gap: 8,
              padding: "8px 10px",
              background: "var(--color-neutral-50)",
              borderRadius: 8,
              fontSize: 13,
            }}
          >
            <img src="/assets/club_league.png" style={{ width: 22, height: 22 }} alt="" />
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 700 }}>{player.club.name}</div>
              <div
                style={{
                  fontFamily: "var(--font-mono)",
                  fontSize: 11,
                  color: "var(--color-neutral-500)",
                }}
              >
                {player.club.tag}
              </div>
            </div>
            <span style={{ color: "var(--color-neutral-400)" }}>→</span>
          </div>

          <div style={{ marginTop: 14 }}>
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
                  fontSize: 11,
                  fontWeight: 700,
                  color: "var(--color-neutral-500)",
                  letterSpacing: "0.06em",
                  textTransform: "uppercase",
                }}
              >
                마지막 갱신
              </span>
              <span style={{ fontSize: 12, color: "var(--color-neutral-700)", fontWeight: 600 }}>
                <RenewedAtLabel />
              </span>
            </div>
            <RenewalButton kind="player" tag={PLAYER.tag} />
            <button
              onClick={() => toast.error(triggerRenewalError())}
              style={{
                background: "none",
                border: "none",
                color: "var(--color-neutral-400)",
                fontSize: 11,
                marginTop: 4,
                cursor: "pointer",
              }}
              title="에러 상태 시연"
            >
              ⓘ 에러 시연
            </button>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="card-head">
          <div className="card-title">프로필 통계</div>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <StatRow
            icon="trophy"
            label="현재 트로피"
            value={player.trophies.toLocaleString()}
            accent="yellow"
          />
          <StatRow
            label="최고 트로피"
            value={player.highestTrophies.toLocaleString()}
            sub={`+${(player.highestTrophies - player.trophies).toLocaleString()}`}
          />
          <StatRow
            label="총 전적 / 승률"
            value={`${totalBattles.toLocaleString()}전`}
            sub={`${(totals.winRate * 100).toFixed(1)}%`}
            subColor="green"
          />
          <StatRow
            label="레벨"
            value={`Lv.${player.expLevel}`}
            sub={`${player.expPoints.toLocaleString()} XP`}
          />
          <StatRow label="3v3 승리" value={player.trioVictories.toLocaleString()} />
          <StatRow label="듀오 승리" value={player.duoVictories.toLocaleString()} />
          <StatRow label="솔로 승리" value={player.soloVictories.toLocaleString()} />
        </div>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="card-head">
          <div className="card-title">보유 브롤러</div>
          <span
            className="num"
            style={{ fontSize: 13, color: "var(--color-brand-yellow-deep)" }}
          >
            {OWNED_BRAWLERS.length}{" "}
            <span style={{ color: "var(--color-neutral-400)", fontWeight: 400 }}>
              / {BRAWLERS.length}
            </span>
          </span>
        </div>
        <div className="card-body">
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
                width: `${(OWNED_BRAWLERS.length / BRAWLERS.length) * 100}%`,
                height: "100%",
                background: "linear-gradient(90deg, #FFC60A, #FF8A1F)",
              }}
            />
          </div>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              marginTop: 8,
              fontSize: 11,
              color: "var(--color-neutral-500)",
            }}
          >
            <span>P11 브롤러: {OWNED_BRAWLERS.filter((b) => b.power === 11).length}명</span>
            <span>
              하이퍼차지: {OWNED_BRAWLERS.filter((b) => b.hasHypercharge).length}개
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
}
