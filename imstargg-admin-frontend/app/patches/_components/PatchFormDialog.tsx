"use client";

import { useState } from "react";
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
import { registerPatch, updatePatch } from "@/lib/api/brawlstars/patch";
import type { BrawlStarsPatch } from "@/lib/types";
import { toast } from "sonner";

interface PatchFormDialogProps {
  /** 없으면 등록, 있으면 그 패치일의 수정이다. */
  patch?: BrawlStarsPatch;
  today: string;
  earliestRegistrableDate: string;
  onSaved: () => void;
}

export function PatchFormDialog({
  patch,
  today,
  earliestRegistrableDate,
  onSaved,
}: PatchFormDialogProps) {
  const [open, setOpen] = useState(false);
  const [patchDate, setPatchDate] = useState("");
  const [memo, setMemo] = useState("");
  const [saving, setSaving] = useState(false);

  // yyyy-MM-dd 문자열 비교로 판단한다(Date 로 바꾸면 UTC 변환으로 날짜가 밀린다).
  const dateLocked = patch !== undefined && patch.patchDate < earliestRegistrableDate;

  const handleOpenChange = (next: boolean) => {
    if (next) {
      setPatchDate(patch?.patchDate ?? "");
      setMemo(patch?.memo ?? "");
    }
    setOpen(next);
  };

  const handleSave = async () => {
    if (!dateLocked) {
      if (!patchDate) {
        toast.error("패치일을 입력해주세요.");
        return;
      }
      if (patchDate < earliestRegistrableDate || patchDate > today) {
        toast.error(`패치일은 ${earliestRegistrableDate} 부터 ${today} 까지만 고를 수 있습니다.`);
        return;
      }
    }
    const body = {
      patchDate: dateLocked ? patch.patchDate : patchDate,
      memo: memo === "" ? null : memo,
    };
    setSaving(true);
    try {
      if (patch) {
        await updatePatch(patch.id, body);
        toast.success("패치일이 수정되었습니다.");
      } else {
        await registerPatch(body);
        toast.success("패치일이 등록되었습니다.");
      }
      setOpen(false);
      onSaved();
    } catch {
      toast.error("저장에 실패했습니다. 같은 날짜가 이미 등록돼 있는지 확인해 주세요.");
    } finally {
      setSaving(false);
    }
  };

  const title = patch ? "패치일 수정" : "패치일 등록";

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger
        render={patch ? <Button variant="outline" size="sm" /> : <Button />}
      >
        {patch ? "수정" : "패치일 등록"}
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label>패치일</Label>
            <Input
              type="date"
              min={earliestRegistrableDate}
              max={today}
              value={patchDate}
              disabled={dateLocked}
              onChange={(e) => setPatchDate(e.target.value)}
            />
            {dateLocked && (
              <p className="mt-1 text-xs text-neutral-500">
                {earliestRegistrableDate} 이전의 패치일은 날짜를 바꿀 수 없어 메모만 고칠 수 있습니다.
              </p>
            )}
          </div>
          <div>
            <Label>메모</Label>
            <Input
              maxLength={255}
              value={memo}
              onChange={(e) => setMemo(e.target.value)}
            />
          </div>
          <Button onClick={handleSave} disabled={saving} className="w-full">
            저장
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
