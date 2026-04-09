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
import { getEvents, getEventModes, createEvent } from "@/lib/api";
import type { BattleEvent, BattleEventMode } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

export default function EventListPage() {
  const [events, setEvents] = useState<BattleEvent[]>([]);
  const [eventModes, setEventModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", modeId: "", mapName: "" });

  const loadEvents = () => getEvents().then(setEvents);

  useEffect(() => {
    loadEvents();
    getEventModes().then(setEventModes);
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    try {
      await createEvent({
        brawlStarsId: id,
        modeId: form.modeId ? Number(form.modeId) : undefined,
        mapName: form.mapName || undefined,
      });
      toast.success("이벤트가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", modeId: "", mapName: "" });
      loadEvents();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>이벤트 등록</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>이벤트 등록</DialogTitle>
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
                <Label>모드 (선택)</Label>
                <Select value={form.modeId} onValueChange={(v) => setForm({ ...form, modeId: v ?? "" })}>
                  <SelectTrigger><SelectValue placeholder="모드 선택" /></SelectTrigger>
                  <SelectContent>
                    {eventModes.map((m) => (
                      <SelectItem key={m.brawlStarsId} value={String(m.brawlStarsId)}>
                        {m.brawlStarsName} ({m.brawlStarsId})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>맵 이름 (선택)</Label>
                <Input
                  value={form.mapName}
                  onChange={(e) => setForm({ ...form, mapName: e.target.value })}
                />
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
            <TableHead>모드</TableHead>
            <TableHead>맵 이름</TableHead>
            <TableHead>맵 이름 (KO)</TableHead>
            <TableHead>맵 이름 (EN)</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((e) => (
            <TableRow key={e.brawlStarsId}>
              <TableCell>{e.brawlStarsId}</TableCell>
              <TableCell>{e.modeBrawlStarsName || "-"}</TableCell>
              <TableCell>{e.mapName || "-"}</TableCell>
              <TableCell>{e.mapNames.KOREAN || "-"}</TableCell>
              <TableCell>{e.mapNames.ENGLISH || "-"}</TableCell>
              <TableCell>
                <Link href={`/events/${e.brawlStarsId}/edit`}>
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
