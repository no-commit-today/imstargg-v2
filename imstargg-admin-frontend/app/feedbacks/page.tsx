"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import { FeedbackFilters } from "./_components/FeedbackFilters";
import { FeedbackTable } from "./_components/FeedbackTable";
import { getFeedbacks } from "@/lib/api/operation/feedback";
import type { FeedbackCategory, FeedbackListResponse } from "@/lib/types";
import { toast } from "sonner";

const PAGE_SIZE = 20;

export default function FeedbackListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [data, setData] = useState<FeedbackListResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const category =
    (params?.get("category") as FeedbackCategory | null) ?? undefined;
  const checkedParam = params?.get("checked") ?? null;
  const checked =
    checkedParam === "true"
      ? true
      : checkedParam === "false"
        ? false
        : undefined;
  const page = Number(params?.get("page") ?? "0");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getFeedbacks({
        category,
        checked,
        page,
        size: PAGE_SIZE,
      });
      setData(res);
    } catch {
      toast.error("목록을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, [category, checked, page]);

  useEffect(() => {
    load();
  }, [load]);

  const goPage = (next: number) => {
    const sp = new URLSearchParams(params?.toString() ?? "");
    sp.set("page", String(next));
    router.replace(`/feedbacks?${sp.toString()}`);
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">피드백</h2>
        <FeedbackFilters />
      </div>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}

      {!loading && data && data.totalElements === 0 && (
        <p className="text-sm text-neutral-500">데이터가 없습니다.</p>
      )}

      {data && data.totalElements > 0 && <FeedbackTable data={data} />}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => goPage(page - 1)}
          >
            이전
          </Button>
          <span className="text-sm">
            {page + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= data.totalPages}
            onClick={() => goPage(page + 1)}
          >
            다음
          </Button>
        </div>
      )}
    </AdminLayout>
  );
}
