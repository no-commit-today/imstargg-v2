interface Props {
  level: number;
}

export default function PowerPill({ level }: Props) {
  const isMax = level === 11;
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 2,
        padding: "2px 6px",
        background: isMax ? "linear-gradient(135deg, #FFC60A, #FF8A1F)" : "#2A2E38",
        color: "#fff",
        fontFamily: "var(--font-num)",
        fontWeight: 800,
        fontSize: 11,
        borderRadius: 4,
        letterSpacing: "0.02em",
      }}
    >
      <span style={{ opacity: 0.8 }}>P</span>
      {level}
    </span>
  );
}
