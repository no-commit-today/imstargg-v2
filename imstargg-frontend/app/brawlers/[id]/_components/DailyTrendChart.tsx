"use client";

import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { DailyResultStat } from "@/domain/statistics/types";

type Props = { items: DailyResultStat[] };

export function DailyTrendChart({ items }: Props) {
  if (items.length === 0) {
    return (
      <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-12 text-center text-sm text-[#5C6271]">
        일별 통계 데이터가 없습니다.
      </div>
    );
  }
  const data = items.map((d) => ({
    date: d.date.slice(5),
    winRate: Number((d.winRate * 100).toFixed(1)),
    starPlayerRate: Number((d.starPlayerRate * 100).toFixed(1)),
    totalBattleCount: d.totalBattleCount,
  }));
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white p-4">
      <h3 className="mb-2 text-sm font-bold">일별 트렌드</h3>
      <div style={{ height: 240 }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
            <CartesianGrid stroke="#ECEEF3" strokeDasharray="3 3" />
            <XAxis dataKey="date" stroke="#5C6271" fontSize={11} />
            <YAxis stroke="#5C6271" fontSize={11} unit="%" domain={[0, 100]} />
            <Tooltip
              formatter={(v: number, name: string) => [`${v}%`, name === "winRate" ? "승률" : "MVP"]}
            />
            <Line type="monotone" dataKey="winRate" stroke="#FFC60A" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="starPlayerRate" stroke="#9146FF" strokeWidth={2} strokeDasharray="4 4" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
