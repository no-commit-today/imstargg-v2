"use client";

import { useState } from "react";

export function FeedbackContentCell({ content }: { content: string }) {
  const [open, setOpen] = useState(false);
  if (!open && content.length > 60) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="text-left text-sm hover:underline"
        title={content}
      >
        {content.slice(0, 60)}…
      </button>
    );
  }
  return (
    <span
      className="whitespace-pre-wrap text-sm cursor-pointer"
      onClick={() => setOpen(false)}
    >
      {content}
    </span>
  );
}
