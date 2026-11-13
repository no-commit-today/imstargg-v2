"use client";

import { useCallback, useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { deletePatch, getPatches } from "@/lib/api/brawlstars/patch";
import type { BrawlStarsPatch, BrawlStarsPatchList } from "@/lib/types";
import { PatchFormDialog } from "./_components/PatchFormDialog";
import { toast } from "sonner";

export default function PatchesPage() {
  const [data, setData] = useState<BrawlStarsPatchList | null>(null);
  const [loading, setLoading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<BrawlStarsPatch | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setData(await getPatches());
    } catch {
      toast.error("패치일을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deletePatch(deleteTarget.id);
      toast.success("패치일이 삭제되었습니다.");
      setDeleteTarget(null);
      load();
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">패치일</h2>
        {data && (
          <PatchFormDialog
            today={data.today}
            earliestRegistrableDate={data.earliestRegistrableDate}
            onSaved={load}
          />
        )}
      </div>
      <p className="mb-4 text-sm text-neutral-500">날짜는 KST 기준입니다</p>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}

      {data && (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>패치일</TableHead>
                <TableHead>메모</TableHead>
                <TableHead>수정</TableHead>
                <TableHead>삭제</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.patches.map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.patchDate}</TableCell>
                  <TableCell>{p.memo ?? "-"}</TableCell>
                  <TableCell>
                    <PatchFormDialog
                      patch={p}
                      today={data.today}
                      earliestRegistrableDate={data.earliestRegistrableDate}
                      onSaved={load}
                    />
                  </TableCell>
                  <TableCell>
                    <Dialog
                      open={deleteTarget?.id === p.id}
                      onOpenChange={(isOpen) => setDeleteTarget(isOpen ? p : null)}
                    >
                      <DialogTrigger
                        render={
                          <Button
                            variant="destructive"
                            size="sm"
                            disabled={p.patchDate < data.earliestRegistrableDate}
                          />
                        }
                      >
                        삭제
                      </DialogTrigger>
                      <DialogContent>
                        <DialogHeader>
                          <DialogTitle>패치일 삭제</DialogTitle>
                        </DialogHeader>
                        <p className="text-sm">{p.patchDate} 패치일을 정말 삭제하시겠습니까?</p>
                        <div className="flex justify-end gap-2 pt-2">
                          <Button variant="outline" onClick={() => setDeleteTarget(null)}>
                            취소
                          </Button>
                          <Button variant="destructive" onClick={handleDelete}>
                            삭제
                          </Button>
                        </div>
                      </DialogContent>
                    </Dialog>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {data.patches.length === 0 && (
            <p className="mt-4 text-sm text-neutral-500">등록된 패치일이 없습니다.</p>
          )}
        </>
      )}
    </AdminLayout>
  );
}
