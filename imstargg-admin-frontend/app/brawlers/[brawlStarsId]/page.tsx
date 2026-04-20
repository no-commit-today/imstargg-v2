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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  getBrawlerDetail,
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
import type { BrawlerDetail, BuffyType, StarPower, Gadget, HyperCharge } from "@/lib/types";
import { toast } from "sonner";
import { ImageUploadField } from "@/components/image-upload-field";

const BUFFY_TYPES: BuffyType[] = ["GADGET", "STAR_POWER", "HYPER_CHARGE"];
const BUFFY_LABELS: Record<BuffyType, string> = {
  GADGET: "가젯 버피",
  STAR_POWER: "스타파워 버피",
  HYPER_CHARGE: "하이퍼차지 버피",
};

// Sub-item (StarPower/Gadget/HyperCharge) edit dialog
function SubItemEditDialog({
  item,
  label,
  onSaveMessages,
  onUploadImage,
}: {
  item: StarPower | Gadget | HyperCharge;
  label: string;
  onSaveMessages: (messages: Record<string, string>) => Promise<void>;
  onUploadImage: (file: File) => Promise<void>;
}) {
  const [ko, setKo] = useState(item.names.KOREAN || "");
  const [en, setEn] = useState(item.names.ENGLISH || "");

  const handleSave = async () => {
    try {
      await onSaveMessages({ KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  return (
    <DialogContent>
      <DialogHeader>
        <DialogTitle>
          {label} 편집 - {item.brawlStarsId}
        </DialogTitle>
      </DialogHeader>
      <div className="space-y-4">
        <div>
          <Label>이름 (KO)</Label>
          <Input value={ko} onChange={(e) => setKo(e.target.value)} />
        </div>
        <div>
          <Label>이름 (EN)</Label>
          <Input value={en} onChange={(e) => setEn(e.target.value)} />
        </div>
        <Button onClick={handleSave}>이름 저장</Button>
        <div>
          <Label>이미지</Label>
          <ImageUploadField
            currentImageUrl={item.imageUrl}
            onUpload={onUploadImage}
            previewClassName="max-h-24"
          />
        </div>
      </div>
    </DialogContent>
  );
}

// Sub-item grid and management
function SubItemSection({
  title,
  items,
  onAdd,
  onSaveMessages,
  onUploadImage,
}: {
  title: string;
  items: (StarPower | Gadget | HyperCharge)[];
  onAdd: (brawlStarsId: number) => Promise<void>;
  onSaveMessages: (itemBrawlStarsId: number, messages: Record<string, string>) => Promise<void>;
  onUploadImage: (itemBrawlStarsId: number, file: File) => Promise<void>;
}) {
  const [addOpen, setAddOpen] = useState(false);
  const [addId, setAddId] = useState("");
  const [editItem, setEditItem] = useState<(StarPower | Gadget | HyperCharge) | null>(null);

  const handleAdd = async () => {
    const id = Number(addId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    try {
      await onAdd(id);
      toast.success("등록되었습니다.");
      setAddOpen(false);
      setAddId("");
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">{title}</h3>
        <Dialog open={addOpen} onOpenChange={setAddOpen}>
          <DialogTrigger render={<Button size="sm" />}>{title} 등록</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{title} 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={addId}
                  onChange={(e) => setAddId(e.target.value)}
                />
              </div>
              <Button onClick={handleAdd} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.brawlStarsId}>
              <TableCell>{item.brawlStarsId}</TableCell>
              <TableCell>{item.names.KOREAN || "-"}</TableCell>
              <TableCell>{item.names.ENGLISH || "-"}</TableCell>
              <TableCell><ImageThumbnail url={item.imageUrl} alt={item.names.KOREAN || String(item.brawlStarsId)} /></TableCell>
              <TableCell>
                <Dialog
                  open={editItem?.brawlStarsId === item.brawlStarsId}
                  onOpenChange={(open) => setEditItem(open ? item : null)}
                >
                  <DialogTrigger render={<Button variant="outline" size="sm" />}>편집</DialogTrigger>
                  <SubItemEditDialog
                    item={item}
                    label={title}
                    onSaveMessages={(msgs) => onSaveMessages(item.brawlStarsId, msgs)}
                    onUploadImage={(file) => onUploadImage(item.brawlStarsId, file)}
                  />
                </Dialog>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

export default function BrawlerDetailPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [detail, setDetail] = useState<BrawlerDetail | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");
  const [selectedGearIds, setSelectedGearIds] = useState<Set<number>>(new Set());

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
      toast.success("버피가 등록되었습니다.");
      loadDetail();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
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

      <Tabs defaultValue="info" className="space-y-4">
        <TabsList>
          <TabsTrigger value="info">기본 정보</TabsTrigger>
          <TabsTrigger value="star-powers">스타파워</TabsTrigger>
          <TabsTrigger value="gadgets">가젯</TabsTrigger>
          <TabsTrigger value="hyper-charges">하이퍼차지</TabsTrigger>
          <TabsTrigger value="gears">기어</TabsTrigger>
        </TabsList>

        <TabsContent value="info" className="space-y-6">
          <Card>
            <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <p>BrawlStars ID: {detail.brawlStarsId}</p>
              <p>Rarity: {detail.rarity}</p>
              <p>Role: {detail.role}</p>
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
            <CardHeader><CardTitle>메인 이미지</CardTitle></CardHeader>
            <CardContent>
              <ImageUploadField
                currentImageUrl={detail.imageUrl}
                onUpload={(file) => uploadBrawlerImage(brawlStarsId, file)}
                onUploaded={loadDetail}
              />
            </CardContent>
          </Card>

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
                        <p className="text-sm text-muted-foreground">미등록</p>
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
        </TabsContent>

        <TabsContent value="star-powers">
          <SubItemSection
            title="스타파워"
            items={detail.starPowers}
            onAdd={async (id) => {
              await createStarPower(brawlStarsId, id);
              loadDetail();
            }}
            onSaveMessages={async (itemId, msgs) => {
              await updateStarPowerMessages(brawlStarsId, itemId, msgs);
              loadDetail();
            }}
            onUploadImage={async (itemId, file) => {
              await uploadStarPowerImage(brawlStarsId, itemId, file);
              loadDetail();
            }}
          />
        </TabsContent>

        <TabsContent value="gadgets">
          <SubItemSection
            title="가젯"
            items={detail.gadgets}
            onAdd={async (id) => {
              await createGadget(brawlStarsId, id);
              loadDetail();
            }}
            onSaveMessages={async (itemId, msgs) => {
              await updateGadgetMessages(brawlStarsId, itemId, msgs);
              loadDetail();
            }}
            onUploadImage={async (itemId, file) => {
              await uploadGadgetImage(brawlStarsId, itemId, file);
              loadDetail();
            }}
          />
        </TabsContent>

        <TabsContent value="hyper-charges">
          <SubItemSection
            title="하이퍼차지"
            items={detail.hyperCharges}
            onAdd={async (id) => {
              await createHyperCharge(brawlStarsId, id);
              loadDetail();
            }}
            onSaveMessages={async (itemId, msgs) => {
              await updateHyperChargeMessages(brawlStarsId, itemId, msgs);
              loadDetail();
            }}
            onUploadImage={async (itemId, file) => {
              await uploadHyperChargeImage(brawlStarsId, itemId, file);
              loadDetail();
            }}
          />
        </TabsContent>

        <TabsContent value="gears">
          <Card>
            <CardHeader><CardTitle>기어 연결</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-2">
                {detail.allGears.map((gear) => (
                  <Button
                    key={gear.brawlStarsId}
                    variant={selectedGearIds.has(gear.brawlStarsId) ? "default" : "outline"}
                    size="sm"
                    onClick={() => toggleGear(gear.brawlStarsId)}
                  >
                    {gear.names?.KOREAN || gear.brawlStarsId} ({gear.rarity})
                  </Button>
                ))}
              </div>
              <Button onClick={handleSaveGears}>기어 연결 저장</Button>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </AdminLayout>
  );
}
