-- player table
create table if not exists player
(
    player_id                             bigint auto_increment primary key,
    brawlstars_tag                        varchar(45)  not null,
    name                                  varchar(105) not null,
    name_color                            varchar(45)  null,
    icon_brawlstars_id                    bigint       not null,
    trophies                              int          not null,
    highest_trophies                      int          not null,
    exp_level                             int          not null,
    exp_points                            int          not null,
    qualified_from_championship_challenge boolean      not null,
    victories_3vs3                        int          not null,
    victories_solo                        int          not null,
    victories_duo                         int          not null,
    best_robo_rumble_time                 int          not null,
    best_time_as_big_brawler              int          not null,
    status                                varchar(25)  not null default 'ACTIVE',
    club_brawlstars_tag                   varchar(45)  null,
    club_name                             varchar(105) null,
    created_at                            timestamp(6)  not null default current_timestamp(6),
    updated_at                            timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_player__brawlstarstag (brawlstars_tag)
);

-- player brawler table
create table if not exists player_brawler
(
    player_brawler_id     bigint auto_increment primary key,
    player_id             bigint      not null,
    brawler_brawlstars_id bigint      not null,
    power                 int         not null,
    trophy_rank           int         not null,
    trophies              int         not null,
    highest_trophies      int         not null,
    created_at            timestamp(6) not null default current_timestamp(6),
    updated_at            timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_playerbrawler__playerid_brawlerbrawlstarsid (player_id, brawler_brawlstars_id)
);

-- player brawler gear table
create table if not exists player_brawler_gear
(
    player_brawler_gear_id bigint auto_increment primary key,
    player_brawler_id      bigint      not null,
    gear_brawlstars_id     bigint      not null,
    created_at             timestamp(6) not null default current_timestamp(6),
    updated_at             timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_playerbrawlergear__playerbrawlerid_gearbrawlstarsid (player_brawler_id, gear_brawlstars_id)
);

-- player brawler star power table
create table if not exists player_brawler_star_power
(
    player_brawler_star_power_id bigint auto_increment primary key,
    player_brawler_id            bigint      not null,
    star_power_brawlstars_id     bigint      not null,
    created_at                   timestamp(6) not null default current_timestamp(6),
    updated_at                   timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_playerbrawlerstarpower__playerbrawlerid_starpowerbrawlstarsid (player_brawler_id, star_power_brawlstars_id)
);

-- player brawler gadget table
create table if not exists player_brawler_gadget
(
    player_brawler_gadget_id bigint auto_increment primary key,
    player_brawler_id        bigint      not null,
    gadget_brawlstars_id     bigint      not null,
    created_at               timestamp(6) not null default current_timestamp(6),
    updated_at               timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_playerbrawlergadget__playerbrawlerid_gadgetbrawlstarsid (player_brawler_id, gadget_brawlstars_id)
);

-- club table
create table if not exists club
(
    club_id              bigint auto_increment primary key,
    brawlstars_tag       varchar(45)  not null,
    badge_brawlstars_id  bigint       not null,
    name                 varchar(105) not null,
    description          varchar(500) not null,
    type                 varchar(45)  not null,
    required_trophies    int          not null,
    trophies             int          not null,
    created_at           timestamp(6)  not null default current_timestamp(6),
    updated_at           timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_club__brawlstarstag (brawlstars_tag)
);

-- club member table
create table if not exists club_member
(
    club_member_id      bigint auto_increment primary key,
    club_id             bigint      not null,
    brawlstars_tag      varchar(45) not null,
    name                varchar(45) not null,
    name_color          varchar(45) null,
    role                varchar(25) not null,
    trophies            int         not null,
    icon_brawlstars_id  bigint      not null,
    created_at          timestamp(6) not null default current_timestamp(6),
    updated_at          timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_clubmember__clubid_brawlstarstag (club_id, brawlstars_tag)
);

-- renewal table
create table if not exists renewal
(
    renewal_id     bigint auto_increment primary key,
    brawlstars_tag varchar(45) not null,
    type           varchar(25) not null,
    status         varchar(25) not null,
    requested_at   timestamp(6) not null,
    executed_at    timestamp(6) null,
    completed_at   timestamp(6) null,
    created_at     timestamp(6) not null default current_timestamp(6),
    updated_at     timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_renewal__brawlstarstag_requestedat (brawlstars_tag, requested_at desc)
);

-- battle table
create table if not exists battle
(
    battle_id                  bigint auto_increment primary key,
    battle_key                 varchar(255)  not null,
    battle_at                timestamp(6)  not null,
    event_brawlstars_id        bigint        null,
    event_mode                 varchar(65)   null,
    event_mode_id              int           null,
    event_map                  varchar(65)   null,
    mode                       varchar(65)  null,
    type                       varchar(65)  null,
    result                     varchar(25)   null,
    duration                   int           null,
    star_player_brawlstars_tag varchar(45)   null,
    player_id                  bigint        not null,
    battle_rank                int           null,
    trophy_change              int           null,
    created_at                 timestamp(6)  not null default current_timestamp(6),
    updated_at                 timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    index ix_battle__playerid_battleat (player_id, battle_at desc)
);

-- battle_team_player table
create table if not exists battle_team_player
(
    battle_team_player_id   bigint auto_increment primary key,
    battle_id               bigint        not null,
    team_idx                int           not null,
    player_idx              int           not null,
    brawlstars_tag          varchar(45)   not null,
    name                    varchar(105)  not null,
    brawler_brawlstars_id   bigint        not null,
    brawler_name            varchar(65)   not null,
    brawler_power           int           not null,
    brawler_trophies        int           not null,
    brawler_trophy_change   int           null,
    created_at              timestamp(6)  not null default current_timestamp(6),
    updated_at              timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    index ix_battleteamplayer__battleid (battle_id)
);

-- player_ranking table
create table if not exists player_ranking
(
    player_ranking_id bigint auto_increment primary key,
    country           varchar(25)  not null,
    collected_at      timestamp(6) not null,
    created_at        timestamp(6) not null default current_timestamp(6),
    updated_at        timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_player_ranking__country_collected_at (country, collected_at)
);

-- player_ranking_item table
create table if not exists player_ranking_item
(
    player_ranking_item_id    bigint auto_increment primary key,
    player_ranking_id         bigint       not null,
    rank_value                int          not null,
    player_brawlstars_tag     varchar(45)  not null,
    player_name               varchar(105) not null,
    player_name_color         varchar(45)  null,
    player_icon_brawlstars_id bigint       not null,
    player_club_name          varchar(105) not null,
    trophies                  int          not null,
    created_at                timestamp(6) not null default current_timestamp(6),
    updated_at                timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_player_ranking_item__player_ranking_id (player_ranking_id)
);

-- brawler_ranking table
create table if not exists brawler_ranking
(
    brawler_ranking_id    bigint auto_increment primary key,
    country               varchar(25)  not null,
    brawler_brawlstars_id bigint       not null,
    collected_at          timestamp(6) not null,
    created_at            timestamp(6) not null default current_timestamp(6),
    updated_at            timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_brawler_ranking__country_brawler_collected_at (country, brawler_brawlstars_id, collected_at)
);

-- brawler_ranking_item table
create table if not exists brawler_ranking_item
(
    brawler_ranking_item_id   bigint auto_increment primary key,
    brawler_ranking_id        bigint       not null,
    rank_value                int          not null,
    player_brawlstars_tag     varchar(45)  not null,
    player_name               varchar(105) not null,
    player_name_color         varchar(45)  null,
    player_icon_brawlstars_id bigint       not null,
    player_club_name          varchar(105) not null,
    trophies                  int          not null,
    created_at                timestamp(6) not null default current_timestamp(6),
    updated_at                timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_ranking_item__brawler_ranking_id (brawler_ranking_id)
);

-- club_ranking table
create table if not exists club_ranking
(
    club_ranking_id bigint auto_increment primary key,
    country         varchar(25)  not null,
    collected_at    timestamp(6) not null,
    created_at      timestamp(6) not null default current_timestamp(6),
    updated_at      timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_club_ranking__country_collected_at (country, collected_at)
);

-- club_ranking_item table
create table if not exists club_ranking_item
(
    club_ranking_item_id bigint auto_increment primary key,
    club_ranking_id      bigint       not null,
    rank_value           int          not null,
    brawlstars_tag       varchar(45)  not null,
    name                 varchar(105) not null,
    badge_brawlstars_id  bigint       not null,
    trophies             int          not null,
    member_count         int          not null,
    created_at           timestamp(6) not null default current_timestamp(6),
    updated_at           timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_club_ranking_item__club_ranking_id (club_ranking_id)
);

-- aggregated_result_battle_key table (파티셔닝: battle_date 기준, 보관 기간 30일)
-- 역할: StatsResultBattleEntity 중복 수집 방지 (battleKey)
create table if not exists aggregated_result_battle_key
(
    aggregated_result_battle_key_id bigint auto_increment,
    battle_date                     date          not null,
    battle_key                      varchar(255)  not null,
    created_at                      timestamp(6)  not null default current_timestamp(6),
    updated_at                      timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    primary key (aggregated_result_battle_key_id, battle_date),
    unique index uk_aggregated_result_battle_key__battle_date_battle_key (battle_date, battle_key)
) partition by range (to_days(battle_date)) (
    partition p_default values less than maxvalue
);
-- 파티션 관리 스크립트 (운영 시 스케줄러 또는 배치에서 실행 필요)
-- 일별 파티션 추가 예시:
--   ALTER TABLE aggregated_result_battle_key ADD PARTITION (
--       PARTITION p_20250124 VALUES LESS THAN (to_days('2025-01-25'))
--   );
-- 오래된 파티션 삭제 예시:
--   ALTER TABLE aggregated_result_battle_key DROP PARTITION p_20250101;

-- aggregated_rank_battle_key table (파티셔닝: battle_date 기준, 보관 기간 30일)
-- 역할: StatsRankBattleEntity 중복 수집 방지 (playerTag + battleKey 조합)
create table if not exists aggregated_rank_battle_key
(
    aggregated_rank_battle_key_id bigint auto_increment,
    battle_date                   date          not null,
    player_brawlstars_tag         varchar(45)   not null,
    battle_key                    varchar(255)  not null,
    created_at                    timestamp(6)  not null default current_timestamp(6),
    updated_at                    timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    primary key (aggregated_rank_battle_key_id, battle_date),
    unique index uk_aggregated_rank_battle_key__date_battlekey_player
        (battle_date, battle_key, player_brawlstars_tag)
) partition by range (to_days(battle_date)) (
    partition p_default values less than maxvalue
);
-- 파티션 관리 스크립트 (운영 시 스케줄러 또는 배치에서 실행 필요)
-- 일별 파티션 추가 예시:
--   ALTER TABLE aggregated_rank_battle_key ADD PARTITION (
--       PARTITION p_20250124 VALUES LESS THAN (to_days('2025-01-25'))
--   );
-- 오래된 파티션 삭제 예시 (30일 초과):
--   ALTER TABLE aggregated_rank_battle_key DROP PARTITION p_20250101;

-- stats_rank_battle table (파티셔닝: created_date 기준, 보관 기간 7일)
-- 역할: 배틀 랭크 통계 데이터 (플레이어별)
create table if not exists stats_rank_battle
(
    stats_rank_battle_id       bigint auto_increment,
    created_date               date          not null,
    battle_key                 varchar(255)  not null,
    battle_at                  timestamp(6)  not null,
    event_brawlstars_id        bigint        not null,
    event_mode_brawlstars_id   bigint        not null,
    event_map                  varchar(65)   not null,
    mode                       varchar(65)   not null,
    type                       varchar(65)   not null,
    star_player_brawlstars_tag varchar(45)   null,
    player_brawlstars_tag      varchar(45)   not null,
    battle_rank                int           not null,
    teams                      json          not null,
    created_at                 timestamp(6)  not null default current_timestamp(6),
    updated_at                 timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    primary key (stats_rank_battle_id, created_date)
) partition by range (to_days(created_date)) (
    partition p_default values less than maxvalue
);
-- 파티션 관리 스크립트 (운영 시 스케줄러 또는 배치에서 실행 필요)
-- 일별 파티션 추가 예시:
--   ALTER TABLE stats_rank_battle ADD PARTITION (
--       PARTITION p_20250124 VALUES LESS THAN (to_days('2025-01-25'))
--   );
-- 오래된 파티션 삭제 예시:
--   ALTER TABLE stats_rank_battle DROP PARTITION p_20250101;

-- stats_result_battle table (파티셔닝: created_date 기준, 보관 기간 7일)
-- 역할: 배틀 결과 통계 데이터 (배틀 단위)
create table if not exists stats_result_battle
(
    stats_result_battle_id     bigint auto_increment,
    created_date               date          not null,
    battle_key                 varchar(255)  not null,
    battle_at                  timestamp(6)  not null,
    event_brawlstars_id        bigint        not null,
    event_mode_brawlstars_id   bigint        not null,
    event_map                  varchar(65)   not null,
    mode                       varchar(65)   not null,
    type                       varchar(65)   not null,
    result                     varchar(25)   not null,
    star_player_brawlstars_tag varchar(45)   null,
    player_brawlstars_tag      varchar(45)   not null,
    teams                      json          not null,
    created_at                 timestamp(6)  not null default current_timestamp(6),
    updated_at                 timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    primary key (stats_result_battle_id, created_date)
) partition by range (to_days(created_date)) (
    partition p_default values less than maxvalue
);
-- 파티션 관리 스크립트 (운영 시 스케줄러 또는 배치에서 실행 필요)
-- 일별 파티션 추가 예시:
--   ALTER TABLE stats_result_battle ADD PARTITION (
--       PARTITION p_20250124 VALUES LESS THAN (to_days('2025-01-25'))
--   );
-- 오래된 파티션 삭제 예시:
--   ALTER TABLE stats_result_battle DROP PARTITION p_20250101;

create table if not exists stats_player
(
    stats_player_id bigint auto_increment primary key,
    brawlstars_tag  varchar(45)  not null,
    created_at      timestamp(6) not null default current_timestamp(6),
    updated_at      timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_stats_player__brawlstars_tag (brawlstars_tag)
);

-- battle_event_mode table
create table if not exists battle_event_mode
(
    battle_event_mode_id bigint auto_increment primary key,
    brawlstars_id        bigint       not null,
    brawlstars_name      varchar(255) not null,
    created_at           timestamp(6) not null default current_timestamp(6),
    updated_at           timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_battle_event_mode__brawlstarsid (brawlstars_id)
);

-- battle_event table
create table if not exists battle_event
(
    battle_event_id      bigint auto_increment primary key,
    brawlstars_id        bigint       not null,
    battle_event_mode_id bigint       null,
    map_name             varchar(105) null,
    created_at           timestamp(6) not null default current_timestamp(6),
    updated_at           timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_battle_event__brawlstarsid (brawlstars_id)
);

-- 일별 브롤러별 전투 결과 통계
create table if not exists brawler_battle_result_stats
(
    brawler_battle_result_stats_id bigint auto_increment primary key,
    event_brawlstars_id           bigint       not null,
    tier_range                    varchar(45)  not null,
    date                          date         not null,
    start_at                timestamp(6) not null,
    end_at                  timestamp(6) not null,
    brawler_brawlstars_id         bigint       not null,
    victory_count                 bigint       not null,
    defeat_count                  bigint       not null,
    draw_count                    bigint       not null,
    star_player_count             bigint       not null,
    created_at                    timestamp(6) not null default current_timestamp(6),
    updated_at                    timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_battle_result_stats_1 (event_brawlstars_id, tier_range, date),
    index ix_brawler_battle_result_stats_2 (tier_range, date, brawler_brawlstars_id)
);

-- 일별 브롤러 솔로(1인) 전투 랭크 통계
create table if not exists brawler_solo_battle_rank_stats
(
    brawler_solo_battle_rank_stats_id bigint auto_increment primary key,
    event_brawlstars_id              bigint       not null,
    tier_range                       varchar(45)  not null,
    date                             date         not null,
    start_at                   timestamp(6) not null,
    end_at                     timestamp(6) not null,
    brawler_brawlstars_id            bigint       not null,
    rank_1_count                     bigint       not null,
    rank_2_count                     bigint       not null,
    rank_3_count                     bigint       not null,
    rank_4_count                     bigint       not null,
    rank_5_count                     bigint       not null,
    rank_6_count                     bigint       not null,
    rank_7_count                     bigint       not null,
    rank_8_count                     bigint       not null,
    rank_9_count                     bigint       not null,
    rank_10_count                    bigint       not null,
    created_at                       timestamp(6) not null default current_timestamp(6),
    updated_at                       timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_solo_battle_rank_stats (event_brawlstars_id, tier_range, date)
);

-- 주간 브롤러 vs 적 브롤러 전투 결과 통계
create table if not exists brawler_enemy_battle_result_weekly_stats
(
    brawler_enemy_battle_result_weekly_stats_id bigint auto_increment primary key,
    event_brawlstars_id                        bigint       not null,
    tier_range                                 varchar(45)  not null,
    date                                       date         not null,
    start_at                             timestamp(6) not null,
    end_at                               timestamp(6) not null,
    brawler_brawlstars_id                      bigint       not null,
    enemy_brawler_brawlstars_id                bigint       not null,
    victory_count                              bigint       not null,
    defeat_count                               bigint       not null,
    draw_count                                 bigint       not null,
    created_at                                 timestamp(6) not null default current_timestamp(6),
    updated_at                                 timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_enemy_battle_result_weekly_stats_1 (event_brawlstars_id, tier_range, date),
    index ix_brawler_enemy_battle_result_weekly_stats_2 (tier_range, date, brawler_brawlstars_id)
);

-- 주간 브롤러 팀(3인) 전투 결과 통계
create table if not exists brawler_team_battle_result_weekly_stats
(
    brawler_team_battle_result_weekly_stats_id bigint auto_increment primary key,
    event_brawlstars_id                       bigint       not null,
    tier_range                                varchar(45)  not null,
    date                                      date         not null,
    start_at                            timestamp(6) not null,
    end_at                              timestamp(6) not null,
    brawler_brawlstars_id                     bigint       not null,
    teammate_brawlstars_id_1                  bigint       not null,
    teammate_brawlstars_id_2                  bigint       not null,
    victory_count                             bigint       not null,
    defeat_count                              bigint       not null,
    draw_count                                bigint       not null,
    created_at                                timestamp(6) not null default current_timestamp(6),
    updated_at                                timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_team_battle_result_weekly_stats_1 (event_brawlstars_id, tier_range, date),
    index ix_brawler_team_battle_result_weekly_stats_2 (tier_range, date, brawler_brawlstars_id)
);

-- 주간 브롤러 듀오(2인) 전투 랭크 통계
create table if not exists brawler_duo_battle_rank_weekly_stats
(
    brawler_duo_battle_rank_weekly_stats_id bigint auto_increment primary key,
    event_brawlstars_id                    bigint       not null,
    tier_range                             varchar(45)  not null,
    date                                   date         not null,
    start_at                         timestamp(6) not null,
    end_at                           timestamp(6) not null,
    brawler_brawlstars_id                  bigint       not null,
    teammate_brawlstars_id                 bigint       not null,
    rank_1_count                           bigint       not null,
    rank_2_count                           bigint       not null,
    rank_3_count                           bigint       not null,
    rank_4_count                           bigint       not null,
    rank_5_count                           bigint       not null,
    created_at                             timestamp(6) not null default current_timestamp(6),
    updated_at                             timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_duo_battle_rank_weekly_stats_1 (event_brawlstars_id, tier_range, date),
    index ix_brawler_duo_battle_rank_weekly_stats_2 (event_brawlstars_id, tier_range, date, brawler_brawlstars_id)
);

-- 주간 브롤러 트리오(3인) 전투 랭크 통계
create table if not exists brawler_trio_battle_rank_weekly_stats
(
    brawler_trio_battle_rank_weekly_stats_id bigint auto_increment primary key,
    event_brawlstars_id                     bigint       not null,
    tier_range                              varchar(45)  not null,
    date                                    date         not null,
    start_at                          timestamp(6) not null,
    end_at                            timestamp(6) not null,
    brawler_brawlstars_id                   bigint       not null,
    teammate_brawlstars_id_1                bigint       not null,
    teammate_brawlstars_id_2                bigint       not null,
    rank_1_count                            bigint       not null,
    rank_2_count                            bigint       not null,
    rank_3_count                            bigint       not null,
    rank_4_count                            bigint       not null,
    created_at                              timestamp(6) not null default current_timestamp(6),
    updated_at                              timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_brawler_trio_battle_rank_weekly_stats_1 (event_brawlstars_id, tier_range, date),
    index ix_brawler_trio_battle_rank_weekly_stats_2 (event_brawlstars_id, tier_range, date, brawler_brawlstars_id)
);

create table if not exists battle_event_rotation_item
(
    battle_event_rotation_item_id bigint auto_increment primary key,
    event_brawlstars_id          bigint       not null,
    modifiers                    json         not null,
    slot_id                      int          not null,
    start_time                   timestamp(6) not null,
    end_time                     timestamp(6) not null,
    event_mode_name              varchar(255) null,
    event_mode_brawlstars_id     bigint       null,
    event_map_name               varchar(255) null,
    created_at                   timestamp(6) not null default current_timestamp(6),
    updated_at                   timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    index ix_battle_event_rotation_item__end_time (end_time)
);

create table if not exists battle_event_solo_rank_event
(
    battle_event_solo_rank_event_id bigint auto_increment primary key,
    solo_rank_season_key            varchar(45)  not null,
    event_brawlstars_id             bigint       not null,
    created_at                      timestamp(6) not null default current_timestamp(6),
    updated_at                      timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_battle_event_solo_rank_event__1 (solo_rank_season_key, event_brawlstars_id)
);

-- brawler table
create table if not exists brawler
(
    brawler_id        bigint auto_increment primary key,
    brawlstars_id     bigint       not null,
    rarity            varchar(45)  not null,
    role              varchar(45)  not null,
    created_at        timestamp(6) not null default current_timestamp(6),
    updated_at        timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_brawler__brawlstarsid (brawlstars_id)
);

-- brawler_gear table
create table if not exists brawler_gear
(
    brawler_gear_id       bigint auto_increment primary key,
    brawler_brawlstars_id bigint       not null,
    gear_brawlstars_id    bigint       not null,
    created_at            timestamp(6) not null default current_timestamp(6),
    updated_at            timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_brawlergear__brawlerbrawlstarsid_gearbrawlstarsid (brawler_brawlstars_id, gear_brawlstars_id)
);

-- gadget table
create table if not exists gadget
(
    gadget_id             bigint auto_increment primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamp(6) not null default current_timestamp(6),
    updated_at            timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_gadget__brawlstarsid (brawlstars_id)
);

-- gear table
create table if not exists gear
(
    gear_id           bigint auto_increment primary key,
    brawlstars_id     bigint       not null,
    rarity            varchar(45)  not null,
    created_at        timestamp(6) not null default current_timestamp(6),
    updated_at        timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_gear__brawlstarsid (brawlstars_id)
);

-- star_power table
create table if not exists star_power
(
    star_power_id         bigint auto_increment primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamp(6) not null default current_timestamp(6),
    updated_at            timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_starpower__brawlstarsid (brawlstars_id)
);

-- brawlstars_image table
create table if not exists brawlstars_image
(
    brawlstars_image_id bigint auto_increment primary key,
    code                varchar(65)  not null,
    stored_name         varchar(255) not null,
    created_at          timestamp(6) not null default current_timestamp(6),
    updated_at          timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_brawlstars_image__code (code)
);

-- message table
create table if not exists message
(
    message_id bigint auto_increment primary key,
    code       varchar(65)  not null,
    lang       varchar(25)  not null,
    content    text         not null,
    created_at timestamp(6) not null default current_timestamp(6),
    updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    unique index uk_message__code_lang (code, lang)
);
