-- 플레이어 브롤러 통계 테이블 추가
-- RANKED / SOLO_RANKED 배틀 타입에 한해 시즌 단위로 브롤러별 승/패/무 누적

create table if not exists player_brawler_stats
(
    player_brawler_stats_id bigint generated always as identity primary key,
    player_brawlstars_tag   varchar(45) not null,
    battle_type             varchar(45) not null,
    start_battle_date       date        not null,
    end_battle_date         date        not null,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_stats
    on player_brawler_stats (player_brawlstars_tag, battle_type, start_battle_date);

create table if not exists player_brawler_stats_item
(
    player_brawler_stats_item_id bigint generated always as identity primary key,
    player_brawler_stats_id      bigint      not null,
    brawler_brawlstars_id        bigint      not null,
    victory_count                bigint      not null,
    defeat_count                 bigint      not null,
    draw_count                   bigint      not null,
    total_count                  bigint      not null,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_stats_item
    on player_brawler_stats_item (player_brawler_stats_id, brawler_brawlstars_id);
