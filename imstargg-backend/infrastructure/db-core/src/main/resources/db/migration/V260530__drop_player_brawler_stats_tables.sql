-- V260530__drop_player_brawler_stats_tables.sql
-- 플레이어 브롤러 통계 사전집계 테이블 제거 (#38)
-- API 호출 시 battle 에서 온더플라이 계산 + Redis 캐시로 대체. battle 영구 보존이라 손실 없음.
drop table if exists player_brawler_stats_item;
drop table if exists player_brawler_stats;
