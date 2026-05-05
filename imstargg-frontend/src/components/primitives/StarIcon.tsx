import { Star } from "lucide-react";

interface Props {
  size?: number;
  label?: string;
  className?: string;
}

export default function StarIcon({ size = 14, label = "스타플레이어", className }: Props) {
  return (
    <Star
      size={size}
      fill="#FFC60A"
      stroke="#FFC60A"
      aria-label={label}
      className={className}
    />
  );
}
