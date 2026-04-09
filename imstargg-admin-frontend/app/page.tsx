"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "./admin-layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getDashboardSummary } from "@/lib/api";
import type { DashboardSummary } from "@/lib/types";
import Link from "next/link";

function StatCard({
  title,
  href,
  stats,
}: {
  title: string;
  href: string;
  stats: { label: string; value: number; total: number }[];
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">
          <Link href={href} className="hover:underline">
            {title}
          </Link>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1 text-sm">
        {stats.map((s) => (
          <div key={s.label} className="flex justify-between">
            <span className="text-muted-foreground">{s.label}</span>
            <span>
              {s.value} / {s.total}
            </span>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);

  useEffect(() => {
    getDashboardSummary().then(setSummary);
  }, []);

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">대시보드</h2>
      {summary ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            title="브롤러"
            href="/brawlers"
            stats={[
              { label: "이름 등록", value: summary.brawlerWithName, total: summary.brawlerTotal },
              { label: "이미지 등록", value: summary.brawlerWithImage, total: summary.brawlerTotal },
            ]}
          />
          <StatCard
            title="기어"
            href="/gears"
            stats={[
              { label: "이름 등록", value: summary.gearWithName, total: summary.gearTotal },
              { label: "이미지 등록", value: summary.gearWithImage, total: summary.gearTotal },
            ]}
          />
          <StatCard
            title="이벤트 모드"
            href="/event-modes"
            stats={[
              { label: "이름 등록", value: summary.eventModeWithName, total: summary.eventModeTotal },
              { label: "아이콘 등록", value: summary.eventModeWithIcon, total: summary.eventModeTotal },
            ]}
          />
          <StatCard
            title="이벤트 맵"
            href="/events"
            stats={[
              { label: "이름 등록", value: summary.eventMapWithName, total: summary.eventMapTotal },
            ]}
          />
        </div>
      ) : (
        <p className="text-muted-foreground">로딩 중...</p>
      )}
    </AdminLayout>
  );
}
