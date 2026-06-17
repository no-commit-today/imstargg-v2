"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminLayout } from "../../admin-layout";
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
import { getGears, createBrawler, uploadBrawlerImage } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { PendingImageInput } from "@/components/pending-image-input";
import type { Gear } from "@/lib/types";
import { toast } from "sonner";

const RARITIES = [
  "STARTING_BRAWLER",
  "RARE",
  "SUPER_RARE",
  "EPIC",
  "MYTHIC",
  "LEGENDARY",
  "ULTRA_LEGENDARY",
];

const ROLES = [
  "TANK",
  "ASSASSIN",
  "SUPPORT",
  "CONTROLLER",
  "DAMAGE_DEALER",
  "MARKSMAN",
  "ARTILLERY",
];

export default function BrawlerCreatePage() {
  const router = useRouter();
  const [gears, setGears] = useState<Gear[]>([]);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", role: "", ko: "", en: "" });
  const [selectedGearIds, setSelectedGearIds] = useState<number[]>([]);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getGears().then(setGears);
  }, []);

  const toggleGear = (id: number) => {
    setSelectedGearIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id],
    );
  };

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity || !form.role) {
      toast.error("ID, rarity, role을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    try {
      await createBrawler({
        brawlStarsId: id,
        rarity: form.rarity,
        role: form.role,
        names: { KOREAN: form.ko, ENGLISH: form.en },
        gearBrawlStarsIds: selectedGearIds,
      });
    } catch {
      toast.error("등록에 실패했습니다.");
      setSubmitting(false);
      return;
    }
    if (imageFile) {
      try {
        await uploadBrawlerImage(id, imageFile);
      } catch {
        toast.error("등록은 됐지만 이미지 업로드에 실패했습니다. 상세에서 다시 업로드하세요.");
      }
    }
    toast.success("브롤러가 등록되었습니다.");
    router.push(`/brawlers/${id}`);
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">브롤러 등록</h2>
      <div className="max-w-2xl space-y-6">
        <Card>
          <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label>BrawlStars ID</Label>
              <Input
                type="number"
                value={form.brawlStarsId}
                onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Rarity</Label>
                <Select value={form.rarity} onValueChange={(v) => setForm({ ...form, rarity: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {RARITIES.map((r) => (<SelectItem key={r} value={r}>{r}</SelectItem>))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Role</Label>
                <Select value={form.role} onValueChange={(v) => setForm({ ...form, role: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {ROLES.map((r) => (<SelectItem key={r} value={r}>{r}</SelectItem>))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={form.ko} onChange={(e) => setForm({ ...form, ko: e.target.value })} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={form.en} onChange={(e) => setForm({ ...form, en: e.target.value })} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>기어 (선택 · 클릭하여 토글)</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-3">
              {gears.map((g) => {
                const selected = selectedGearIds.includes(g.brawlStarsId);
                return (
                  <button
                    key={g.brawlStarsId}
                    type="button"
                    onClick={() => toggleGear(g.brawlStarsId)}
                    className={`flex w-20 flex-col items-center gap-1 rounded-md border p-2 ${
                      selected ? "border-primary ring-2 ring-primary" : "border-border"
                    }`}
                  >
                    <ImageThumbnail url={g.imageUrl} alt={g.names.KOREAN || String(g.brawlStarsId)} />
                    <span className="w-full truncate text-center text-xs">
                      {g.names.KOREAN || g.brawlStarsId}
                    </span>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>이미지 (선택)</CardTitle></CardHeader>
          <CardContent>
            <PendingImageInput file={imageFile} onChange={setImageFile} />
          </CardContent>
        </Card>

        <div className="flex gap-2">
          <Button onClick={handleCreate} disabled={submitting}>
            {submitting ? "등록 중..." : "등록"}
          </Button>
          <Button variant="outline" onClick={() => router.push("/brawlers")}>취소</Button>
        </div>
      </div>
    </AdminLayout>
  );
}
