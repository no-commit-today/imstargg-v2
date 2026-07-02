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
import {
  getGears,
  createBrawler,
  uploadBrawlerImage,
  createGadget,
  uploadGadgetImage,
  createStarPower,
  uploadStarPowerImage,
  createHyperCharge,
  uploadHyperChargeImage,
} from "@/lib/api";
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

type PendingSubItem = {
  key: number;
  brawlStarsId: string;
  ko: string;
  en: string;
  file: File | null;
};

let pendingSubItemKey = 0;
const nextPendingSubItemKey = () => ++pendingSubItemKey;

function PendingSubItemsCard({
  title,
  items,
  onChange,
}: {
  title: string;
  items: PendingSubItem[];
  onChange: (items: PendingSubItem[]) => void;
}) {
  const updateItem = (key: number, patch: Partial<PendingSubItem>) =>
    onChange(items.map((it) => (it.key === key ? { ...it, ...patch } : it)));

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>{title} (선택)</CardTitle>
        <Button
          size="sm"
          variant="outline"
          onClick={() =>
            onChange([
              ...items,
              { key: nextPendingSubItemKey(), brawlStarsId: "", ko: "", en: "", file: null },
            ])
          }
        >
          행 추가
        </Button>
      </CardHeader>
      <CardContent className="space-y-3">
        {items.length === 0 && (
          <p className="text-sm text-muted-foreground">함께 등록할 항목이 없으면 비워두세요.</p>
        )}
        {items.map((item) => (
          <div key={item.key} className="space-y-3 rounded-md border p-3">
            <div className="flex items-end gap-2">
              <div className="flex-1">
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={item.brawlStarsId}
                  onChange={(e) => updateItem(item.key, { brawlStarsId: e.target.value })}
                />
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onChange(items.filter((it) => it.key !== item.key))}
              >
                삭제
              </Button>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={item.ko} onChange={(e) => updateItem(item.key, { ko: e.target.value })} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={item.en} onChange={(e) => updateItem(item.key, { en: e.target.value })} />
              </div>
            </div>
            <div>
              <Label>이미지 (선택)</Label>
              <PendingImageInput file={item.file} onChange={(f) => updateItem(item.key, { file: f })} />
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export default function BrawlerCreatePage() {
  const router = useRouter();
  const [gears, setGears] = useState<Gear[]>([]);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", role: "", ko: "", en: "" });
  const [selectedGearIds, setSelectedGearIds] = useState<number[]>([]);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [gadgets, setGadgets] = useState<PendingSubItem[]>([]);
  const [starPowers, setStarPowers] = useState<PendingSubItem[]>([]);
  const [hyperCharges, setHyperCharges] = useState<PendingSubItem[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getGears().then(setGears);
  }, []);

  const toggleGear = (id: number) => {
    setSelectedGearIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id],
    );
  };

  const subItemGroups = [
    { label: "가젯", items: gadgets, create: createGadget, upload: uploadGadgetImage },
    { label: "스타파워", items: starPowers, create: createStarPower, upload: uploadStarPowerImage },
    { label: "하이퍼차지", items: hyperCharges, create: createHyperCharge, upload: uploadHyperChargeImage },
  ];

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity || !form.role) {
      toast.error("ID, rarity, role을 입력해주세요.");
      return;
    }
    if (subItemGroups.some((group) => group.items.some((item) => !Number(item.brawlStarsId)))) {
      toast.error("하위 아이템의 BrawlStars ID를 입력해주세요. 필요 없는 행은 삭제하세요.");
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
    const failed: string[] = [];
    if (imageFile) {
      try {
        await uploadBrawlerImage(id, imageFile);
      } catch {
        failed.push("브롤러 이미지");
      }
    }
    for (const group of subItemGroups) {
      for (const item of group.items) {
        const itemId = Number(item.brawlStarsId);
        try {
          await group.create(id, itemId, { KOREAN: item.ko, ENGLISH: item.en });
        } catch {
          failed.push(`${group.label} ${itemId} 등록`);
          continue;
        }
        if (item.file) {
          try {
            await group.upload(id, itemId, item.file);
          } catch {
            failed.push(`${group.label} ${itemId} 이미지`);
          }
        }
      }
    }
    if (failed.length > 0) {
      toast.error(`일부 항목에 실패했습니다: ${failed.join(", ")} — 상세 페이지에서 다시 시도하세요.`);
    }
    setSubmitting(false);
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

        <PendingSubItemsCard title="가젯" items={gadgets} onChange={setGadgets} />
        <PendingSubItemsCard title="스타파워" items={starPowers} onChange={setStarPowers} />
        <PendingSubItemsCard title="하이퍼차지" items={hyperCharges} onChange={setHyperCharges} />

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
