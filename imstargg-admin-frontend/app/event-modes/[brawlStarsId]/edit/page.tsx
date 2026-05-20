"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  getEventModes,
  updateEventMode,
  updateEventModeMessages,
  uploadEventModeImage,
} from "@/lib/api";
import type { BattleEventMode, BattleEventModeResultType } from "@/lib/types";
import { BATTLE_EVENT_MODE_RESULT_TYPES } from "@/lib/types";
import { toast } from "sonner";
import { ImageUploadField } from "@/components/image-upload-field";

export default function EventModeEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [eventMode, setEventMode] = useState<BattleEventMode | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");
  const [resultType, setResultType] = useState<BattleEventModeResultType>("RESULT");
  const [playerCounts, setPlayerCounts] = useState<string>("");

  const loadEventMode = useCallback(() => {
    getEventModes().then((modes) => {
      const found = modes.find((m) => m.brawlStarsId === brawlStarsId);
      if (found) {
        setEventMode(found);
        setKo(found.names.KOREAN || "");
        setEn(found.names.ENGLISH || "");
        setResultType(found.resultType);
        setPlayerCounts(found.playerCounts.join(","));
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

  const parsePlayerCounts = (): number[] | null => {
    const parsed = playerCounts
      .split(",")
      .map((s) => s.trim())
      .filter((s) => s.length > 0)
      .map((s) => Number(s));
    if (parsed.length === 0 || parsed.some((n) => !Number.isInteger(n) || n < 1)) {
      return null;
    }
    return parsed;
  };

  const handleSaveResultType = async () => {
    if (!eventMode) return;
    try {
      await updateEventMode(brawlStarsId, { resultType, playerCounts: eventMode.playerCounts });
      toast.success("결과 타입이 저장되었습니다.");
      loadEventMode();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleSavePlayerCounts = async () => {
    const parsed = parsePlayerCounts();
    if (parsed === null) {
      toast.error("팀 인원수는 1 이상의 정수 리스트여야 합니다.");
      return;
    }
    try {
      await updateEventMode(brawlStarsId, { resultType, playerCounts: parsed });
      toast.success("플레이어 인원수가 저장되었습니다.");
      loadEventMode();
    } catch {
      toast.error("저장에 실패했습니다.");
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
          <CardHeader><CardTitle>결과 타입</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label>Result Type</Label>
              <Select
                value={resultType}
                onValueChange={(v) => setResultType((v ?? "RESULT") as BattleEventModeResultType)}
              >
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {BATTLE_EVENT_MODE_RESULT_TYPES.map((r) => (
                    <SelectItem key={r} value={r}>{r}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button onClick={handleSaveResultType}>결과 타입 저장</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>플레이어 인원수</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label>Player Counts</Label>
              <Input
                value={playerCounts}
                onChange={(e) => setPlayerCounts(e.target.value)}
                placeholder="3,3"
              />
              <p className="mt-1 text-xs text-muted-foreground">
                각 팀의 인원 수를 쉼표로 구분. 예: 3v3 → 3,3 / Solo Showdown → 1,1,1,1,1,1,1,1,1,1
              </p>
            </div>
            <Button onClick={handleSavePlayerCounts}>플레이어 인원수 저장</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>아이콘</CardTitle></CardHeader>
          <CardContent>
            <ImageUploadField
              currentImageUrl={eventMode.iconImageUrl}
              onUpload={(file) => uploadEventModeImage(brawlStarsId, file)}
              onUploaded={loadEventMode}
              emptyText="아이콘 없음"
              successMessage="아이콘이 업로드되었습니다."
            />
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
