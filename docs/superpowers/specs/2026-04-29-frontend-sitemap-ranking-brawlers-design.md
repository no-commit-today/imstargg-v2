# Sitemap — Ranking Brawler 페이지 추가 Frontend Design Spec

**작성일**: 2026-04-29
**상태**: Draft → 승인 대기

## 목적

`app/sitemap.ts` 에 누락된 `/ranking/brawlers/{id}` (브롤러별 랭킹 상세) URL 을 추가한다. 데이터는 이미 `brawlerApi.list` 호출로 확보 중이므로 새 fetch 없이 사이트맵 항목만 한 번 더 매핑하면 된다.

## 현재 상태

`app/sitemap.ts` (47 줄) 에는 정적 페이지 7 종 + 모든 브롤러(`/brawlers/{id}`) + 활성 이벤트(`/events/{id}`) 가 포함되어 있다. `brawlerApi.list({ next: { revalidate: 86400 } })` 결과를 사용 중. `/ranking/brawlers/{id}` 는 페이지가 존재하지만 sitemap 에 없어 검색 노출이 약하다.

## Goals

1. `app/sitemap.ts` 의 결과 배열에 `${base}/ranking/brawlers/{id}` 형식의 URL 이 모든 브롤러 ID 에 대해 포함된다.
2. `brawlerApi.list` 호출은 한 번만 이루어진다 (기존 호출 결과 재사용).
3. 기존 정적 페이지 / 브롤러 / 이벤트 entries 는 변경되지 않는다.
4. 빌드 후 `/sitemap.xml` 응답에 `<loc>https://imstargg.com/ranking/brawlers/{id}</loc>` 가 모든 브롤러에 대해 노출된다.
5. `brawlerApi.list` 가 503 등으로 실패하면 기존 try/catch 로 ranking brawler entries 도 빈 배열로 fallback (현재 동작 보존).
6. `pnpm typecheck` / `pnpm lint` / `pnpm build` 통과.

## Non-goals

- 인기 플레이어 / 클럽 sitemap 추가 (별도 spec).
- `priority` / `changeFrequency` 의 글로벌 재조정.
- robots.ts 변경.

## 설계

### 변경 위치

`imstargg-frontend/app/sitemap.ts` 의 brawlerEntries 매핑 직후, 동일 try 블록 안에서 `rankingBrawlerEntries` 를 같이 만든다. 새 fetch 없음.

```diff
   let brawlerEntries: MetadataRoute.Sitemap = [];
+  let rankingBrawlerEntries: MetadataRoute.Sitemap = [];
   try {
     const list = await brawlerApi.list({ next: { revalidate: 86400 } });
     brawlerEntries = list.content.map((b) => ({
       url: `${base}/brawlers/${b.id}`,
       lastModified: now,
       changeFrequency: "weekly",
       priority: 0.5,
     }));
+    rankingBrawlerEntries = list.content.map((b) => ({
+      url: `${base}/ranking/brawlers/${b.id}`,
+      lastModified: now,
+      changeFrequency: "hourly",
+      priority: 0.6,
+    }));
   } catch {
     brawlerEntries = [];
+    rankingBrawlerEntries = [];
   }
```

`return` 배열의 `...brawlerEntries` 다음 줄에 `...rankingBrawlerEntries` 를 추가한다.

### 메타 값 근거
- `changeFrequency: "hourly"` — 랭킹은 자주 변동. 다른 ranking 정적 페이지들과 동일.
- `priority: 0.6` — `/ranking/brawlers` 메타 페이지(0.6) 와 동급. 개별 브롤러 메타 페이지(0.5) 보다 ranking 디테일이 살짝 높은 가치.

## 테스팅 전략

### 단위 테스트
- 만들지 않는다. `sitemap.ts` 는 외부 API 의존이고 메인 로직은 단순 매핑. 회귀는 빌드 + curl 로 충분.

### 빌드/메타 검증
- `pnpm build` 통과.
- 빌드 후 `pnpm start` 또는 prod 배포 후 `curl https://imstargg.com/sitemap.xml | grep ranking/brawlers` 로 entries 가 모든 브롤러에 대해 노출되는지 확인.
- 또는 dev 서버에서 `curl http://localhost:3000/sitemap.xml | grep -c ranking/brawlers` 가 `brawlerApi.list` content 의 길이와 동일.

### 정적 검증
- `pnpm typecheck` / `pnpm lint` 통과.

## 위험과 대응

| 위험 | 대응 |
|---|---|
| `brawlerApi.list` 503 시 ranking 도 빈 배열 → SEO 누락 | 의도된 fallback. 일시 장애. 다음 revalidate(24h) 에 회복. |
| 새 브롤러 추가 시 sitemap 갱신 지연 | `revalidate: 86400` (24h) — 도메인 특성상 충분. |
| sitemap entry 수 폭증 | 현재 약 100 브롤러 + 100 이벤트 + 100 ranking 브롤러 = 약 300 entries. Google sitemap 50,000 한도와 거리 멀다. |

## 작업 산출물

이 spec 승인 후 별도 plan 문서에서 1 task 로 분해 (sitemap.ts 수정 + 검증).
