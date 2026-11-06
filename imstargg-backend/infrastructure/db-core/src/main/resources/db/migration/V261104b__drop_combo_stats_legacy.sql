-- 조합 통계 재구성(V261104a)의 이관 소스였던 _legacy 두 테이블을 지운다.
-- 대상: brawler_pair_battle_result_stats_legacy, brawler_enemy_battle_result_stats_legacy
--
-- 배경: 스펙 B(조합 통계 재구성) §3.8 7번(DDL 2차). V261104a 가 기존 2인 조합·카운터 테이블을 _legacy 로 옮겨
--   이관 소스와 롤백 대비로 남겨 두었다. 새 테이블로 옮긴 뒤에는 읽는 곳이 없다.
--
-- 배포 순서: V261104a 배포 뒤 §3.8 6번 확인이 끝난 다음 적용. 이후 롤백 불가.
--   6번 확인: 새 테이블 버킷·행 수, 기본 티어에서 _legacy 방향 합계와 새 행 수 일치(V261104a 끝의 검증 쿼리), 3인 잡 성공,
--   trio API 응답, 화면, Redis 새 키, 다음 날 02:00 KST partitionManageJob 의 월 파티션 스텝 성공.
--
-- 적용: lock_timeout 3s 로 적용한다. _legacy 는 새 이미지가 읽지 않아 락 경합이 없다.
--
-- 롤백: 불가. 이 DDL 뒤에는 _legacy 를 원래 이름으로 되돌리는 V261104a 의 롤백 절차를 쓸 수 없다.
--
-- 멱등: drop table if exists. 시퀀스 brawler_pair_battle_result_stats_seq_legacy·brawler_enemy_battle_result_stats_seq_legacy 는
--   legacy id 컬럼에 owned by 로 묶여 있어 테이블과 함께 지워진다(인덱스·제약도 함께 지워진다).
--   이 파일을 적용한 뒤에는 V261104a 를 다시 적용하지 않는다(이관 insert 가 _legacy 없음으로 실패한다).

set lock_timeout = '3s';

drop table if exists brawler_pair_battle_result_stats_legacy;
drop table if exists brawler_enemy_battle_result_stats_legacy;
