"use client";
import { useEffect, useState } from "react";
import { getRenewalState, startRenewal, subscribeRenewal, type RenewalState } from "@/lib/renewal";

interface Props {
  size?: "sm" | "md" | "lg";
  compact?: boolean;
}

export default function RenewalButton({ size = "md", compact = false }: Props) {
  const [state, setState] = useState<RenewalState>(getRenewalState());
  useEffect(() => subscribeRenewal(setState), []);

  const handleClick = () => {
    if (state.status === "IDLE") startRenewal();
  };

  const padding = size === "lg" ? "10px 18px" : size === "sm" ? "6px 10px" : "8px 14px";
  const fontSize = size === "lg" ? 14 : size === "sm" ? 12 : 13;

  let label: React.ReactNode = null;
  let icon: React.ReactNode = "⟳";
  let classes = "btn";
  let disabled = false;
  let extra: React.ReactNode = null;

  if (state.status === "IDLE") {
    label = compact ? null : "전적 갱신";
    classes = "btn btn-primary";
  } else if (state.status === "RENEWING") {
    label = compact ? null : "갱신중…";
    icon = <span className="spinner" />;
    disabled = true;
    extra = (
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          height: 3,
          background: "rgba(255,198,10,0.2)",
          borderRadius: "0 0 7px 7px",
          overflow: "hidden",
        }}
      >
        <div
          style={{
            width: `${state.progressPct}%`,
            height: "100%",
            background: "var(--color-brand-yellow)",
            transition: "width 0.2s",
          }}
        />
      </div>
    );
  } else if (state.status === "COOLDOWN") {
    const m = Math.floor(state.cooldownSecondsLeft / 60);
    const s = state.cooldownSecondsLeft % 60;
    label = compact ? null : `${m}:${s.toString().padStart(2, "0")} 후`;
    disabled = true;
  } else if (state.status === "JUST_DONE") {
    return (
      <button
        className="btn"
        disabled
        style={{
          padding,
          fontSize,
          position: "relative",
          background: "rgba(43,213,118,0.15)",
          color: "#1a9550",
          border: "1px solid rgba(43,213,118,0.3)",
          opacity: 1,
          cursor: "default",
        }}
      >
        <span style={{ fontWeight: 800 }}>✓</span>
        {!compact && <span>갱신 완료</span>}
      </button>
    );
  }

  return (
    <button
      className={classes}
      onClick={handleClick}
      disabled={disabled}
      style={{ padding, fontSize, position: "relative" }}
      title={
        state.status === "COOLDOWN"
          ? `${state.cooldownSecondsLeft}초 후에 다시 갱신할 수 있어요`
          : "플레이어 데이터를 갱신합니다"
      }
    >
      <span style={{ display: "inline-flex", alignItems: "center" }}>{icon}</span>
      {label && <span>{label}</span>}
      {extra}
    </button>
  );
}
