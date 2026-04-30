import type { CSSProperties } from "react";

interface Props {
  name: string;
  size?: number;
  className?: string;
}

export function PlayerAvatar({ name, size = 80, className = "" }: Props) {
  const initial = (name ?? "?").slice(0, 1) || "?";
  const fontSize = Math.max(16, size * 0.4);
  const style: CSSProperties = { width: size, height: size, fontSize };
  return (
    <div
      className={`flex items-center justify-center rounded-md bg-gradient-to-br from-[#FFC60A] to-[#FF8A1F] font-display font-extrabold text-white ${className}`}
      style={style}
      title={name}
    >
      {initial}
    </div>
  );
}
