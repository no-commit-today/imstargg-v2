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
import { getBrawlers, createBrawler } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import type { BrawlerListItem } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

const RARITIES = [
  "STARTING_BRAWLER",
  "RARE",
  "SUPER_RARE",
  "EPIC",
  "MYTHIC",
  "LEGENDARY",
  "TIME_LIMITED_BRAWLER",
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

export default function BrawlerListPage() {
  const [brawlers, setBrawlers] = useState<BrawlerListItem[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", role: "" });

  const loadBrawlers = () => getBrawlers().then(setBrawlers);

  useEffect(() => {
    loadBrawlers();
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity || !form.role) {
      toast.error("모든 필드를 입력해주세요.");
      return;
    }
    try {
      await createBrawler({ brawlStarsId: id, rarity: form.rarity, role: form.role });
      toast.success("브롤러가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", rarity: "", role: "" });
      loadBrawlers();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">브롤러 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>브롤러 등록</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>브롤러 등록</DialogTitle>
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
                    {RARITIES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Role</Label>
                <Select value={form.role} onValueChange={(v) => setForm({ ...form, role: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {ROLES.map((r) => (
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
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>Role</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {brawlers.map((b) => (
            <TableRow key={b.brawlStarsId}>
              <TableCell>{b.brawlStarsId}</TableCell>
              <TableCell>{b.names.KOREAN || "-"}</TableCell>
              <TableCell>{b.names.ENGLISH || "-"}</TableCell>
              <TableCell>{b.rarity}</TableCell>
              <TableCell>{b.role}</TableCell>
              <TableCell><ImageThumbnail url={b.imageUrl} alt={b.names.KOREAN || String(b.brawlStarsId)} /></TableCell>
              <TableCell>
                <Link href={`/brawlers/${b.brawlStarsId}`}>
                  <Button variant="outline" size="sm">상세</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
