-- 팀 통계 GROUP BY team_key 최적화용 covering index.
-- 기존 ix_*_1 (event, tier, start, end) 는 유지.
create index if not exists ix_brawler_team_battle_result_stats_2
    on brawler_team_battle_result_stats
    (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);

create index if not exists ix_brawler_team_battle_rank_stats_2
    on brawler_team_battle_rank_stats
    (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_size, team_key);
