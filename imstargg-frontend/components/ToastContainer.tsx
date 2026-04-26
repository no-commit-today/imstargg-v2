"use client";
import { useEffect, useState } from "react";
import { getToasts, subscribeToasts, type Toast } from "@/lib/toast";

export default function ToastContainer() {
  const [list, setList] = useState<Toast[]>(getToasts());
  useEffect(() => subscribeToasts(setList), []);
  return (
    <div className="toast-container">
      {list.map((t) => (
        <div key={t.id} className={`toast ${t.kind}`}>
          {t.kind === "error" && "⚠️"}
          {t.kind === "success" && "✓"}
          <span>{t.message}</span>
        </div>
      ))}
    </div>
  );
}
