-- 방향성 pair/trio 브롤러 통계 테이블 전환
-- 서비스 미운영 상태로, 기존 team 통계 테이블은 데이터 마이그레이션 없이 제거한다.

drop table if exists brawler_team_battle_result_stats_brawler;
drop table if exists brawler_team_battle_rank_stats_brawler;
drop table if exists brawler_team_battle_result_stats;
drop table if exists brawler_team_battle_rank_stats;

create table if not exists brawler_pair_battle_result_stats
(
    brawler_pair_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id                 bigint      not null,
    tier_range                          varchar(45) not null,
    start_battle_date                   date        not null,
    end_battle_date                     date        not null,
    brawler_brawlstars_id               bigint      not null,
    pair_brawler_brawlstars_id          bigint      not null,
    victory_count                       bigint      not null,
    defeat_count                        bigint      not null,
    draw_count                          bigint      not null,
    total_count                         bigint      not null,
    created_at                          timestamptz not null default now(),
    updated_at                          timestamptz not null default now(),
    constraint ck_brawler_pair_battle_result_stats_distinct_brawler
        check (brawler_brawlstars_id <> pair_brawler_brawlstars_id)
);
create index if not exists ix_brawler_pair_battle_result_stats_2 on brawler_pair_battle_result_stats (brawler_brawlstars_id, tier_range, start_battle_date, end_battle_date);
create unique index if not exists uk_brawler_pair_battle_result_stats__1
    on brawler_pair_battle_result_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, pair_brawler_brawlstars_id
    );

create table if not exists brawler_pair_battle_rank_stats
(
    brawler_pair_battle_rank_stats_id bigint generated always as identity primary key,
    event_brawlstars_id               bigint      not null,
    tier_range                        varchar(45) not null,
    start_battle_date                 date        not null,
    end_battle_date                   date        not null,
    brawler_brawlstars_id             bigint      not null,
    pair_brawler_brawlstars_id        bigint      not null,
    rank_counts                       jsonb       not null,
    total_count                       bigint      not null,
    weighted_rank_sum                 bigint      not null,
    created_at                        timestamptz not null default now(),
    updated_at                        timestamptz not null default now(),
    constraint ck_brawler_pair_battle_rank_stats_distinct_brawler
        check (brawler_brawlstars_id <> pair_brawler_brawlstars_id)
);
create unique index if not exists uk_brawler_pair_battle_rank_stats__1
    on brawler_pair_battle_rank_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, pair_brawler_brawlstars_id
    );

create table if not exists brawler_trio_battle_result_stats
(
    brawler_trio_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id                 bigint      not null,
    tier_range                          varchar(45) not null,
    start_battle_date                   date        not null,
    end_battle_date                     date        not null,
    brawler_brawlstars_id               bigint      not null,
    pair_brawler1_brawlstars_id         bigint      not null,
    pair_brawler2_brawlstars_id         bigint      not null,
    victory_count                       bigint      not null,
    defeat_count                        bigint      not null,
    draw_count                          bigint      not null,
    total_count                         bigint      not null,
    created_at                          timestamptz not null default now(),
    updated_at                          timestamptz not null default now(),
    constraint ck_brawler_trio_battle_result_stats_distinct_brawler
        check (
            brawler_brawlstars_id <> pair_brawler1_brawlstars_id
            and brawler_brawlstars_id <> pair_brawler2_brawlstars_id
            and pair_brawler1_brawlstars_id < pair_brawler2_brawlstars_id
        )
);
create unique index if not exists uk_brawler_trio_battle_result_stats__1
    on brawler_trio_battle_result_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, pair_brawler1_brawlstars_id, pair_brawler2_brawlstars_id
    );

create table if not exists brawler_trio_battle_rank_stats
(
    brawler_trio_battle_rank_stats_id bigint generated always as identity primary key,
    event_brawlstars_id               bigint      not null,
    tier_range                        varchar(45) not null,
    start_battle_date                 date        not null,
    end_battle_date                   date        not null,
    brawler_brawlstars_id             bigint      not null,
    pair_brawler1_brawlstars_id       bigint      not null,
    pair_brawler2_brawlstars_id       bigint      not null,
    rank_counts                       jsonb       not null,
    total_count                       bigint      not null,
    weighted_rank_sum                 bigint      not null,
    created_at                        timestamptz not null default now(),
    updated_at                        timestamptz not null default now(),
    constraint ck_brawler_trio_battle_rank_stats_distinct_brawler
        check (
            brawler_brawlstars_id <> pair_brawler1_brawlstars_id
            and brawler_brawlstars_id <> pair_brawler2_brawlstars_id
            and pair_brawler1_brawlstars_id < pair_brawler2_brawlstars_id
        )
);
create unique index if not exists uk_brawler_trio_battle_rank_stats__1
    on brawler_trio_battle_rank_stats (
        event_brawlstars_id, start_battle_date, end_battle_date, tier_range,
        brawler_brawlstars_id, pair_brawler1_brawlstars_id, pair_brawler2_brawlstars_id
    );
