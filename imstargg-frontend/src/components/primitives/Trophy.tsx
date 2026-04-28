import { fmtNum } from "@/lib/data";

interface Props {
  value: number;
  size?: number;
}

export default function Trophy({ value, size = 14 }: Props) {
  return (
    <span className="trophy" style={{ fontSize: size }}>
      <img src="/assets/trophy.png" alt="" style={{ width: size, height: size }} />
      <span>{fmtNum(value)}</span>
    </span>
  );
}
