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
    club_brawlstars_tag                   varchar(45)  null,
    club_name                             varchar(105) null,
    created_at                            timestamp(6)  not null default current_timestamp(6),
    updated_at                            timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    deleted                               boolean      not null default false,
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
    deleted               boolean     not null default false,
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
    deleted                boolean     not null default false,
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
    deleted                      boolean     not null default false,
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
    deleted                  boolean     not null default false,
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
    deleted              boolean      not null default false,
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
    deleted             boolean     not null default false,
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
    deleted        boolean     not null default false,
    index ix_renewal__brawlstarstag_type_requestedat (brawlstars_tag, type, requested_at desc)
);

-- battle table
create table if not exists battle
(
    battle_id                  bigint auto_increment primary key,
    battle_key                 varchar(255)  not null,
    battle_at                timestamp(6)  not null,
    event_brawlstars_id        bigint        null,
    event_mode                 varchar(65)   null,
    event_map                  varchar(65)   null,
    mode                       varchar(65)  null,
    type                       varchar(65)  null,
    result                     varchar(25)   null,
    duration                   int           null,
    star_player_brawlstars_tag varchar(45)   null,
    player_id                  bigint        not null,
    battle_rank                int           null,
    trophy_change              int           null,
    teams                      json          not null,
    created_at                 timestamp(6)  not null default current_timestamp(6),
    updated_at                 timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    deleted                    boolean       not null default false,
    index ix_battle__playerid_battleat (player_id, battle_at desc)
);

-- player_ranking table
create table if not exists player_ranking
(
    player_ranking_id bigint auto_increment primary key,
    country           varchar(25)  not null,
    collected_at      timestamp(6) not null,
    created_at        timestamp(6) not null default current_timestamp(6),
    updated_at        timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted           boolean      not null default false,
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
    deleted                   boolean      not null default false,
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
    deleted               boolean      not null default false,
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
    deleted                   boolean      not null default false,
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
    deleted         boolean      not null default false,
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
    deleted              boolean      not null default false,
    index ix_club_ranking_item__club_ranking_id (club_ranking_id)
);

-- stats_battle table (파티셔닝: created_date 기준)
create table if not exists stats_battle
(
    stats_battle_id            bigint auto_increment,
    created_date               date          not null,
    battle_key                 varchar(255)  not null,
    battle_at                  timestamp(6)  not null,
    event_brawlstars_id        bigint        not null,
    event_mode                 varchar(65)   not null,
    event_map                  varchar(65)   not null,
    mode                       varchar(65)  null,
    type                       varchar(65)  null,
    result                     varchar(25)   null,
    duration                   int           null,
    star_player_brawlstars_tag varchar(45)   null,
    player_brawlstars_tag      varchar(45)   not null,
    battle_rank                int           null,
    trophy_change              int           null,
    teams                      json          not null,
    created_at                 timestamp(6)  not null default current_timestamp(6),
    updated_at                 timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    deleted                    boolean       not null default false,
    primary key (stats_battle_id, created_date)
) partition by range (to_days(created_date)) (
    partition p_default values less than maxvalue
);
-- 파티션 관리 스크립트 (운영 시 스케줄러 또는 배치에서 실행 필요)
-- 일별 파티션 추가 예시:
--   ALTER TABLE stats_battle ADD PARTITION (
--       PARTITION p_20250124 VALUES LESS THAN (to_days('2025-01-25'))
--   );
-- 오래된 파티션 삭제 예시:
--   ALTER TABLE stats_battle DROP PARTITION p_20250101;

-- aggregated_battle_key table (파티셔닝: battle_date 기준)
create table if not exists aggregated_battle_key
(
    aggregated_battle_key_id bigint auto_increment,
    battle_date              date          not null,
    battle_key               varchar(255)  not null,
    created_at               timestamp(6)  not null default current_timestamp(6),
    updated_at               timestamp(6)  not null default current_timestamp(6) on update current_timestamp(6),
    deleted                  boolean       not null default false,
    primary key (aggregated_battle_key_id, battle_date),
    unique index uk_aggregated_battle_key__battle_date_battle_key (battle_date, battle_key)
) partition by range (to_days(battle_date)) (
    partition p_default values less than maxvalue
);
-- 파티션 관리 스크립트 (운영 시 스케줄러 또는 배치에서 실행 필요)
-- 일별 파티션 추가 예시:
--   ALTER TABLE aggregated_battle_key ADD PARTITION (
--       PARTITION p_20250124 VALUES LESS THAN (to_days('2025-01-25'))
--   );
-- 오래된 파티션 삭제 예시:
--   ALTER TABLE aggregated_battle_key DROP PARTITION p_20250101;
