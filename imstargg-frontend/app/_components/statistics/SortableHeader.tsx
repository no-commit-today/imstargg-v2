"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { type SortOrder, toggleSortOrder } from "@/domain/statistics/sort";

type Props = {
  column: string;
  label: string;
  currentSort: string;
  currentOrder: SortOrder;
  /** "" | "pair" | "enemy" — sort/order 파라미터 prefix. 비어 있으면 sort/order 직접 사용. */
  paramPrefix?: string;
};

function paramName(prefix: string | undefined, base: "sort" | "order"): string {
  if (!prefix) return base;
  return `${prefix}${base.charAt(0).toUpperCase()}${base.slice(1)}`;
}

export function SortableHeader({
  column,
  label,
  currentSort,
  currentOrder,
  paramPrefix,
}: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const isActive = currentSort === column;

  function onClick() {
    const sp = new URLSearchParams(params);
    const sortKey = paramName(paramPrefix, "sort");
    const orderKey = paramName(paramPrefix, "order");
    const nextOrder: SortOrder = isActive ? toggleSortOrder(currentOrder) : "DESC";
    sp.set(sortKey, column);
    sp.set(orderKey, nextOrder);
    router.replace(`${pathname}?${sp.toString()}`, { scroll: false });
  }

  const arrow = !isActive ? "" : currentOrder === "DESC" ? "▼" : "▲";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-1 text-xs font-bold uppercase tracking-wider ${
        isActive ? "text-[#14161B]" : "text-[#5C6271] hover:text-[#14161B]"
      }`}
    >
      {label}
      {arrow && <span aria-hidden>{arrow}</span>}
    </button>
  );
}
