"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getEvents, updateEventMessages } from "@/lib/api";
import type { BattleEvent } from "@/lib/types";
import { toast } from "sonner";

export default function EventEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [event, setEvent] = useState<BattleEvent | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");

  const loadEvent = useCallback(() => {
    getEvents().then((events) => {
      const found = events.find((e) => e.brawlStarsId === brawlStarsId);
      if (found) {
        setEvent(found);
        setKo(found.mapNames.KOREAN || "");
        setEn(found.mapNames.ENGLISH || "");
      }
    });
  }, [brawlStarsId]);

  useEffect(() => {
    loadEvent();
  }, [loadEvent]);

  const handleSaveMessages = async () => {
    try {
      await updateEventMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("맵 이름이 저장되었습니다.");
      loadEvent();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  if (!event) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">이벤트 편집 - {brawlStarsId}</h2>

      <div className="space-y-6">
        <Card>
          <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>BrawlStars ID: {event.brawlStarsId}</p>
            <p>모드: {event.modeBrawlStarsName || "-"}</p>
            <p>맵 이름: {event.mapName || "-"}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>맵 이름 번역</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label>맵 이름 (KO)</Label>
              <Input value={ko} onChange={(e) => setKo(e.target.value)} />
            </div>
            <div>
              <Label>맵 이름 (EN)</Label>
              <Input value={en} onChange={(e) => setEn(e.target.value)} />
            </div>
            <Button onClick={handleSaveMessages}>저장</Button>
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
