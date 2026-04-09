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
import { getMaintenances, registerMaintenance, deleteMaintenance } from "@/lib/api";
import type { Maintenance } from "@/lib/types";
import { toast } from "sonner";

export default function MaintenancePage() {
  const [maintenances, setMaintenances] = useState<Maintenance[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ startTime: "", endTime: "" });
  const [deleteTarget, setDeleteTarget] = useState<Maintenance | null>(null);

  const loadMaintenances = () => getMaintenances().then(setMaintenances);

  useEffect(() => {
    loadMaintenances();
  }, []);

  const handleRegister = async () => {
    if (!form.startTime || !form.endTime) {
      toast.error("시작 시간과 종료 시간을 입력해주세요.");
      return;
    }
    try {
      await registerMaintenance({
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
      });
      toast.success("점검이 등록되었습니다.");
      setOpen(false);
      setForm({ startTime: "", endTime: "" });
      loadMaintenances();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteMaintenance(deleteTarget.id);
      toast.success("점검이 삭제되었습니다.");
      setDeleteTarget(null);
      loadMaintenances();
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">API 점검 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>점검 등록</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>점검 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>시작 시간</Label>
                <Input
                  type="datetime-local"
                  value={form.startTime}
                  onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                />
              </div>
              <div>
                <Label>종료 시간</Label>
                <Input
                  type="datetime-local"
                  value={form.endTime}
                  onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                />
              </div>
              <Button onClick={handleRegister} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>ID</TableHead>
            <TableHead>시작 시간</TableHead>
            <TableHead>종료 시간</TableHead>
            <TableHead>등록 시간</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {maintenances.map((m) => (
            <TableRow key={m.id}>
              <TableCell>{m.id}</TableCell>
              <TableCell>{m.startTime}</TableCell>
              <TableCell>{m.endTime}</TableCell>
              <TableCell>{m.createdAt || "-"}</TableCell>
              <TableCell>
                <Dialog
                  open={deleteTarget?.id === m.id}
                  onOpenChange={(isOpen) => setDeleteTarget(isOpen ? m : null)}
                >
                  <DialogTrigger render={<Button variant="destructive" size="sm" />}>삭제</DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>점검 삭제</DialogTitle>
                    </DialogHeader>
                    <p className="text-sm">정말 삭제하시겠습니까?</p>
                    <div className="flex justify-end gap-2 pt-2">
                      <Button variant="outline" onClick={() => setDeleteTarget(null)}>취소</Button>
                      <Button variant="destructive" onClick={handleDelete}>삭제</Button>
                    </div>
                  </DialogContent>
                </Dialog>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
