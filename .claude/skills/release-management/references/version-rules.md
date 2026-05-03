# Version Rules

이 모노레포의 버전 규칙. 스킬과 스크립트 모두 이 규칙을 따른다.

## 형식

각 앱은 SemVer 2.0 의 `MAJOR.MINOR.PATCH` 또는 SNAPSHOT 형태:

- `0.1.0` — finalized (운영 배포 가능 상태)
- `0.1.0-SNAPSHOT` — dev 단계 (다음 main PR 에서 finalize 될 후보)

## SNAPSHOT bump 매트릭스

| 현재 상태 | 요청된 bump | 결과 | 비고 |
|---|---|---|---|
| `X.Y.Z` (finalized) | patch | `X.Y.(Z+1)-SNAPSHOT` | 첫 작업 시 SNAPSHOT 으로 진입 |
| `X.Y.Z` (finalized) | minor | `X.(Y+1).0-SNAPSHOT` | |
| `X.Y.Z` (finalized) | major | `(X+1).0.0-SNAPSHOT` | |
| `X.Y.Z-SNAPSHOT` (patch kind) | patch | **변경 없음** | 같은 종류 누적 |
| `X.Y.Z-SNAPSHOT` (patch kind) | minor | `X.(Y+1).0-SNAPSHOT` | 격상 |
| `X.Y.Z-SNAPSHOT` (patch kind) | major | `(X+1).0.0-SNAPSHOT` | 격상 |
| `X.Y.0-SNAPSHOT` (minor kind) | patch | **변경 없음** | 더 작은 종류 무시 |
| `X.Y.0-SNAPSHOT` (minor kind) | minor | **변경 없음** | 같은 종류 |
| `X.Y.0-SNAPSHOT` (minor kind) | major | `(X+1).0.0-SNAPSHOT` | 격상 |
| `X.0.0-SNAPSHOT` (major kind) | 어느 것 | **변경 없음** | 이미 최대 |

### "Snapshot kind" 추론

스크립트는 SNAPSHOT 의 종류를 다음 규칙으로 추론한다 (이전 finalized 버전을 모를 때):

- `X.0.0-SNAPSHOT` → major kind
- `X.Y.0-SNAPSHOT` (Y > 0) → minor kind
- `X.Y.Z-SNAPSHOT` (Z > 0) → patch kind

이 휴리스틱은 대부분 정확하지만 완벽하진 않다. 더 정밀하게 하려면 `bump_version.py` 의 `--prev` 옵션으로 직전 finalized 버전을 명시하면 된다.

## Bump 종류 판단 가이드 (Claude 가 diff 보고 결정)

| 종류 | 신호 (변경 내용) | 예시 |
|---|---|---|
| **patch** | 버그 수정, 내부 리팩토링, 문서, 테스트 추가, dependency patch up | "fix: 갱신 polling race condition", 변수명 변경, JSDoc 추가 |
| **minor** | 새 기능 추가, 호환 유지된 변경, public API 추가, config 옵션 추가 | 새 엔드포인트 추가, 새 옵션 prop 추가, 새 페이지 |
| **major** | breaking change | API 응답 형식 변경, env 필수 추가, DB 스키마 forward-only, 메서드 시그니처 변경, default 동작 변경, 컴포넌트 prop 제거 |

### 판단 원칙

- **의심스러우면 작은 쪽으로** (patch). 격상은 나중에 또 가능.
- **breaking 의심되면 사용자 명시 confirm**. 무인 모드라도 강제 검수.
- **0.x.y 는 SemVer spec 상 minor 도 breaking 가능**하지만, 운영 안정성을 위해 같은 규칙 적용.
- **여러 종류 변경이 한 PR 에 섞이면 가장 큰 종류로** (예: patch fix + minor 새 기능 → minor).

## 라이브러리 / 공통 모듈 변경

`imstargg-backend/{infrastructure,support,client,core/core-enum}/` 의 변경은 의존하는 모든 bootable backend 앱에 같은 종류로 bump 적용:

- 영향 받는 앱: core-api, core-batch, core-worker
- core-admin 은 관리 대상 외이므로 영향 받아도 무시

## 무시되는 변경

다음은 bump 대상이 아님:

- `imstargg-backend/core/core-admin/**`
- `imstargg-admin-frontend/**`
- `docs/**`
- `.github/**`
- root 설정 (`.gitignore`, `README.md` at root, etc.)
- `.claude/**` (스킬 자체 변경 등)

## CalVer 결정 (묶음 릴리즈 노트)

- 형식: `YYYY.MM.N` (예: `2026.05.0`, `2026.05.1`)
- N 은 같은 달의 0-based 카운터
- 결정 방법: `ls docs/release/<YYYY>.<MM>.*.md` 의 최대값 + 1
