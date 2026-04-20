"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";

interface ImageUploadFieldProps {
  currentImageUrl: string | null | undefined;
  onUpload: (file: File) => Promise<void>;
  onUploaded?: () => void;
  emptyText?: string;
  accept?: string;
  successMessage?: string;
  errorMessage?: string;
  previewClassName?: string;
}

export function ImageUploadField({
  currentImageUrl,
  onUpload,
  onUploaded,
  emptyText = "이미지 없음",
  accept = "image/*",
  successMessage = "이미지가 업로드되었습니다.",
  errorMessage = "업로드에 실패했습니다.",
  previewClassName = "max-h-32",
}: ImageUploadFieldProps) {
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [inputKey, setInputKey] = useState(0);

  useEffect(() => {
    if (!pendingFile) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(pendingFile);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [pendingFile]);

  const handleSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setPendingFile(file);
  };

  const resetPending = () => {
    setPendingFile(null);
    setInputKey((k) => k + 1);
  };

  const handleSave = async () => {
    if (!pendingFile) return;
    setUploading(true);
    try {
      await onUpload(pendingFile);
      toast.success(successMessage);
      resetPending();
      onUploaded?.();
    } catch {
      toast.error(errorMessage);
    } finally {
      setUploading(false);
    }
  };

  const displayUrl = previewUrl ?? currentImageUrl;

  return (
    <div className="space-y-2">
      {displayUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={displayUrl} alt="" className={previewClassName} />
      ) : (
        <p className="text-sm text-muted-foreground">{emptyText}</p>
      )}
      {pendingFile && (
        <p className="text-xs text-muted-foreground">선택됨: {pendingFile.name}</p>
      )}
      <Input key={inputKey} type="file" accept={accept} onChange={handleSelect} />
      {pendingFile && (
        <div className="flex gap-2">
          <Button size="sm" onClick={handleSave} disabled={uploading}>
            {uploading ? "저장 중..." : "이미지 저장"}
          </Button>
          <Button size="sm" variant="outline" onClick={resetPending} disabled={uploading}>
            취소
          </Button>
        </div>
      )}
    </div>
  );
}
