import Image from "next/image";
import { formatNumber } from "@/lib/format";

interface Props {
  value: number;
  size?: number;
}

export default function Trophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <Image
        src="/assets/icon_trophy.png"
        alt=""
        role="img"
        width={size}
        height={size}
        unoptimized
      />
      <span>{formatNumber(value)}</span>
    </span>
  );
}
