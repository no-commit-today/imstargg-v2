# core-batch 운영/복원력 개선 설계서 (스켈레톤)

- 작성일: 2026-04-23
- 상태: **스켈레톤** — 스펙 A/B 와 독립적으로 작은 PR 로 분할해 진행 가능.
- 선행 문서: core-batch 코드 리뷰 최종 보고서 (2026-04-23)

## 1. 목적

코드 리뷰에서 식별된 **운영 가시성·복원력** 이슈를 정리한다. 데이터 정확성(A)이나 파이프라인 재설계(B) 와 독립적으로 가치 있는 작은 개선이 대부분이라 묶음으로 다룬다.

## 2. 범위 (후보)

- **2-D** `BrawlStarsApiTasklet` 이 `ServiceUnavailable` 을 삼키고 step 을 정상 완료로 처리. 알림 없음.
- **2-E** `QuerydslPagingItemReader` 사용처 `orderBy` 누락 (page skip/duplicate 위험): `BattleEventSyncJobConfig`, `StatsPlayerSyncJobConfig`.
- 3-F `QuerydslEntityCursorItemReader.isSaveState=false`, `QuerydslZeroPagingItemReader` 계약 JavaDoc 명시.
- Low-4 랭킹/로테이션 수집 잡 이상 탐지 알림 추가 (0건 응답·급감).
- Low-15 `DiscordAlertManager` 재시도·백오프.
- Low-16 `BrawlerTeamBattleRankStatsEntity.weightedRankSum` 계산 속성 통일.

## 3. 접근 방향

- 각 항목은 독립적인 작은 변경이라 플랜 단계에서 **우선순위만 정하고 각각 하나의 todo** 로 수행.
- 운영 알림 기준(`급감`, `0건`) 은 현 관측치 기반으로 임계값 확정 필요.
- `DiscordAlertManager` 재시도는 idempotent 한 webhook 이므로 exponential backoff + max 3 attempts 기본안.

## 4. 비목표

- 통계 데이터 정확성 (스펙 A).
- 팀 통계 파이프라인 구조 변경 (스펙 B).
- 파티션 매니지 잡 리팩토링, `BrawlerSyncJob`/`OwnershipJob` 성능 개선 (별도 스펙).
