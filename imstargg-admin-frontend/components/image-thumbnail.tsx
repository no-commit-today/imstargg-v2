"use client";

import { useEffect, useState } from "react";
import { ImageOff } from "lucide-react";

interface ImageThumbnailProps {
  url: string | null;
  alt: string;
}

export function ImageThumbnail({ url, alt }: ImageThumbnailProps) {
  const [error, setError] = useState(false);

  useEffect(() => {
    setError(false);
  }, [url]);

  if (!url || error) {
    return (
      <div className="flex h-10 w-10 items-center justify-center rounded border bg-muted">
        <ImageOff className="h-4 w-4 text-muted-foreground" />
      </div>
    );
  }

  return (
    <img
      src={url}
      alt={alt}
      className="h-10 w-10 rounded border object-contain"
      onError={() => setError(true)}
    />
  );
}
