"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getGears, updateGearMessages, uploadGearImage } from "@/lib/api";
import type { Gear } from "@/lib/types";
import { toast } from "sonner";

export default function GearEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [gear, setGear] = useState<Gear | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");

  const loadGear = useCallback(() => {
    getGears().then((gears) => {
      const found = gears.find((g) => g.brawlStarsId === brawlStarsId);
      if (found) {
        setGear(found);
        setKo(found.names.KOREAN || "");
        setEn(found.names.ENGLISH || "");
      }
    });
  }, [brawlStarsId]);

  useEffect(() => {
    loadGear();
  }, [loadGear]);

  const handleSaveMessages = async () => {
    try {
      await updateGearMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadGear();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleUploadImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadGearImage(brawlStarsId, file);
      toast.success("이미지가 업로드되었습니다.");
      loadGear();
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  if (!gear) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">기어 편집 - {brawlStarsId}</h2>

      <div className="space-y-6">
        <Card>
          <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>BrawlStars ID: {gear.brawlStarsId}</p>
            <p>Rarity: {gear.rarity}</p>
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
          <CardHeader><CardTitle>이미지</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {gear.hasImage ? (
              <p className="text-sm text-muted-foreground">이미지 있음</p>
            ) : (
              <p className="text-sm text-muted-foreground">이미지 없음</p>
            )}
            <Input type="file" accept="image/*" onChange={handleUploadImage} />
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
