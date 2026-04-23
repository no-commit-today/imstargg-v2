-- rank 전투 unique key 축소: (battle_date, battle_key, player_brawlstars_tag)
-- → (battle_date, battle_key)
-- 근거: battle_key 를 자기 팀 플레이어 태그 기반으로 생성하여 팀 단위 1 row 로 dedup.
-- 서비스 미운영 시기라 기존 데이터는 없거나 truncate 가능함을 전제.

drop index if exists uk_stats_rank_battle__battle_date_battle_key_player;

create unique index if not exists uk_stats_rank_battle__battle_date_battle_key
    on stats_rank_battle (battle_date, battle_key);
