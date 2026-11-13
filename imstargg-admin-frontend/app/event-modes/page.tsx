"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
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
import { getEventModes, createEventMode, uploadEventModeImage } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { PendingImageInput } from "@/components/pending-image-input";
import { STATS_EVENT_MODE_TYPE_LABELS, type BattleEventMode } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

export default function EventModeListPage() {
  const [eventModes, setEventModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<{
    brawlStarsId: string;
    brawlStarsName: string;
    ko: string;
    en: string;
  }>({
    brawlStarsId: "",
    brawlStarsName: "",
    ko: "",
    en: "",
  });
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadEventModes = () => getEventModes().then(setEventModes);

  useEffect(() => {
    loadEventModes();
  }, []);

  const resetForm = () => {
    setForm({
      brawlStarsId: "",
      brawlStarsName: "",
      ko: "",
      en: "",
    });
    setImageFile(null);
  };

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.brawlStarsName) {
      toast.error("ID와 이름(brawlStarsName)을 입력해주세요.");
      return;
    }
    setSubmitting(true);
    try {
      await createEventMode({
        brawlStarsId: id,
        brawlStarsName: form.brawlStarsName,
        names: { KOREAN: form.ko, ENGLISH: form.en },
      });
    } catch {
      toast.error("등록에 실패했습니다.");
      setSubmitting(false);
      return;
    }
    if (imageFile) {
      try {
        await uploadEventModeImage(id, imageFile);
      } catch {
        toast.error("등록은 됐지만 아이콘 업로드에 실패했습니다. 편집에서 다시 업로드하세요.");
      }
    }
    toast.success("이벤트 모드가 등록되었습니다.");
    setSubmitting(false);
    setOpen(false);
    resetForm();
    loadEventModes();
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 모드 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>이벤트 모드 등록</DialogTrigger>
          <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>이벤트 모드 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>BrawlStars ID</Label>
                  <Input
                    type="number"
                    value={form.brawlStarsId}
                    onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                  />
                </div>
                <div>
                  <Label>BrawlStars Name</Label>
                  <Input
                    value={form.brawlStarsName}
                    onChange={(e) => setForm({ ...form, brawlStarsName: e.target.value })}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>이름 (KO)</Label>
                  <Input value={form.ko} onChange={(e) => setForm({ ...form, ko: e.target.value })} />
                </div>
                <div>
                  <Label>이름 (EN)</Label>
                  <Input value={form.en} onChange={(e) => setForm({ ...form, en: e.target.value })} />
                </div>
              </div>
              <div>
                <Label>아이콘 (선택)</Label>
                <PendingImageInput file={imageFile} onChange={setImageFile} />
              </div>
              <Button onClick={handleCreate} className="w-full" disabled={submitting}>
                {submitting ? "등록 중..." : "등록"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>BrawlStars Name</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>통계 수집</TableHead>
            <TableHead>아이콘</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {eventModes.map((m) => (
            <TableRow key={m.brawlStarsId}>
              <TableCell>{m.brawlStarsId}</TableCell>
              <TableCell>{m.brawlStarsName}</TableCell>
              <TableCell>{m.names.KOREAN || "-"}</TableCell>
              <TableCell>{m.names.ENGLISH || "-"}</TableCell>
              <TableCell>{m.statsType ? STATS_EVENT_MODE_TYPE_LABELS[m.statsType] : "수집 안 함"}</TableCell>
              <TableCell><ImageThumbnail url={m.iconImageUrl} alt={m.names.KOREAN || m.brawlStarsName} /></TableCell>
              <TableCell>
                <Link href={`/event-modes/${m.brawlStarsId}/edit`}>
                  <Button variant="outline" size="sm">편집</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
