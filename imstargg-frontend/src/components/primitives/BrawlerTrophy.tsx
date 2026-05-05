import { formatNumber } from "@/lib/format";

interface Props {
  value: number;
  size?: number;
}

function trophyIcon(value: number): string {
  if (value >= 3000) return "/assets/icon_prestige_trophy.webp";
  if (value >= 1000) return "/assets/icon_prestige_trophy_2.webp";
  return "/assets/icon_trophy.png";
}

export default function BrawlerTrophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <img src={trophyIcon(value)} alt="" role="img" width={size} height={size} />
      <span>{formatNumber(value)}</span>
    </span>
  );
}
