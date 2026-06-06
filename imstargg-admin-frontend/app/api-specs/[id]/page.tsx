"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../admin-layout";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiSpecDiffView } from "../_components/ApiSpecDiffView";
import { getApiSpecDetail } from "@/lib/api/brawlstars/api-spec";
import type { ApiSpecDetail } from "@/lib/types";
import { toast } from "sonner";

export default function ApiSpecDetailPage() {
  const params = useParams();
  const id = Number(params?.id);
  const [spec, setSpec] = useState<ApiSpecDetail | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) return;
    setLoading(true);
    try {
      setSpec(await getApiSpecDetail(id));
    } catch {
      toast.error("스냅샷을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">API 스펙 스냅샷 #{id}</h2>
        <Link href="/api-specs">
          <Button variant="outline" size="sm">
            목록으로
          </Button>
        </Link>
      </div>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}

      {spec && (
        <>
          <div className="mb-4 text-sm text-neutral-500">
            <p>수집 시각: {new Date(spec.collectedAt).toLocaleString("ko-KR")}</p>
            {spec.previousCollectedAt && (
              <p>
                이전 스냅샷:{" "}
                <Link href={`/api-specs/${spec.previousId}`} className="underline">
                  #{spec.previousId}
                </Link>{" "}
                ({new Date(spec.previousCollectedAt).toLocaleString("ko-KR")})
              </p>
            )}
          </div>

          <Tabs defaultValue="diff" className="space-y-4">
            <TabsList>
              <TabsTrigger value="diff">이전과 비교</TabsTrigger>
              <TabsTrigger value="content">원문</TabsTrigger>
            </TabsList>
            <TabsContent value="diff">
              {spec.previousContent ? (
                <ApiSpecDiffView previousContent={spec.previousContent} content={spec.content} />
              ) : (
                <p className="text-sm text-neutral-500">
                  이전 스냅샷이 없습니다. (최초 수집 스냅샷)
                </p>
              )}
            </TabsContent>
            <TabsContent value="content">
              <pre className="overflow-x-auto rounded-md border bg-muted/30 p-4 text-xs leading-5">
                {spec.content}
              </pre>
            </TabsContent>
          </Tabs>
        </>
      )}
    </AdminLayout>
  );
}
