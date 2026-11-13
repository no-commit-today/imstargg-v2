import { request } from "@/lib/api";
import type {
  NewsArticleDetail,
  NewsArticleListItem,
  NewsArticleType,
  NewsArticleVersion,
} from "@/lib/types";

export async function getNewsArticles(): Promise<NewsArticleListItem[]> {
  return request<NewsArticleListItem[]>("/news-articles");
}

export async function getNewsArticleDetail(id: number): Promise<NewsArticleDetail> {
  return request<NewsArticleDetail>(`/news-articles/${id}`);
}

export async function getNewsArticleVersions(id: number): Promise<NewsArticleVersion[]> {
  return request<NewsArticleVersion[]>(`/news-articles/${id}/versions`);
}

export async function updateNewsArticleType(id: number, type: NewsArticleType): Promise<void> {
  return request<void>(`/news-articles/${id}/type`, {
    method: "PUT",
    body: JSON.stringify({ type }),
  });
}
