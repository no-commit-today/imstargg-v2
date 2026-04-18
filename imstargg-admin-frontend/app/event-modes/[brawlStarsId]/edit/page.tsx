"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getEventModes, updateEventModeMessages, uploadEventModeImage } from "@/lib/api";
import type { BattleEventMode } from "@/lib/types";
import { toast } from "sonner";

export default function EventModeEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [eventMode, setEventMode] = useState<BattleEventMode | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");

  const loadEventMode = useCallback(() => {
    getEventModes().then((modes) => {
      const found = modes.find((m) => m.brawlStarsId === brawlStarsId);
      if (found) {
        setEventMode(found);
        setKo(found.names.KOREAN || "");
        setEn(found.names.ENGLISH || "");
      }
    });
  }, [brawlStarsId]);

  useEffect(() => {
    loadEventMode();
  }, [loadEventMode]);

  const handleSaveMessages = async () => {
    try {
      await updateEventModeMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadEventMode();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleUploadImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadEventModeImage(brawlStarsId, file);
      toast.success("아이콘이 업로드되었습니다.");
      loadEventMode();
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  if (!eventMode) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">이벤트 모드 편집 - {brawlStarsId}</h2>

      <div className="space-y-6">
        <Card>
          <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>BrawlStars ID: {eventMode.brawlStarsId}</p>
            <p>BrawlStars Name: {eventMode.brawlStarsName}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>이름</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label>이름 (KO)</Label>
              <Input value={ko} onChange={(e) => setKo(e.target.value)} />
            </div>
            <div>
              <Label>이름 (EN)</Label>
              <Input value={en} onChange={(e) => setEn(e.target.value)} />
            </div>
            <Button onClick={handleSaveMessages}>이름 저장</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>아이콘</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {eventMode.iconImageUrl ? (
              <img src={eventMode.iconImageUrl} alt="" className="max-h-32" />
            ) : (
              <p className="text-sm text-muted-foreground">아이콘 없음</p>
            )}
            <Input type="file" accept="image/*" onChange={handleUploadImage} />
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
