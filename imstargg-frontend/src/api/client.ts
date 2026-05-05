import { unstable_rethrow } from "next/navigation";
import { env } from "@/config/env";
import { NetworkError, toApiError } from "@/api/errors";

export type FetchOpts = {
  next?: { revalidate?: number; tags?: string[] };
  signal?: AbortSignal;
  method?: "GET" | "POST";
  body?: unknown;
  cache?: RequestCache;
};

export async function apiFetch<T>(path: string, opts: FetchOpts = {}): Promise<T> {
  const url = `${env.NEXT_PUBLIC_API_BASE_URL}${path}`;
  const init: RequestInit & { next?: FetchOpts["next"] } = {
    method: opts.method ?? "GET",
    headers: {
      Accept: "application/json",
      ...(opts.body ? { "Content-Type": "application/json" } : {}),
    },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
    signal: opts.signal,
    cache: opts.cache,
    next: opts.next,
  };
  let res: Response;
  try {
    res = await fetch(url, init);
  } catch (e) {
    unstable_rethrow(e);
    throw new NetworkError(path, e);
  }
  if (!res.ok) throw await toApiError(res, path);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
