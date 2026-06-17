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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getGears, createGear, uploadGearImage } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { PendingImageInput } from "@/components/pending-image-input";
import type { Gear } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

const GEAR_RARITIES = ["SUPER_RARE", "EPIC", "MYTHIC"];

export default function GearListPage() {
  const [gears, setGears] = useState<Gear[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", ko: "", en: "" });
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadGears = () => getGears().then(setGears);

  useEffect(() => {
    loadGears();
  }, []);

  const resetForm = () => {
    setForm({ brawlStarsId: "", rarity: "", ko: "", en: "" });
    setImageFile(null);
  };

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity) {
      toast.error("ID와 rarity를 입력해주세요.");
      return;
    }
    setSubmitting(true);
    try {
      await createGear({
        brawlStarsId: id,
        rarity: form.rarity,
        names: { KOREAN: form.ko, ENGLISH: form.en },
      });
    } catch {
      toast.error("등록에 실패했습니다.");
      setSubmitting(false);
      return;
    }
    if (imageFile) {
      try {
        await uploadGearImage(id, imageFile);
      } catch {
        toast.error("등록은 됐지만 이미지 업로드에 실패했습니다. 편집에서 다시 업로드하세요.");
      }
    }
    toast.success("기어가 등록되었습니다.");
    setSubmitting(false);
    setOpen(false);
    resetForm();
    loadGears();
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">기어 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>기어 등록</DialogTrigger>
          <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>기어 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>Rarity</Label>
                <Select value={form.rarity} onValueChange={(v) => setForm({ ...form, rarity: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {GEAR_RARITIES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
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
                <Label>이미지 (선택)</Label>
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
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {gears.map((g) => (
            <TableRow key={g.brawlStarsId}>
              <TableCell>{g.brawlStarsId}</TableCell>
              <TableCell>{g.names.KOREAN || "-"}</TableCell>
              <TableCell>{g.names.ENGLISH || "-"}</TableCell>
              <TableCell>{g.rarity}</TableCell>
              <TableCell><ImageThumbnail url={g.imageUrl} alt={g.names.KOREAN || String(g.brawlStarsId)} /></TableCell>
              <TableCell>
                <Link href={`/gears/${g.brawlStarsId}/edit`}>
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
