"use client";

import { type ReactNode } from "react";
import type { SortOrder } from "@/domain/statistics/sort";

type Props<C extends string> = {
  column: C;
  label: ReactNode;
  title?: string;
  currentSort: C;
  currentOrder: SortOrder;
  onSort: (column: C) => void;
};

export function SortableHeader<C extends string>({
  column,
  label,
  title,
  currentSort,
  currentOrder,
  onSort,
}: Props<C>) {
  const isActive = currentSort === column;
  const arrow = !isActive ? "" : currentOrder === "DESC" ? "▼" : "▲";
  return (
    <button
      type="button"
      data-sortable=""
      data-active={isActive ? "true" : undefined}
      onClick={() => onSort(column)}
      title={title}
      className="inline-flex items-center gap-0.5 font-bold tracking-wider"
    >
      {label}
      {arrow && <span aria-hidden>{arrow}</span>}
    </button>
  );
}
