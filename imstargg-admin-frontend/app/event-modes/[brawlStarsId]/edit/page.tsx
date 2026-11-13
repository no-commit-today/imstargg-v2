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
  updateEventModeMessages,
  updateEventModeStatsType,
  uploadEventModeImage,
} from "@/lib/api";
import {
  STATS_EVENT_MODE_TYPE_LABELS,
  type BattleEventMode,
  type StatsEventModeType,
} from "@/lib/types";
import { toast } from "sonner";
import { ImageUploadField } from "@/components/image-upload-field";

// Select 는 null 값을 담지 못하므로 "수집 안 함" 은 이 값으로 표현하고 저장할 때 null 로 바꾼다.
const NONE = "NONE";

const STATS_TYPE_ITEMS: Record<string, string> = {
  [NONE]: "수집 안 함",
  ...STATS_EVENT_MODE_TYPE_LABELS,
};

export default function EventModeEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [eventMode, setEventMode] = useState<BattleEventMode | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");
  const [statsType, setStatsType] = useState<string>(NONE);

  const loadEventMode = useCallback(() => {
    getEventModes().then((modes) => {
      const found = modes.find((m) => m.brawlStarsId === brawlStarsId);
      if (found) {
        setEventMode(found);
        setKo(found.names.KOREAN || "");
        setEn(found.names.ENGLISH || "");
        setStatsType(found.statsType ?? NONE);
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

  const handleSaveStatsType = async () => {
    try {
      await updateEventModeStatsType(
        brawlStarsId,
        statsType === NONE ? null : (statsType as StatsEventModeType),
      );
      toast.success(
        "통계 수집 유형이 저장되었습니다. 수집 서버에는 최대 5분 뒤 반영되고, 켠 모드의 통계는 다음 날 배치 뒤부터 보입니다.",
      );
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

        <Card>
          <CardHeader>
            <CardTitle>통계 수집</CardTitle>
            <p className="text-sm text-muted-foreground">
              팀 구성이 다른 모드에 유형을 매기면 그 모드는 수집되지 않습니다.
            </p>
          </CardHeader>
          <CardContent className="space-y-3">
            <Select
              value={statsType}
              items={STATS_TYPE_ITEMS}
              onValueChange={(v) => setStatsType(v ?? NONE)}
            >
              <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                {Object.entries(STATS_TYPE_ITEMS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>{label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button onClick={handleSaveStatsType}>저장</Button>
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
