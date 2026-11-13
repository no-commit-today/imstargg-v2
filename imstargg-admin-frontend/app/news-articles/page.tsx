"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getNewsArticles } from "@/lib/api/brawlstars/news-article";
import type { NewsArticleListItem, NewsArticleType } from "@/lib/types";
import { formatSeoulTime } from "./_components/formatSeoulTime";
import { NewsArticleTypeSelect } from "./_components/NewsArticleTypeSelect";

export default function NewsArticlesPage() {
  const [articles, setArticles] = useState<NewsArticleListItem[] | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setArticles(await getNewsArticles());
    } catch {
      toast.error("공식 뉴스 글을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // 목록을 다시 읽으면 스크롤이 튀므로 그 행의 유형만 바꾼다
  const handleTypeChanged = (id: number, type: NewsArticleType) => {
    setArticles((prev) => prev && prev.map((a) => (a.id === id ? { ...a, type } : a)));
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">공식 뉴스</h2>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}

      {articles && (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>유형</TableHead>
                <TableHead>제목</TableHead>
                <TableHead>게시일</TableHead>
                <TableHead>처음 본 시각</TableHead>
                <TableHead>판 수</TableHead>
                <TableHead>이력</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {articles.map((a) => (
                <TableRow key={a.id}>
                  <TableCell>
                    <NewsArticleTypeSelect
                      key={`${a.id}-${a.type}`}
                      id={a.id}
                      initialType={a.type}
                      onChanged={(type) => handleTypeChanged(a.id, type)}
                    />
                  </TableCell>
                  <TableCell>
                    <a
                      href={a.linkUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="underline"
                    >
                      {a.title}
                    </a>
                  </TableCell>
                  <TableCell>{formatSeoulTime(a.publishedAt)}</TableCell>
                  <TableCell>{formatSeoulTime(a.seenAt)}</TableCell>
                  <TableCell>{a.versionCount}</TableCell>
                  <TableCell>
                    <Link href={`/news-articles/${a.id}`}>
                      <Button variant="outline" size="sm">
                        이력
                      </Button>
                    </Link>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {articles.length === 0 && (
            <p className="mt-4 text-sm text-neutral-500">수집된 글이 없습니다.</p>
          )}
        </>
      )}
    </AdminLayout>
  );
}
