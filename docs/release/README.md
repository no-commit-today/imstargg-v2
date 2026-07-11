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

- [2026.07.0](2026.07.0.md) — 2026-07-11. core-api gzip 압축·페이지네이션 검증·갱신 흐름 리치 도메인화(409 경합), core-batch 랭킹 사전 갱신 배치잡·키셋 페이징·API 스펙 정규화 추적, core-worker INSERT 경합 재시도·배틀 누락 보강, frontend 홈 데이터 섹션·상세 ISR 복구·폰트 셀프호스팅·AdSense 수동 광고 전환. **DB**: 통계 집계 SEQUENCE 전환·trio 테이블 제거.
- [2026.06.3](2026.06.3.md) — 2026-06-19. frontend 신뢰 페이지 추가(`/about`·`/privacy`·`/terms` + 푸터 법적 링크·sitemap, 애드센스 정책 대응), 헤더 검색창 제거·검색 입력 variant 통합(리팩터).
- [2026.06.2](2026.06.2.md) — 2026-06-07. core-batch tasklet 트랜잭션 제거로 `idle in transaction` 해소(버그 수정), 통계 잡 기능별 하위 패키지 재구성(리팩터).
- [2026.06.1](2026.06.1.md) — 2026-06-06. 결과형 통계 item 전투수 버그 수정(`battleCount` rename + 캐시 v2), core-batch 잡 재설계(brawler sync 통합·rankingCollectJob 통합·API 스펙 추적 추가), OffsetDateTime→Instant 전환.
- [2026.06.0](2026.06.0.md) — 2026-06-05. 게임 모드 색상 백엔드 DB 관리(`battle_event_mode.color` 추가 + API 응답 + 프론트 연동), 이벤트 카드 모바일 최적화·배틀 실제시간 툴팁 등 프론트 개선.
- [2026.05.3](2026.05.3.md) — 2026-05-31. 플레이어 브롤러 통계 on-the-fly 전환 (사전집계 테이블 제거 + Redis 캐시), core-worker 캐시 eviction, GA4 연동, graceful shutdown, 도커 멀티플랫폼 in-image 빌드 CI.
- [2026.05.2](2026.05.2.md) — 2026-05-24. Feedback 도메인 rating 재설계 (API/DB/UI), 통계 Repository → Statistics 객체화, 통계 잡 평탄화, 데이터 테이블 + 게임 액센트 디자인 시스템.
- [2026.05.1](2026.05.1.md) — 2026-05-05. 시즌 목록 API, Brawler 카탈로그 분리, 플레이어/클럽/이벤트 UI 다수 개선, `stats_rank_battle` 컬럼 정리.
- [2026.05.0](2026.05.0.md) — 2026-05-02. 애플리케이션별 버전 관리 도입.
