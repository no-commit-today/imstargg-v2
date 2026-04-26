-- 집계 테이블 자연키 unique 인덱스 추가 (멱등 재적재 대비 + 중복 방어선)

create unique index if not exists uk_brawler_battle_result_stats__1
    on brawler_battle_result_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_battle_rank_stats__1
    on brawler_battle_rank_stats (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);

create unique index if not exists uk_brawler_team_battle_result_stats__1
    on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_team_battle_rank_stats__1
    on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date, team_key);

create unique index if not exists uk_brawler_enemy_battle_result_stats__1
    on brawler_enemy_battle_result_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, enemy_brawler_brawlstars_id
    );
