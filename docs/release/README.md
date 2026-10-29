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

- [2026.10.0](2026.10.0.md) — 2026-10-29. 브롤스타즈 자원 soft-delete 도입(`BaseSoftDeleteEntity`, 명시적 `...DeletedAtIsNull` 필터). core-batch 0.4.3 — `brawlerSyncJob` 이 게임에서 제거된 기어를 매번 "추가"로 알리던 문제 해결. core-api 0.5.1 — 브롤러 하위 항목 카탈로그에서 삭제분 제외(브롤러·이벤트는 유지, 응답 스펙 무변경). core-worker 0.2.5 — 공통 모듈 변경 동기화(동작 무변경). frontend 변경 없음. **DB**: `V261020` 수동 선적용 필요(7개 테이블 nullable `deleted_at` 추가 + 제거된 기어 8건 삭제 상태 복원, 재작성 없음).
- [2026.08.2](2026.08.2.md) — 2026-08-10. core-api 0.5.0 — 경쟁전 이벤트 조회에 옵션 `seasonKey` 추가(생략 시 현재 시즌, 응답 스펙 무변경). 시즌 해석을 `ofKey` → 범위가 닫힌 `all(clock)` 매칭으로 바꿔 과대 시즌 번호의 무한 루프를 400 즉시 거부로 전환. core-batch 0.4.2 — `BattleEventSyncItemWriter` 테스트 10케이스 추가(프로덕션 코드 무변경). frontend 0.7.2 — 이벤트 카드 통계 요약 줄 제거, RSC 의 통계 API N+1 제거(`2+N` → 2건). core-worker 0.2.4 변경 없음. 마이그레이션 없음.
- [2026.08.1](2026.08.1.md) — 2026-08-05. core-worker 0.2.4 — 갱신 쓰기 경로 8개 테이블 IDENTITY→SEQUENCE 전환으로 신규 플레이어 갱신 6~9.5초 → 1초 안팎(로컬 709ms→68ms), 모드 ID `Long` 통일. core-batch 0.4.1 / core-api 0.4.5 — 소유율 카운트 `OwnershipCountEmbedded` 추출(DDL 무변경, 응답 스펙 무변경). frontend 0.7.1 — 짧은 페이지 푸터 하단 고정, 갱신 실패 피드백 수정. **DB**: 마이그레이션 2건 수동 선적용 필요 — `V260804b`(battle int→bigint)는 전체 테이블 재작성 + ACCESS EXCLUSIVE 락이라 점검 시간대 필수.
- [2026.08.0](2026.08.0.md) — 2026-08-03. core-api 0.4.4 — 갱신이 PENDING 에 영구히 갇히던 레이스 수정(SQS 발행 전에 PENDING 커밋). frontend 0.7.0 — 홈을 로고+검색 진입점으로 단순화(이벤트·랭킹·메타·소개 섹션 제거 → 정적 페이지), 멈춘 갱신을 사용자가 재시도로 복구할 수 있도록 버튼 조건 수정. 마이그레이션 없음.
- [2026.07.1](2026.07.1.md) — 2026-07-11. **Hotfix**. core-api 0.4.3 — Redis `statistics` 캐시에서 빈 리스트(`kotlin.collections.EmptyList`) 역직렬화가 `PolymorphicTypeValidator` 에 거부되던 버그 수정(Jackson 3 마이그레이션 후행). 폴백으로 500 은 없었으나 캐시 무력화·DB 폴백 누적. 코드만 변경(마이그레이션 없음).
- [2026.07.0](2026.07.0.md) — 2026-07-11. core-api gzip 압축·페이지네이션 검증·갱신 흐름 리치 도메인화(409 경합), core-batch 랭킹 사전 갱신 배치잡·키셋 페이징·API 스펙 정규화 추적, core-worker INSERT 경합 재시도·배틀 누락 보강, frontend 홈 데이터 섹션·상세 ISR 복구·폰트 셀프호스팅·AdSense 수동 광고 전환. **DB**: 통계 집계 SEQUENCE 전환·trio 테이블 제거.
- [2026.06.3](2026.06.3.md) — 2026-06-19. frontend 신뢰 페이지 추가(`/about`·`/privacy`·`/terms` + 푸터 법적 링크·sitemap, 애드센스 정책 대응), 헤더 검색창 제거·검색 입력 variant 통합(리팩터).
- [2026.06.2](2026.06.2.md) — 2026-06-07. core-batch tasklet 트랜잭션 제거로 `idle in transaction` 해소(버그 수정), 통계 잡 기능별 하위 패키지 재구성(리팩터).
- [2026.06.1](2026.06.1.md) — 2026-06-06. 결과형 통계 item 전투수 버그 수정(`battleCount` rename + 캐시 v2), core-batch 잡 재설계(brawler sync 통합·rankingCollectJob 통합·API 스펙 추적 추가), OffsetDateTime→Instant 전환.
- [2026.06.0](2026.06.0.md) — 2026-06-05. 게임 모드 색상 백엔드 DB 관리(`battle_event_mode.color` 추가 + API 응답 + 프론트 연동), 이벤트 카드 모바일 최적화·배틀 실제시간 툴팁 등 프론트 개선.
- [2026.05.3](2026.05.3.md) — 2026-05-31. 플레이어 브롤러 통계 on-the-fly 전환 (사전집계 테이블 제거 + Redis 캐시), core-worker 캐시 eviction, GA4 연동, graceful shutdown, 도커 멀티플랫폼 in-image 빌드 CI.
- [2026.05.2](2026.05.2.md) — 2026-05-24. Feedback 도메인 rating 재설계 (API/DB/UI), 통계 Repository → Statistics 객체화, 통계 잡 평탄화, 데이터 테이블 + 게임 액센트 디자인 시스템.
- [2026.05.1](2026.05.1.md) — 2026-05-05. 시즌 목록 API, Brawler 카탈로그 분리, 플레이어/클럽/이벤트 UI 다수 개선, `stats_rank_battle` 컬럼 정리.
- [2026.05.0](2026.05.0.md) — 2026-05-02. 애플리케이션별 버전 관리 도입.
