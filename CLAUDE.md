# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

브롤스타즈 게임 통계 서비스. 모노레포 구조.

## 도메인 지식 (브롤스타즈)

- **브롤러**: 약 100개 존재하는 플레이어블 캐릭터
- **전투 유형**:
  - 결과형(승/패/무): 주로 3v3, 1v1/2v2/5v5도 존재
  - 순위형(1위~N위): 개인전(1명×10팀), 듀오(2명×5팀), 트리오(3명×4팀)
- **이벤트**: 전투 맵. 총 약 100개 맵 중 로테이션으로 약 10개 활성화 (활성화 시간은 맵마다 상이, 브롤패스 시즌에 따라 변경)
  - 새로운 이벤트가 시즌별로 추가
- **경쟁전(Solo Rank)**: 약 20개 맵, 경쟁전 시즌마다 변경 가능

## 모노레포 구조

```
imstargg-v2/
├── imstargg-backend/           # Spring Boot + Kotlin 백엔드
│   └── core/
│       ├── core-api            # 사용자용 REST API (프론트엔드: imstargg-frontend)
│       ├── core-admin          # 관리자용 REST API (프론트엔드: imstargg-admin-frontend)
│       ├── core-worker         # 백그라운드 워커 (배틀/플레이어 동기화 등)
│       └── core-batch          # 배치 작업 (스케줄링/집계 등)
├── imstargg-frontend/          # Next.js + TypeScript — core-api 클라이언트
└── imstargg-admin-frontend/    # Next.js + TypeScript — core-admin 클라이언트
```

**API 변경 시 동기화**: 백엔드 API 스펙(요청/응답 DTO, 경로, 상태코드 등)이 변경되면 대응되는 프론트엔드에 반드시 반영

- `core-api` 변경 → `imstargg-frontend`
- `core-admin` 변경 → `imstargg-admin-frontend`

## Git 전략

`release` 브랜치 없는 Git Flow. `main`(운영) · `develop`(통합 개발) 2개 상시 브랜치.

| 브랜치 | 역할 | 갱신 방식 |
|---|---|---|
| `main` | 운영 배포 대상. finalized 버전(SemVer) | `develop` → `main` PR 병합 + 배포 봇 이미지 태그 커밋 |
| `develop` | 통합 개발 브랜치. SNAPSHOT 버전 | 기능 브랜치 PR 병합 (+ 필요 시 `main` backmerge) |
| `feature/<설명>` | 기능 개발 | `develop`에서 분기 → `develop`로 PR |
| `hotfix/<설명>` | 운영 긴급 수정 | `main`에서 분기 → `main` 반영 후 `develop` backmerge |

- **명명**: 접두사 풀워드(`feature/` `fix/` `refactor/` `docs/` `chore/` `hotfix/`) + kebab-case 설명. 브랜치 접두사 `feature/` 와 Conventional Commits 타입 `feat:` 혼동 금지.

**흐름**: `develop`에서 기능 브랜치 분기 → 작업 → `develop`로 PR(`pr-test.yml` 이 변경 영역 backend/frontend 테스트 — admin-frontend 는 CI 테스트 없음, 로컬 검증 필수) → 배포 준비되면 `develop` → `main` PR → 병합 후 `Deploy` 워크플로로 백엔드 앱(core-api/worker/batch) 수동 배포.

**커밋·버전**: 커밋은 Conventional Commits(`feat:` `fix:` `refactor:` `docs:` `chore:` `ci:`, scope 예 `ci(deploy):`). 버전은 앱별 독립 SemVer(`develop`=SNAPSHOT, `main`=finalized) — bump·main PR 준비·hotfix·릴리즈 노트는 `/release` 스킬로 자동화(`.claude/skills/release-management/`). core-admin·admin-frontend 는 버전 관리 대상 외.

## 하위 CLAUDE.md 참조

- `imstargg-backend/CLAUDE.md` - 백엔드 빌드, 테스트, 아키텍처
- `imstargg-frontend/CLAUDE.md` - 사용자 프론트엔드 개발 가이드
- `imstargg-admin-frontend/CLAUDE.md` - 관리자 프론트엔드 개발 가이드
