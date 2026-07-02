"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../admin-layout";
import { Button } from "@/components/ui/button";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  getBrawlerDetail,
  getGears,
  updateBrawlerMessages,
  uploadBrawlerImage,
  createBrawlerBuffy,
  uploadBrawlerBuffyImage,
  deleteBrawlerBuffy,
  updateBrawlerGears,
  createStarPower,
  updateStarPowerMessages,
  uploadStarPowerImage,
  createGadget,
  updateGadgetMessages,
  uploadGadgetImage,
  createHyperCharge,
  updateHyperChargeMessages,
  uploadHyperChargeImage,
} from "@/lib/api";
import type { BrawlerDetail, BuffyType, Gear, StarPower, Gadget, HyperCharge } from "@/lib/types";
import { toast } from "sonner";
import { ImageUploadField } from "@/components/image-upload-field";
import { PendingImageInput } from "@/components/pending-image-input";

const BUFFY_TYPES: BuffyType[] = ["GADGET", "STAR_POWER", "HYPER_CHARGE"];
const BUFFY_LABELS: Record<BuffyType, string> = {
  GADGET: "가젯 버피",
  STAR_POWER: "스타파워 버피",
  HYPER_CHARGE: "하이퍼차지 버피",
};

function SubItemRow({
  item,
  onSaveMessages,
  onUploadImage,
  onReload,
}: {
  item: StarPower | Gadget | HyperCharge;
  onSaveMessages: (messages: Record<string, string>) => Promise<void>;
  onUploadImage: (file: File) => Promise<void>;
  onReload: () => void;
}) {
  const [ko, setKo] = useState(item.names.KOREAN || "");
  const [en, setEn] = useState(item.names.ENGLISH || "");

  const handleSave = async () => {
    try {
      await onSaveMessages({ KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      onReload();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  return (
    <div className="space-y-3 rounded-md border p-3">
      <p className="text-xs text-muted-foreground">ID {item.brawlStarsId}</p>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <Label>이름 (KO)</Label>
          <Input value={ko} onChange={(e) => setKo(e.target.value)} />
        </div>
        <div>
          <Label>이름 (EN)</Label>
          <Input value={en} onChange={(e) => setEn(e.target.value)} />
        </div>
      </div>
      <Button size="sm" onClick={handleSave}>이름 저장</Button>
      <div>
        <Label>이미지</Label>
        <ImageUploadField
          currentImageUrl={item.imageUrl}
          onUpload={onUploadImage}
          onUploaded={onReload}
          previewClassName="max-h-24"
        />
      </div>
    </div>
  );
}

function SubItemSection({
  title,
  items,
  onAdd,
  onSaveMessages,
  onUploadImage,
  onReload,
}: {
  title: string;
  items: (StarPower | Gadget | HyperCharge)[];
  onAdd: (brawlStarsId: number, names: Record<string, string>) => Promise<void>;
  onSaveMessages: (itemBrawlStarsId: number, messages: Record<string, string>) => Promise<void>;
  onUploadImage: (itemBrawlStarsId: number, file: File) => Promise<void>;
  onReload: () => void;
}) {
  const [addOpen, setAddOpen] = useState(false);
  const [addId, setAddId] = useState("");
  const [addKo, setAddKo] = useState("");
  const [addEn, setAddEn] = useState("");
  const [addFile, setAddFile] = useState<File | null>(null);
  const [adding, setAdding] = useState(false);

  const resetAddForm = () => {
    setAddId("");
    setAddKo("");
    setAddEn("");
    setAddFile(null);
  };

  const handleAdd = async () => {
    const id = Number(addId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    setAdding(true);
    try {
      await onAdd(id, { KOREAN: addKo, ENGLISH: addEn });
    } catch {
      toast.error("등록에 실패했습니다.");
      setAdding(false);
      return;
    }
    if (addFile) {
      try {
        await onUploadImage(id, addFile);
      } catch {
        toast.error("등록됐지만 이미지 업로드에 실패했습니다. 아래 목록에서 다시 업로드하세요.");
      }
    }
    toast.success("등록되었습니다.");
    setAdding(false);
    setAddOpen(false);
    resetAddForm();
    onReload();
  };

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>
          {title} <span className="text-sm font-normal text-muted-foreground">· {items.length}</span>
        </CardTitle>
        <Dialog open={addOpen} onOpenChange={setAddOpen}>
          <DialogTrigger render={<Button size="sm" />}>{title} 추가</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{title} 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input type="number" value={addId} onChange={(e) => setAddId(e.target.value)} />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <Label>이름 (KO)</Label>
                  <Input value={addKo} onChange={(e) => setAddKo(e.target.value)} />
                </div>
                <div>
                  <Label>이름 (EN)</Label>
                  <Input value={addEn} onChange={(e) => setAddEn(e.target.value)} />
                </div>
              </div>
              <div>
                <Label>이미지 (선택)</Label>
                <PendingImageInput file={addFile} onChange={setAddFile} />
              </div>
              <Button onClick={handleAdd} disabled={adding} className="w-full">
                {adding ? "등록 중..." : "등록"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </CardHeader>
      <CardContent className="space-y-3">
        {items.length === 0 && (
          <p className="text-sm text-muted-foreground">등록된 항목이 없습니다.</p>
        )}
        {items.map((item) => (
          <SubItemRow
            key={item.brawlStarsId}
            item={item}
            onSaveMessages={(msgs) => onSaveMessages(item.brawlStarsId, msgs)}
            onUploadImage={(file) => onUploadImage(item.brawlStarsId, file)}
            onReload={onReload}
          />
        ))}
      </CardContent>
    </Card>
  );
}

export default function BrawlerDetailPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [detail, setDetail] = useState<BrawlerDetail | null>(null);
  const [gears, setGears] = useState<Gear[]>([]);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");
  const [selectedGearIds, setSelectedGearIds] = useState<Set<number>>(new Set());
  const [buffyFiles, setBuffyFiles] = useState<Record<BuffyType, File | null>>({
    GADGET: null,
    STAR_POWER: null,
    HYPER_CHARGE: null,
  });

  const loadDetail = useCallback(() => {
    getBrawlerDetail(brawlStarsId).then((d) => {
      setDetail(d);
      setKo(d.names.KOREAN || "");
      setEn(d.names.ENGLISH || "");
      setSelectedGearIds(new Set(d.linkedGearBrawlStarsIds));
    });
  }, [brawlStarsId]);

  useEffect(() => {
    loadDetail();
    getGears().then(setGears);
  }, [loadDetail]);

  if (!detail) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  const handleSaveMessages = async () => {
    try {
      await updateBrawlerMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadDetail();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleCreateBuffy = async (type: BuffyType) => {
    try {
      await createBrawlerBuffy(brawlStarsId, type);
    } catch {
      toast.error("등록에 실패했습니다.");
      return;
    }
    const file = buffyFiles[type];
    if (file) {
      try {
        await uploadBrawlerBuffyImage(brawlStarsId, type, file);
      } catch {
        toast.error("등록됐지만 이미지 업로드에 실패했습니다. 이미지를 다시 업로드하세요.");
      }
    }
    toast.success("버피가 등록되었습니다.");
    setBuffyFiles((prev) => ({ ...prev, [type]: null }));
    loadDetail();
  };

  const handleDeleteBuffy = async (type: BuffyType) => {
    if (!confirm(`${BUFFY_LABELS[type]}를 삭제할까요? 이미지도 함께 삭제됩니다.`)) return;
    try {
      await deleteBrawlerBuffy(brawlStarsId, type);
      toast.success("버피가 삭제되었습니다.");
      loadDetail();
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  const handleSaveGears = async () => {
    try {
      await updateBrawlerGears(brawlStarsId, Array.from(selectedGearIds));
      toast.success("기어 연결이 저장되었습니다.");
      loadDetail();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const toggleGear = (gearId: number) => {
    setSelectedGearIds((prev) => {
      const next = new Set(prev);
      if (next.has(gearId)) next.delete(gearId);
      else next.add(gearId);
      return next;
    });
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">브롤러 상세 - {brawlStarsId}</h2>

      <div className="max-w-3xl space-y-6">
        <Card>
          <CardHeader><CardTitle>이름 · 기본정보</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              ID {detail.brawlStarsId} · {detail.rarity} · {detail.role}
            </p>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={ko} onChange={(e) => setKo(e.target.value)} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={en} onChange={(e) => setEn(e.target.value)} />
              </div>
            </div>
            <Button onClick={handleSaveMessages}>이름 저장</Button>
            <div>
              <Label>메인 이미지</Label>
              <ImageUploadField
                currentImageUrl={detail.imageUrl}
                onUpload={(file) => uploadBrawlerImage(brawlStarsId, file)}
                onUploaded={loadDetail}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between">
            <CardTitle>기어</CardTitle>
            <Button size="sm" onClick={handleSaveGears}>기어 연결 저장</Button>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-3">
              {gears.map((gear) => {
                const selected = selectedGearIds.has(gear.brawlStarsId);
                return (
                  <button
                    key={gear.brawlStarsId}
                    type="button"
                    onClick={() => toggleGear(gear.brawlStarsId)}
                    className={`flex w-20 flex-col items-center gap-1 rounded-md border p-2 ${
                      selected ? "border-primary ring-2 ring-primary" : "border-border"
                    }`}
                  >
                    <ImageThumbnail url={gear.imageUrl} alt={gear.names.KOREAN || String(gear.brawlStarsId)} />
                    <span className="w-full truncate text-center text-xs">
                      {gear.names.KOREAN || gear.brawlStarsId}
                    </span>
                    <span className="text-[10px] text-muted-foreground">{gear.rarity}</span>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>

        <SubItemSection
          title="가젯"
          items={detail.gadgets}
          onAdd={async (id, names) => {
            await createGadget(brawlStarsId, id, names);
          }}
          onSaveMessages={(itemId, msgs) => updateGadgetMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadGadgetImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <SubItemSection
          title="스타파워"
          items={detail.starPowers}
          onAdd={async (id, names) => {
            await createStarPower(brawlStarsId, id, names);
          }}
          onSaveMessages={(itemId, msgs) => updateStarPowerMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadStarPowerImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <SubItemSection
          title="하이퍼차지"
          items={detail.hyperCharges}
          onAdd={async (id, names) => {
            await createHyperCharge(brawlStarsId, id, names);
          }}
          onSaveMessages={(itemId, msgs) => updateHyperChargeMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadHyperChargeImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <Card>
          <CardHeader><CardTitle>버피</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {BUFFY_TYPES.map((type) => {
              const buffy = detail.buffies.find((b) => b.type === type);
              return (
                <div
                  key={type}
                  className="flex items-start gap-4 border-b pb-4 last:border-b-0 last:pb-0"
                >
                  <div className="flex-1 space-y-2">
                    <p className="text-sm font-medium">{BUFFY_LABELS[type]}</p>
                    {buffy ? (
                      <ImageUploadField
                        currentImageUrl={buffy.imageUrl}
                        onUpload={(file) => uploadBrawlerBuffyImage(brawlStarsId, type, file)}
                        onUploaded={loadDetail}
                        previewClassName="max-h-24"
                      />
                    ) : (
                      <PendingImageInput
                        file={buffyFiles[type]}
                        onChange={(f) => setBuffyFiles((prev) => ({ ...prev, [type]: f }))}
                      />
                    )}
                  </div>
                  <div className="pt-5">
                    {buffy ? (
                      <Button variant="outline" size="sm" onClick={() => handleDeleteBuffy(type)}>
                        삭제
                      </Button>
                    ) : (
                      <Button size="sm" onClick={() => handleCreateBuffy(type)}>
                        등록
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
