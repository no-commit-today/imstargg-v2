# Sitemap — Ranking Brawler URL 추가 Implementation Plan

**Goal:** `app/sitemap.ts` 의 brawler list 결과를 재사용해 `/ranking/brawlers/{id}` URL 을 sitemap 에 추가한다.

**Spec:** `docs/superpowers/specs/2026-04-29-frontend-sitemap-ranking-brawlers-design.md`

---

### Task 1: sitemap.ts 에 ranking 브롤러 entries 추가

**Files:** `imstargg-frontend/app/sitemap.ts`

- [ ] **Step 1: 변경**

`imstargg-frontend/app/sitemap.ts` 의 brawlerEntries try 블록 안에 `rankingBrawlerEntries` 도 같이 만들고, return 배열에 spread.

```ts
let brawlerEntries: MetadataRoute.Sitemap = [];
let rankingBrawlerEntries: MetadataRoute.Sitemap = [];
try {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  brawlerEntries = list.content.map((b) => ({
    url: `${base}/brawlers/${b.id}`,
    lastModified: now,
    changeFrequency: "weekly",
    priority: 0.5,
  }));
  rankingBrawlerEntries = list.content.map((b) => ({
    url: `${base}/ranking/brawlers/${b.id}`,
    lastModified: now,
    changeFrequency: "hourly",
    priority: 0.6,
  }));
} catch {
  brawlerEntries = [];
  rankingBrawlerEntries = [];
}
```

return 배열에 `...rankingBrawlerEntries` 추가.

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: dev 서버에서 sitemap.xml 확인**

```bash
cd imstargg-frontend
pnpm dev > /tmp/sitemap-dev.log 2>&1 &
sleep 8
/usr/bin/curl -s http://localhost:3000/sitemap.xml | grep -c ranking/brawlers
pkill -f "next dev"
```

Expected: ranking/brawlers entry 카운트가 brawlerApi.list content 길이와 동일 (약 90+).

- [ ] **Step 4: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/sitemap.ts
git commit -m "feat(frontend): sitemap 에 /ranking/brawlers/{id} URL 추가"
```
