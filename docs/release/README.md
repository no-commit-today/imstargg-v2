# Release Notes

각 릴리즈마다 `docs/release/{YYYY.MM.N}.md` 파일을 생성한다.

## 작성 절차

1. `_template.md` 를 새 파일명으로 복사.
2. 각 앱의 source-of-truth 위치(아래)에서 현재 버전을 확인해 채운다.
3. 변경 사항·Breaking Changes·마이그레이션 필요 사항·롤백 주의 사항을 채운다.
4. PR 로 머지한다 — 릴리즈 노트 자체가 변경 이력이 된다.

## 앱별 버전 source-of-truth

| 앱 | 위치 |
|---|---|
| core-api | `imstargg-backend/gradle.properties` `coreApiVersion` |
| core-batch | `imstargg-backend/gradle.properties` `coreBatchVersion` |
| core-worker | `imstargg-backend/gradle.properties` `coreWorkerVersion` |
| frontend | `imstargg-frontend/package.json` `version` |

## 인덱스

- [2026.05.1](2026.05.1.md) — 2026-05-05. 시즌 목록 API, Brawler 카탈로그 분리, 플레이어/클럽/이벤트 UI 다수 개선, `stats_rank_battle` 컬럼 정리.
- [2026.05.0](2026.05.0.md) — 2026-05-02. 애플리케이션별 버전 관리 도입.
