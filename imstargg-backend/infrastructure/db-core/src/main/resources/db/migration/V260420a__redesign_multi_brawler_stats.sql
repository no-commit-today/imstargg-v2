-- 여러 브롤러 통계 테이블 재설계
-- 기존 (period_type, battle_date) 키 -> (start_battle_date, end_battle_date) 로 변경.
-- 서비스 미운영 상태로, 데이터 마이그레이션 없이 drop + recreate 로 진행한다.

drop table if exists brawler_team_battle_result_stats_brawler;
drop table if exists brawler_team_battle_rank_stats_brawler;
drop table if exists brawler_enemy_battle_result_stats;
drop table if exists brawler_team_battle_result_stats;
drop table if exists brawler_team_battle_rank_stats;

create table if not exists brawler_enemy_battle_result_stats
(
    brawler_enemy_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id                  bigint       not null,
    tier_range                           varchar(45)  not null,
    start_battle_date                    date         not null,
    end_battle_date                      date         not null,
    brawler_brawlstars_id                bigint       not null,
    enemy_brawler_brawlstars_id          bigint       not null,
    victory_count                        bigint       not null,
    defeat_count                         bigint       not null,
    draw_count                           bigint       not null,
    total_count                          bigint       not null,
    created_at                           timestamptz  not null default now(),
    updated_at                           timestamptz  not null default now()
);
create index if not exists ix_brawler_enemy_battle_result_stats_1 on brawler_enemy_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date);
create index if not exists ix_brawler_enemy_battle_result_stats_2 on brawler_enemy_battle_result_stats (brawler_brawlstars_id, tier_range, start_battle_date, end_battle_date);

create table if not exists brawler_team_battle_result_stats
(
    brawler_team_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id                 bigint       not null,
    tier_range                          varchar(45)  not null,
    start_battle_date                   date         not null,
    end_battle_date                     date         not null,
    team_size                           int          not null,
    team_key                            varchar(100) not null,
    victory_count                       bigint       not null,
    defeat_count                        bigint       not null,
    draw_count                          bigint       not null,
    total_count                         bigint       not null,
    created_at                          timestamptz  not null default now(),
    updated_at                          timestamptz  not null default now()
);
create index if not exists ix_brawler_team_battle_result_stats_1 on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date);

create table if not exists brawler_team_battle_result_stats_brawler
(
    brawler_team_battle_result_stats_brawler_id bigint generated always as identity primary key,
    brawler_team_battle_result_stats_id         bigint      not null,
    brawler_brawlstars_id                       bigint      not null,
    created_at                                  timestamptz not null default now(),
    updated_at                                  timestamptz not null default now()
);
create index if not exists ix_brawler_team_battle_result_stats_brawler_1 on brawler_team_battle_result_stats_brawler (brawler_brawlstars_id, brawler_team_battle_result_stats_id);

create table if not exists brawler_team_battle_rank_stats
(
    brawler_team_battle_rank_stats_id bigint generated always as identity primary key,
    event_brawlstars_id               bigint       not null,
    tier_range                        varchar(45)  not null,
    start_battle_date                 date         not null,
    end_battle_date                   date         not null,
    team_size                         int          not null,
    team_key                          varchar(100) not null,
    rank_counts                       jsonb        not null,
    total_count                       bigint       not null,
    weighted_rank_sum                 bigint       not null,
    created_at                        timestamptz  not null default now(),
    updated_at                        timestamptz  not null default now()
);
create index if not exists ix_brawler_team_battle_rank_stats_1 on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, start_battle_date, end_battle_date);

create table if not exists brawler_team_battle_rank_stats_brawler
(
    brawler_team_battle_rank_stats_brawler_id bigint generated always as identity primary key,
    brawler_team_battle_rank_stats_id         bigint      not null,
    brawler_brawlstars_id                     bigint      not null,
    created_at                                timestamptz not null default now(),
    updated_at                                timestamptz not null default now()
);
create index if not exists ix_brawler_team_battle_rank_stats_brawler_1 on brawler_team_battle_rank_stats_brawler (brawler_brawlstars_id, brawler_team_battle_rank_stats_id);
