"use client";

import { useEffect, useMemo } from "react";
import { Input } from "@/components/ui/input";

interface PendingImageInputProps {
  file: File | null;
  onChange: (file: File | null) => void;
  accept?: string;
}

export function PendingImageInput({ file, onChange, accept = "image/*" }: PendingImageInputProps) {
  // 렌더 중 파생(불필요한 effect 회피) + 변경/언마운트 시 revoke 로 blob 누수 방지.
  const previewUrl = useMemo(() => (file ? URL.createObjectURL(file) : null), [file]);

  useEffect(() => {
    if (!previewUrl) return;
    return () => URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  return (
    <div className="space-y-2">
      <Input type="file" accept={accept} onChange={(e) => onChange(e.target.files?.[0] ?? null)} />
      {previewUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={previewUrl} alt="" className="max-h-32" />
      )}
      {file && <p className="text-xs text-muted-foreground">선택됨: {file.name}</p>}
    </div>
  );
}
