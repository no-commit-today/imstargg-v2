interface Props {
  trophies: number;
  size?: number;
}

const STEPS = [
  { min: 3000, src: "/assets/icon_trophy_brawler_prestige_3.webp", overlay: "3" },
  { min: 2000, src: "/assets/icon_trophy_brawler_prestige_2.webp", overlay: "2" },
  { min: 1000, src: "/assets/icon_trophy_brawler_prestige_1.webp", overlay: "1" },
  { min: 750, src: "/assets/icon_trophy_brawler_gold.webp", overlay: null as string | null },
  { min: 500, src: "/assets/icon_trophy_brawler_silver.webp", overlay: null },
  { min: 250, src: "/assets/icon_trophy_brawler_bronze.webp", overlay: null },
  { min: 0, src: "/assets/icon_trophy_brawler_wood.webp", overlay: null },
];

export default function PrestigeBadge({ trophies, size = 18 }: Props) {
  const step = STEPS.find((s) => trophies >= s.min)!;
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <img src={step.src} alt="" role="img" width={size} height={size} />
      {step.overlay && (
        <span
          style={{
            position: "absolute",
            inset: 0,
            display: "grid",
            placeItems: "center",
            color: "#FFFFFF",
            fontFamily: "var(--font-num)",
            fontWeight: 800,
            fontSize: size * 0.55,
            textShadow: "0 1px 2px rgba(0,0,0,0.5)",
          }}
        >
          {step.overlay}
        </span>
      )}
    </span>
  );
}
