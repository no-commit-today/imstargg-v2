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
import { getEventModes, createEventMode } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import type { BattleEventMode, BattleEventModeResultType } from "@/lib/types";
import { BATTLE_EVENT_MODE_RESULT_TYPES } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

export default function EventModeListPage() {
  const [eventModes, setEventModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<{
    brawlStarsId: string;
    brawlStarsName: string;
    resultType: BattleEventModeResultType;
  }>({ brawlStarsId: "", brawlStarsName: "", resultType: "RESULT" });

  const loadEventModes = () => getEventModes().then(setEventModes);

  useEffect(() => {
    loadEventModes();
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.brawlStarsName) {
      toast.error("모든 필드를 입력해주세요.");
      return;
    }
    try {
      await createEventMode({
        brawlStarsId: id,
        brawlStarsName: form.brawlStarsName,
        resultType: form.resultType,
      });
      toast.success("이벤트 모드가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", brawlStarsName: "", resultType: "RESULT" });
      loadEventModes();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 모드 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>이벤트 모드 등록</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>이벤트 모드 등록</DialogTitle>
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
                <Label>BrawlStars Name</Label>
                <Input
                  value={form.brawlStarsName}
                  onChange={(e) => setForm({ ...form, brawlStarsName: e.target.value })}
                />
              </div>
              <div>
                <Label>Result Type</Label>
                <Select
                  value={form.resultType}
                  onValueChange={(v) =>
                    setForm({ ...form, resultType: (v ?? "RESULT") as BattleEventModeResultType })
                  }
                >
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {BATTLE_EVENT_MODE_RESULT_TYPES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <Button onClick={handleCreate} className="w-full">등록</Button>
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
            <TableHead>결과 타입</TableHead>
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
              <TableCell>{m.resultType}</TableCell>
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
