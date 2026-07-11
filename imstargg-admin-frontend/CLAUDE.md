<!-- markdownlint-disable-next-line MD041 -->
@AGENTS.md

# imstargg-admin-frontend

core-admin 백엔드의 관리자 프론트엔드. Next.js App Router 이지만 **전 페이지 클라이언트 컴포넌트**(`"use client"`) — RSC·TanStack Query 미사용. 사용자 프론트(imstargg-frontend)와 스택·컨벤션이 다르므로 패턴을 서로 복사하지 말 것.

## 명령어

```bash
pnpm dev              # 개발 서버 (core-admin 백엔드 필요: localhost:8080)
pnpm build            # 프로덕션 빌드
pnpm lint
```

테스트 프레임워크 없음 — 검증은 `pnpm lint && pnpm exec tsc --noEmit && pnpm build`. CI(pr-test.yml)·버전 관리(`/release`)·Deploy 워크플로 대상 아님.

## 구조

- `app/` — 라우트: brawlers, gears, events, event-modes, maintenances, api-specs, feedbacks. 모든 페이지는 `AdminLayout`(사이드바, `app/admin-layout.tsx`) 래퍼 사용
- `lib/api.ts` — 모든 백엔드 호출이 거치는 `request<T>()` wrapper. base URL: `NEXT_PUBLIC_API_URL`(기본 `http://localhost:8080`) — 사용자 프론트의 `NEXT_PUBLIC_API_BASE_URL` 과 이름이 다름
- `lib/types.ts` — API 응답 타입. 다국어 필드는 `Record<Language, string>` (`KOREAN | ENGLISH`)
- `components/ui/` — shadcn. 이미지 공용 컴포넌트: `ImageUploadField` / `ImageThumbnail` / `PendingImageInput`
- 인증 없음 (로그인·미들웨어 없음)

## 패턴

- 데이터 로드: `useCallback` + `useEffect`. 뮤테이션은 핸들러에서 API 직접 호출 → 성공 시 `toast.success` + 데이터 reload, 실패 시 `toast.error` (sonner)
- 폼: react-hook-form 미사용 — 제어 컴포넌트 + 핸들러에서 수동 검증
- 이미지 업로드: `FormData` 사용 (`request()` 가 Content-Type 헤더 자동 제거)
- `useSearchParams()` 사용 시 Suspense 래퍼 필수 (예: feedbacks 페이지)
- eslint `react-hooks/set-state-in-effect` 주의: useEffect 안에서 prop 파생 setState 금지 → `useMemo` 파생 + cleanup 전용 effect (object URL 등)
