-- 휴면 trio 통계 테이블 제거 (API/배치 미제공 상태로 사용처 없음 — 2026-07-07 설계서 참조)
-- 적용 전 확인: 두 테이블 모두 row count 0 이어야 한다.
--   select count(*) from brawler_trio_battle_result_stats;
--   select count(*) from brawler_trio_battle_rank_stats;
drop table if exists brawler_trio_battle_result_stats;
drop table if exists brawler_trio_battle_rank_stats;
