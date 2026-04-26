import type { CSSProperties } from "react";
import { CLS, type ClassKey } from "@/lib/data";

interface Props {
  cls: ClassKey;
  size?: number;
}

export default function ClassIcon({ cls, size = 18 }: Props) {
  const c = CLS[cls];
  if (!c) return null;
  const style: CSSProperties & Record<string, string | number> = {
    "--cls-color": c.color,
    width: size,
    height: size,
  };
  return (
    <span className="class-icon" style={style}>
      <img
        src={`/assets/${c.icon}`}
        alt={c.name}
        style={{ width: size * 0.65, height: size * 0.65 }}
      />
    </span>
  );
}
