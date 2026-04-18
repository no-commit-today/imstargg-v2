"use client";

import { useState } from "react";
import { ImageOff } from "lucide-react";

interface ImageThumbnailProps {
  url: string | null;
  alt: string;
}

export function ImageThumbnail({ url, alt }: ImageThumbnailProps) {
  if (!url) {
    return <ImageThumbnailFallback />;
  }

  return <ImageThumbnailImg key={url} url={url} alt={alt} />;
}

function ImageThumbnailFallback() {
  return (
    <div className="flex h-10 w-10 items-center justify-center rounded border bg-muted">
      <ImageOff className="h-4 w-4 text-muted-foreground" />
    </div>
  );
}

function ImageThumbnailImg({ url, alt }: { url: string; alt: string }) {
  const [error, setError] = useState(false);

  if (error) {
    return <ImageThumbnailFallback />;
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={url}
      alt={alt}
      className="h-10 w-10 rounded border object-contain"
      onError={() => setError(true)}
    />
  );
}
