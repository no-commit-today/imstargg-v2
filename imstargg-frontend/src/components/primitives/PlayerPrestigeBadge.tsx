interface Props {
  level: number;
  size?: number;
}

export default function PlayerPrestigeBadge({ level, size = 22 }: Props) {
  if (level < 1) return null;
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src="/assets/icon_total_prestige.webp" alt="" role="img" width={size} height={size} />
      <span
        aria-label={`프레스티지 ${level}`}
        style={{
          position: "absolute",
          inset: 0,
          display: "grid",
          placeItems: "center",
          color: "#FFFFFF",
          fontFamily: "var(--font-num)",
          fontWeight: 800,
          fontSize: size * 0.5,
          textShadow: "0 1px 2px rgba(0,0,0,0.5)",
        }}
      >
        {level}
      </span>
    </span>
  );
}
