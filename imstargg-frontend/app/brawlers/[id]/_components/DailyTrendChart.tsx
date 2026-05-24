"use client";

import { CartesianGrid, Line, LineChart, Tooltip, XAxis, YAxis } from "recharts";
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  type ChartConfig,
} from "@/components/ui/chart";
import type { DailyResultStat } from "@/domain/statistics/types";
import StarIcon from "@/components/primitives/StarIcon";

type Props = { items: DailyResultStat[] };

const chartConfig: ChartConfig = {
  winRate: { label: "승률", color: "#FFC60A" },
  starPlayerRate: { label: <StarIcon size={12} />, color: "#9146FF" },
};

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
      <ChartContainer config={chartConfig} className="aspect-auto h-60 w-full">
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
          <CartesianGrid stroke="#ECEEF3" strokeDasharray="3 3" />
          <XAxis dataKey="date" stroke="#5C6271" fontSize={11} />
          <YAxis stroke="#5C6271" fontSize={11} unit="%" domain={[0, 100]} />
          <Tooltip
            formatter={(v, name) => [`${v}%`, name === "winRate" ? "승률" : "스타"]}
          />
          <ChartLegend content={<ChartLegendContent />} verticalAlign="top" />
          <Line type="monotone" dataKey="winRate" stroke="var(--color-winRate)" strokeWidth={2} dot={false} />
          <Line type="monotone" dataKey="starPlayerRate" stroke="var(--color-starPlayerRate)" strokeWidth={2} strokeDasharray="4 4" dot={false} />
        </LineChart>
      </ChartContainer>
    </div>
  );
}
