"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { toast } from "sonner";
import { AdminLayout } from "../../admin-layout";
import { Button } from "@/components/ui/button";
import { getNewsArticleDetail, getNewsArticleVersions } from "@/lib/api/brawlstars/news-article";
import type { NewsArticleDetail, NewsArticleType, NewsArticleVersion } from "@/lib/types";
import { NewsArticleDiffView } from "../_components/NewsArticleDiffView";
import { NewsArticleTypeSelect } from "../_components/NewsArticleTypeSelect";
import { formatSeoulTime } from "../_components/formatSeoulTime";

export default function NewsArticleHistoryPage() {
  const params = useParams();
  const id = Number(params?.id);
  const [detail, setDetail] = useState<NewsArticleDetail | null>(null);
  const [versions, setVersions] = useState<NewsArticleVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [failedId, setFailedId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) return;
    setLoading(true);
    try {
      const [d, v] = await Promise.all([getNewsArticleDetail(id), getNewsArticleVersions(id)]);
      setDetail(d);
      setVersions(v);
      setFailedId(null);
    } catch {
      toast.error("글을 불러오지 못했습니다.");
      setFailedId(id);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  // id 가 바뀐 직후 이전 글이 잠깐 보이지 않도록 현재 id 의 데이터만 그린다
  const current = detail && detail.id === id ? detail : null;
  const notFound = !Number.isFinite(id) || failedId === id;

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">공식 뉴스 이력</h2>
        <Link href="/news-articles">
          <Button variant="outline" size="sm">
            목록으로
          </Button>
        </Link>
      </div>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}

      {notFound && <p className="text-sm text-neutral-500">글을 찾을 수 없습니다.</p>}

      {!notFound && current && (
        <>
          <div className="mb-4 space-y-2">
            <a
              href={current.linkUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-lg font-semibold underline"
            >
              {current.title}
            </a>
            <div className="flex items-center gap-2 text-sm text-neutral-500">
              <span>유형</span>
              <NewsArticleTypeSelect
                key={current.id}
                id={current.id}
                initialType={current.type}
                onChanged={(type: NewsArticleType) =>
                  setDetail((prev) => (prev ? { ...prev, type } : prev))
                }
              />
            </div>
            <p className="text-sm text-neutral-500">게시일: {formatSeoulTime(current.publishedAt)}</p>
            <p className="text-sm text-neutral-500">
              이 판을 처음 본 시각: {formatSeoulTime(current.seenAt)}
            </p>
          </div>

          <div className="flex flex-col gap-4 md:flex-row">
            <ul className="w-full shrink-0 space-y-1 md:w-72">
              {versions.map((v) => (
                <li key={v.id}>
                  <Link
                    href={`/news-articles/${v.id}`}
                    aria-current={v.id === current.id ? "page" : undefined}
                    className={`block rounded-md border px-3 py-2 text-sm ${
                      v.id === current.id
                        ? "border-primary bg-muted font-semibold"
                        : "hover:bg-muted/50"
                    }`}
                  >
                    <div>{formatSeoulTime(v.seenAt)}</div>
                    <div className="truncate text-neutral-500">{v.title}</div>
                  </Link>
                </li>
              ))}
            </ul>

            <div className="min-w-0 flex-1 space-y-2">
              <p className="text-sm text-neutral-500">
                {current.previousSeenAt
                  ? `직전 판(${formatSeoulTime(current.previousSeenAt)}) → 이 판(${formatSeoulTime(current.seenAt)})`
                  : `첫 판(${formatSeoulTime(current.seenAt)})`}
              </p>
              <NewsArticleDiffView
                previousContent={current.previousContent}
                content={current.content}
              />
            </div>
          </div>
        </>
      )}
    </AdminLayout>
  );
}
