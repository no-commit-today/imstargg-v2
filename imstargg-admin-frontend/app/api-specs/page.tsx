"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { AdminLayout } from "../admin-layout";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { getApiSpecs } from "@/lib/api/brawlstars/api-spec";
import type { ApiSpecListItem } from "@/lib/types";
import { toast } from "sonner";

export default function ApiSpecListPage() {
  const [specs, setSpecs] = useState<ApiSpecListItem[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSpecs(await getApiSpecs());
    } catch {
      toast.error("목록을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">API 스펙 추적</h2>
      </div>

      <p className="mb-4 text-sm text-neutral-500">
        브롤스타즈 API 스펙(swagger)의 변경 스냅샷 이력입니다. 변경이 감지된 시점에만 새 스냅샷이
        저장됩니다.
      </p>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}

      {!loading && specs.length === 0 && (
        <p className="text-sm text-neutral-500">데이터가 없습니다.</p>
      )}

      {specs.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>수집 시각</TableHead>
              <TableHead>크기</TableHead>
              <TableHead>라인 수</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {specs.map((spec) => (
              <TableRow key={spec.id}>
                <TableCell>{spec.id}</TableCell>
                <TableCell>{new Date(spec.collectedAt).toLocaleString("ko-KR")}</TableCell>
                <TableCell>{(spec.sizeBytes / 1024).toFixed(1)} KB</TableCell>
                <TableCell>{spec.lineCount.toLocaleString()}</TableCell>
                <TableCell>
                  <Link href={`/api-specs/${spec.id}`}>
                    <Button variant="outline" size="sm">
                      상세
                    </Button>
                  </Link>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </AdminLayout>
  );
}
