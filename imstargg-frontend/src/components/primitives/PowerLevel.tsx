import Image from "next/image";

interface Props {
  level: number;
  size?: number;
}

export default function PowerLevel({ level, size = 22 }: Props) {
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <Image
        src="/assets/power_level_base_empty.svg"
        alt=""
        role="img"
        width={size}
        height={size}
        unoptimized
      />
      <span
        aria-label={`파워 레벨 ${level}`}
        style={{
          position: "absolute",
          inset: 0,
          display: "grid",
          placeItems: "center",
          color: "#FFFFFF",
          fontFamily: "var(--font-num)",
          fontWeight: 800,
          fontSize: size * 0.55,
          textShadow: "0 1px 2px rgba(0,0,0,0.4)",
        }}
      >
        {level}
      </span>
    </span>
  );
}
