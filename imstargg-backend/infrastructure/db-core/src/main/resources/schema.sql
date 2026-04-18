-- player table
create table if not exists player
(
    player_id                             bigint generated always as identity primary key,
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
    total_prestige_level                  int          not null,
    status                                varchar(25)  not null default 'ACTIVE',
    club_brawlstars_tag                   varchar(45)  null,
    club_name                             varchar(105) null,
    created_at                            timestamptz  not null default now(),
    updated_at                            timestamptz  not null default now()
);
create unique index if not exists uk_player__brawlstarstag on player (brawlstars_tag);

-- player brawler table
create table if not exists player_brawler
(
    player_brawler_id     bigint generated always as identity primary key,
    player_id             bigint      not null,
    brawler_brawlstars_id bigint      not null,
    power                 int         not null,
    trophy_rank           int         not null,
    trophies              int         not null,
    highest_trophies      int         not null,
    prestige_level        int         not null,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);
create unique index if not exists uk_playerbrawler__playerid_brawlerbrawlstarsid on player_brawler (player_id, brawler_brawlstars_id);

-- player brawler gear table
create table if not exists player_brawler_gear
(
    player_brawler_gear_id bigint generated always as identity primary key,
    player_brawler_id      bigint      not null,
    gear_brawlstars_id     bigint      not null,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);
create unique index if not exists uk_playerbrawlergear__playerbrawlerid_gearbrawlstarsid on player_brawler_gear (player_brawler_id, gear_brawlstars_id);

-- player brawler star power table
create table if not exists player_brawler_star_power
(
    player_brawler_star_power_id bigint generated always as identity primary key,
    player_brawler_id            bigint      not null,
    star_power_brawlstars_id     bigint      not null,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_star_power__1 on player_brawler_star_power (player_brawler_id, star_power_brawlstars_id);

-- player brawler hyper charge table
create table if not exists player_brawler_hyper_charge
(
    player_brawler_hyper_charge_id bigint generated always as identity primary key,
    player_brawler_id              bigint      not null,
    hyper_charge_brawlstars_id     bigint      not null,
    created_at                     timestamptz not null default now(),
    updated_at                     timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_hyper_charge__1 on player_brawler_hyper_charge (player_brawler_id, hyper_charge_brawlstars_id);

-- player brawler buffy table
create table if not exists player_brawler_buffy
(
    player_brawler_buffy_id bigint generated always as identity primary key,
    player_brawler_id       bigint      not null,
    buffy_type              varchar(25) not null,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);
create unique index if not exists uk_playerbrawlerbuffy__playerbrawlerid_buffytype on player_brawler_buffy (player_brawler_id, buffy_type);

-- player brawler gadget table
create table if not exists player_brawler_gadget
(
    player_brawler_gadget_id bigint generated always as identity primary key,
    player_brawler_id        bigint      not null,
    gadget_brawlstars_id     bigint      not null,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);
create unique index if not exists uk_playerbrawlergadget__playerbrawlerid_gadgetbrawlstarsid on player_brawler_gadget (player_brawler_id, gadget_brawlstars_id);

-- club table
create table if not exists club
(
    club_id              bigint generated always as identity primary key,
    brawlstars_tag       varchar(45)  not null,
    badge_brawlstars_id  bigint       not null,
    name                 varchar(105) not null,
    description          varchar(500) not null,
    type                 varchar(45)  not null,
    required_trophies    int          not null,
    trophies             int          not null,
    status               varchar(25)  not null default 'ACTIVE',
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now()
);
create unique index if not exists uk_club__brawlstarstag on club (brawlstars_tag);

-- club member table
create table if not exists club_member
(
    club_member_id      bigint generated always as identity primary key,
    club_id             bigint      not null,
    brawlstars_tag      varchar(45) not null,
    name                varchar(45) not null,
    name_color          varchar(45) null,
    role                varchar(25) not null,
    trophies            int         not null,
    icon_brawlstars_id  bigint      not null,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);
create unique index if not exists uk_clubmember__clubid_brawlstarstag on club_member (club_id, brawlstars_tag);

-- renewal table
create table if not exists renewal
(
    renewal_id     bigint generated always as identity primary key,
    brawlstars_tag varchar(45) not null,
    type           varchar(25) not null,
    status         varchar(25) not null,
    requested_at   timestamptz not null,
    executed_at    timestamptz null,
    completed_at   timestamptz null,
    version        bigint      not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);
create unique index if not exists uk_renewal__brawlstarstag_type on renewal (brawlstars_tag, type);

-- battle table
create table if not exists battle
(
    battle_id                  bigint generated always as identity primary key,
    battle_key                 varchar(255)  not null,
    battle_at                  timestamptz   not null,
    event_brawlstars_id        bigint        null,
    event_mode                 varchar(65)   null,
    event_mode_id              int           null,
    event_map                  varchar(65)   null,
    mode                       varchar(65)   null,
    type                       varchar(65)   null,
    result                     varchar(25)   null,
    duration                   int           null,
    star_player_brawlstars_tag varchar(45)   null,
    player_id                  bigint        not null,
    battle_rank                int           null,
    trophy_change              int           null,
    created_at                 timestamptz   not null default now(),
    updated_at                 timestamptz   not null default now()
);
create index if not exists ix_battle__playerid_battleat on battle (player_id, battle_at desc);

-- battle_team_player table
create table if not exists battle_team_player
(
    battle_team_player_id   bigint generated always as identity primary key,
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
    created_at              timestamptz   not null default now(),
    updated_at              timestamptz   not null default now()
);
create index if not exists ix_battleteamplayer__battleid on battle_team_player (battle_id);

-- player_ranking table
create table if not exists player_ranking
(
    player_ranking_id bigint generated always as identity primary key,
    country           varchar(25)  not null,
    collected_at      timestamptz  not null,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);
create index if not exists ix_player_ranking__country_collected_at on player_ranking (country, collected_at);

-- player_ranking_item table
create table if not exists player_ranking_item
(
    player_ranking_item_id    bigint generated always as identity primary key,
    player_ranking_id         bigint       not null,
    rank_value                int          not null,
    player_brawlstars_tag     varchar(45)  not null,
    player_name               varchar(105) not null,
    player_name_color         varchar(45)  null,
    player_icon_brawlstars_id bigint       not null,
    player_club_name          varchar(105) not null,
    trophies                  int          not null,
    created_at                timestamptz  not null default now(),
    updated_at                timestamptz  not null default now()
);
create unique index if not exists uk_player_ranking_item__1 on player_ranking_item (player_ranking_id, player_brawlstars_tag);

-- brawler_ranking table
create table if not exists brawler_ranking
(
    brawler_ranking_id    bigint generated always as identity primary key,
    country               varchar(25)  not null,
    brawler_brawlstars_id bigint       not null,
    collected_at          timestamptz  not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);
create index if not exists ix_brawler_ranking__country_brawler_collected_at on brawler_ranking (country, brawler_brawlstars_id, collected_at);

-- brawler_ranking_item table
create table if not exists brawler_ranking_item
(
    brawler_ranking_item_id   bigint generated always as identity primary key,
    brawler_ranking_id        bigint       not null,
    rank_value                int          not null,
    player_brawlstars_tag     varchar(45)  not null,
    player_name               varchar(105) not null,
    player_name_color         varchar(45)  null,
    player_icon_brawlstars_id bigint       not null,
    player_club_name          varchar(105) not null,
    trophies                  int          not null,
    created_at                timestamptz  not null default now(),
    updated_at                timestamptz  not null default now()
);
create unique index if not exists uk_brawler_ranking_item__1 on brawler_ranking_item (brawler_ranking_id, player_brawlstars_tag);

-- club_ranking table
create table if not exists club_ranking
(
    club_ranking_id bigint generated always as identity primary key,
    country         varchar(25)  not null,
    collected_at    timestamptz  not null,
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now()
);
create index if not exists ix_club_ranking__country_collected_at on club_ranking (country, collected_at);

-- club_ranking_item table
create table if not exists club_ranking_item
(
    club_ranking_item_id bigint generated always as identity primary key,
    club_ranking_id      bigint       not null,
    rank_value           int          not null,
    brawlstars_tag       varchar(45)  not null,
    name                 varchar(105) not null,
    badge_brawlstars_id  bigint       not null,
    trophies             int          not null,
    member_count         int          not null,
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now()
);
create unique index if not exists uk_club_ranking_item__club_ranking_id_brawlstars_tag on club_ranking_item (club_ranking_id, brawlstars_tag);

-- player_daily_ranking table
create table if not exists player_daily_ranking
(
    player_daily_ranking_id bigint generated always as identity primary key,
    country                 varchar(25)  not null,
    date                    date         not null,
    start_time              timestamptz  not null,
    end_time                timestamptz  not null,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now()
);
create unique index if not exists uk_player_daily_ranking__country_date on player_daily_ranking (country, date);

-- player_daily_ranking_item table
create table if not exists player_daily_ranking_item
(
    player_daily_ranking_item_id bigint generated always as identity primary key,
    player_daily_ranking_id      bigint       not null,
    rank_value                   int          not null,
    player_brawlstars_tag        varchar(45)  not null,
    player_name                  varchar(105) not null,
    player_name_color            varchar(45)  null,
    player_icon_brawlstars_id    bigint       not null,
    player_club_name             varchar(105) not null,
    trophies                     int          not null,
    created_at                   timestamptz  not null default now(),
    updated_at                   timestamptz  not null default now()
);
create unique index if not exists uk_player_daily_ranking_item__ranking_id_player_tag on player_daily_ranking_item (player_daily_ranking_id, player_brawlstars_tag);

-- brawler_daily_ranking table
create table if not exists brawler_daily_ranking
(
    brawler_daily_ranking_id bigint generated always as identity primary key,
    country                  varchar(25)  not null,
    brawler_brawlstars_id    bigint       not null,
    date                     date         not null,
    start_time               timestamptz  not null,
    end_time                 timestamptz  not null,
    created_at               timestamptz  not null default now(),
    updated_at               timestamptz  not null default now()
);
create unique index if not exists uk_brawler_daily_ranking__country_brawler_date on brawler_daily_ranking (country, brawler_brawlstars_id, date);

-- brawler_daily_ranking_item table
create table if not exists brawler_daily_ranking_item
(
    brawler_daily_ranking_item_id bigint generated always as identity primary key,
    brawler_daily_ranking_id      bigint       not null,
    rank_value                    int          not null,
    player_brawlstars_tag         varchar(45)  not null,
    player_name                   varchar(105) not null,
    player_name_color             varchar(45)  null,
    player_icon_brawlstars_id     bigint       not null,
    player_club_name              varchar(105) not null,
    trophies                      int          not null,
    created_at                    timestamptz  not null default now(),
    updated_at                    timestamptz  not null default now()
);
create unique index if not exists uk_brawler_daily_ranking_item__ranking_id_player_tag on brawler_daily_ranking_item (brawler_daily_ranking_id, player_brawlstars_tag);

-- club_daily_ranking table
create table if not exists club_daily_ranking
(
    club_daily_ranking_id bigint generated always as identity primary key,
    country               varchar(25)  not null,
    date                  date         not null,
    start_time            timestamptz  not null,
    end_time              timestamptz  not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);
create unique index if not exists uk_club_daily_ranking__country_date on club_daily_ranking (country, date);

-- club_daily_ranking_item table
create table if not exists club_daily_ranking_item
(
    club_daily_ranking_item_id bigint generated always as identity primary key,
    club_daily_ranking_id      bigint       not null,
    rank_value                 int          not null,
    brawlstars_tag             varchar(45)  not null,
    name                       varchar(105) not null,
    badge_brawlstars_id        bigint       not null,
    trophies                   int          not null,
    member_count               int          not null,
    created_at                 timestamptz  not null default now(),
    updated_at                 timestamptz  not null default now()
);
create unique index if not exists uk_club_daily_ranking_item__ranking_id_tag on club_daily_ranking_item (club_daily_ranking_id, brawlstars_tag);

-- stats_rank_battle table (파티셔닝: battle_date 기준, 보관 기간 7일)
-- 역할: 배틀 랭크 통계 데이터 (플레이어별)
create table if not exists stats_rank_battle
(
    stats_rank_battle_id       bigint generated always as identity,
    battle_date                date          not null,
    battle_key                 varchar(255)  not null,
    battle_at                  timestamptz   not null,
    event_brawlstars_id        bigint        not null,
    event_mode_brawlstars_id   bigint        not null,
    event_map                  varchar(65)   not null,
    mode                       varchar(65)   not null,
    type                       varchar(65)   not null,
    star_player_brawlstars_tag varchar(45)   null,
    player_brawlstars_tag      varchar(45)   not null,
    battle_rank                int           not null,
    teams                      jsonb         not null,
    created_at                 timestamptz   not null default now(),
    updated_at                 timestamptz   not null default now(),
    primary key (stats_rank_battle_id, battle_date)
) partition by range (battle_date);
create unique index if not exists uk_stats_rank_battle__battle_date_battle_key_player
    on stats_rank_battle (battle_date, battle_key, player_brawlstars_tag);
create index if not exists ix_stats_rank_battle__battle_date_event
    on stats_rank_battle (battle_date, event_brawlstars_id);
create table if not exists stats_rank_battle_default partition of stats_rank_battle default;

-- stats_result_battle table (파티셔닝: battle_date 기준, 보관 기간 7일)
-- 역할: 배틀 결과 통계 데이터 (배틀 단위)
create table if not exists stats_result_battle
(
    stats_result_battle_id     bigint generated always as identity,
    battle_date                date          not null,
    battle_key                 varchar(255)  not null,
    battle_at                  timestamptz   not null,
    event_brawlstars_id        bigint        not null,
    event_mode_brawlstars_id   bigint        not null,
    event_map                  varchar(65)   not null,
    mode                       varchar(65)   not null,
    type                       varchar(65)   not null,
    result                     varchar(25)   not null,
    star_player_brawlstars_tag varchar(45)   null,
    player_brawlstars_tag      varchar(45)   not null,
    teams                      jsonb         not null,
    created_at                 timestamptz   not null default now(),
    updated_at                 timestamptz   not null default now(),
    primary key (stats_result_battle_id, battle_date)
) partition by range (battle_date);
create unique index if not exists uk_stats_result_battle__battle_date_battle_key
    on stats_result_battle (battle_date, battle_key);
create index if not exists ix_stats_result_battle__battle_date_event
    on stats_result_battle (battle_date, event_brawlstars_id);
create table if not exists stats_result_battle_default partition of stats_result_battle default;
-- 파티션 관리: partition-manage.sql 참조

create table if not exists stats_player
(
    stats_player_id bigint generated always as identity primary key,
    brawlstars_tag  varchar(45)  not null,
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now()
);
create unique index if not exists uk_stats_player__brawlstars_tag on stats_player (brawlstars_tag);

-- battle_event_mode table
create table if not exists battle_event_mode
(
    battle_event_mode_id bigint generated always as identity primary key,
    brawlstars_id        bigint       not null,
    brawlstars_name      varchar(255) not null,
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now()
);
create unique index if not exists uk_battle_event_mode__brawlstarsid on battle_event_mode (brawlstars_id);

-- battle_event table
create table if not exists battle_event
(
    battle_event_id      bigint generated always as identity primary key,
    brawlstars_id        bigint       not null,
    battle_event_mode_id bigint       null,
    map_name             varchar(105) null,
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now()
);
create unique index if not exists uk_battle_event__brawlstarsid on battle_event (brawlstars_id);

-- 일별 브롤러별 전투 결과 통계
create table if not exists brawler_battle_result_stats
(
    brawler_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id           bigint       not null,
    tier_range                    varchar(45)  not null,
    battle_date                   date         not null,
    brawler_brawlstars_id         bigint       not null,
    victory_count                 bigint       not null,
    defeat_count                  bigint       not null,
    draw_count                    bigint       not null,
    total_count                   bigint       not null,
    star_player_count             bigint       not null,
    created_at                    timestamptz  not null default now(),
    updated_at                    timestamptz  not null default now()
);
create index if not exists ix_brawler_battle_result_stats_1 on brawler_battle_result_stats (event_brawlstars_id, tier_range, battle_date);
create index if not exists ix_brawler_battle_result_stats_2 on brawler_battle_result_stats (brawler_brawlstars_id, tier_range, battle_date);

-- 일별 브롤러 전투 랭크 통계
create table if not exists brawler_battle_rank_stats
(
    brawler_battle_rank_stats_id bigint generated always as identity primary key,
    event_brawlstars_id          bigint       not null,
    tier_range                   varchar(45)  not null,
    battle_date                  date         not null,
    brawler_brawlstars_id        bigint       not null,
    rank_counts                  jsonb        not null,
    total_count                  bigint       not null,
    created_at                   timestamptz  not null default now(),
    updated_at                   timestamptz  not null default now()
);
create index if not exists ix_brawler_battle_rank_stats on brawler_battle_rank_stats (event_brawlstars_id, tier_range, battle_date);

-- 브롤러 vs 적 브롤러 전투 결과 통계
create table if not exists brawler_enemy_battle_result_stats
(
    brawler_enemy_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id                  bigint       not null,
    period_type                          varchar(25)  not null,
    tier_range                           varchar(45)  not null,
    battle_date                          date         not null,
    brawler_brawlstars_id                bigint       not null,
    enemy_brawler_brawlstars_id          bigint       not null,
    victory_count                        bigint       not null,
    defeat_count                         bigint       not null,
    draw_count                           bigint       not null,
    total_count                          bigint       not null,
    created_at                           timestamptz  not null default now(),
    updated_at                           timestamptz  not null default now()
);
create index if not exists ix_brawler_enemy_battle_result_stats_1 on brawler_enemy_battle_result_stats (event_brawlstars_id, tier_range, period_type, battle_date);
create index if not exists ix_brawler_enemy_battle_result_stats_2 on brawler_enemy_battle_result_stats (brawler_brawlstars_id, tier_range, period_type, battle_date);

-- 브롤러 팀 전투 결과 통계
create table if not exists brawler_team_battle_result_stats
(
    brawler_team_battle_result_stats_id bigint generated always as identity primary key,
    event_brawlstars_id                 bigint       not null,
    period_type                         varchar(25)  not null,
    tier_range                          varchar(45)  not null,
    battle_date                         date         not null,
    team_size                           int          not null,
    team_key                            varchar(100) not null,
    victory_count                       bigint       not null,
    defeat_count                        bigint       not null,
    draw_count                          bigint       not null,
    total_count                         bigint       not null,
    created_at                          timestamptz  not null default now(),
    updated_at                          timestamptz  not null default now()
);
create index if not exists ix_brawler_team_battle_result_stats_1 on brawler_team_battle_result_stats (event_brawlstars_id, tier_range, period_type, battle_date);

create table if not exists brawler_team_battle_result_stats_brawler
(
    brawler_team_battle_result_stats_brawler_id bigint generated always as identity primary key,
    brawler_team_battle_result_stats_id          bigint       not null,
    brawler_brawlstars_id                        bigint       not null,
    created_at                                   timestamptz  not null default now(),
    updated_at                                   timestamptz  not null default now()
);
create index if not exists ix_brawler_team_battle_result_stats_brawler_1 on brawler_team_battle_result_stats_brawler (brawler_brawlstars_id, brawler_team_battle_result_stats_id);

-- 브롤러 팀 전투 랭크 통계
create table if not exists brawler_team_battle_rank_stats
(
    brawler_team_battle_rank_stats_id bigint generated always as identity primary key,
    event_brawlstars_id               bigint       not null,
    period_type                       varchar(25)  not null,
    tier_range                        varchar(45)  not null,
    battle_date                       date         not null,
    team_size                         int          not null,
    team_key                          varchar(100) not null,
    rank_counts                       jsonb        not null,
    total_count                       bigint       not null,
    weighted_rank_sum                 bigint       not null,
    created_at                        timestamptz  not null default now(),
    updated_at                        timestamptz  not null default now()
);
create index if not exists ix_brawler_team_battle_rank_stats_1 on brawler_team_battle_rank_stats (event_brawlstars_id, tier_range, period_type, battle_date);

create table if not exists brawler_team_battle_rank_stats_brawler
(
    brawler_team_battle_rank_stats_brawler_id bigint generated always as identity primary key,
    brawler_team_battle_rank_stats_id          bigint       not null,
    brawler_brawlstars_id                      bigint       not null,
    created_at                                 timestamptz  not null default now(),
    updated_at                                 timestamptz  not null default now()
);
create index if not exists ix_brawler_team_battle_rank_stats_brawler_1 on brawler_team_battle_rank_stats_brawler (brawler_brawlstars_id, brawler_team_battle_rank_stats_id);

create table if not exists battle_event_rotation
(
    battle_event_rotation_id bigint generated always as identity primary key,
    collected_at             timestamptz not null,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);
create index if not exists ix_battle_event_rotation__collected_at on battle_event_rotation (collected_at);

create table if not exists battle_event_rotation_item
(
    battle_event_rotation_item_id bigint generated always as identity primary key,
    battle_event_rotation_id     bigint       not null,
    event_brawlstars_id          bigint       not null,
    modifiers                    jsonb        not null,
    slot_id                      int          not null,
    start_time                   timestamptz  not null,
    end_time                     timestamptz  not null,
    event_mode_name              varchar(255) null,
    event_mode_brawlstars_id     bigint       null,
    event_map_name               varchar(255) null,
    created_at                   timestamptz  not null default now(),
    updated_at                   timestamptz  not null default now()
);
create index if not exists ix_battle_event_rotation_item__battle_event_rotation_id on battle_event_rotation_item (battle_event_rotation_id);

create table if not exists battle_event_solo_rank_event
(
    battle_event_solo_rank_event_id bigint generated always as identity primary key,
    solo_rank_season_key            varchar(45)  not null,
    event_brawlstars_id             bigint       not null,
    created_at                      timestamptz  not null default now(),
    updated_at                      timestamptz  not null default now()
);
create unique index if not exists uk_battle_event_solo_rank_event__1 on battle_event_solo_rank_event (solo_rank_season_key, event_brawlstars_id);

-- brawler table
create table if not exists brawler
(
    brawler_id        bigint generated always as identity primary key,
    brawlstars_id     bigint       not null,
    rarity            varchar(45)  not null,
    role              varchar(45)  not null,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);
create unique index if not exists uk_brawler__brawlstarsid on brawler (brawlstars_id);

-- brawler_gear table
create table if not exists brawler_gear
(
    brawler_gear_id       bigint generated always as identity primary key,
    brawler_brawlstars_id bigint       not null,
    gear_brawlstars_id    bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);
create unique index if not exists uk_brawlergear__brawlerbrawlstarsid_gearbrawlstarsid on brawler_gear (brawler_brawlstars_id, gear_brawlstars_id);

-- gadget table
create table if not exists gadget
(
    gadget_id             bigint generated always as identity primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);
create unique index if not exists uk_gadget__brawlstarsid on gadget (brawlstars_id);

-- gear table
create table if not exists gear
(
    gear_id           bigint generated always as identity primary key,
    brawlstars_id     bigint       not null,
    rarity            varchar(45)  not null,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);
create unique index if not exists uk_gear__brawlstarsid on gear (brawlstars_id);

-- hyper_charge table
create table if not exists hyper_charge
(
    hyper_charge_id       bigint generated always as identity primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);
create unique index if not exists uk_hypercharge__brawlstarsid on hyper_charge (brawlstars_id);

-- star_power table
create table if not exists star_power
(
    star_power_id         bigint generated always as identity primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now()
);
create unique index if not exists uk_starpower__brawlstarsid on star_power (brawlstars_id);

-- brawlstars_image table
create table if not exists brawlstars_image
(
    brawlstars_image_id bigint generated always as identity primary key,
    type                varchar(45)  not null,
    brawlstars_id       bigint       not null,
    stored_name         varchar(255) not null,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now()
);
create unique index if not exists uk_brawlstars_image__type_brawlstarsid on brawlstars_image (type, brawlstars_id);

-- message table
create table if not exists message
(
    message_id    bigint generated always as identity primary key,
    type          varchar(45) not null,
    brawlstars_id bigint      not null,
    lang          varchar(25) not null,
    content       text        not null,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);
create unique index if not exists uk_message__type_brawlstarsid_lang on message (type, brawlstars_id, lang);

-- brawlstars_api_maintenance table
create table if not exists brawlstars_api_maintenance
(
    brawlstars_api_maintenance_id bigint generated always as identity primary key,
    start_time                    timestamptz not null,
    end_time                      timestamptz not null,
    created_at                    timestamptz not null default now(),
    updated_at                    timestamptz not null default now()
);
create index if not exists ix_brawlstars_api_maintenance__end_time on brawlstars_api_maintenance (end_time);

-- brawler_gear_ownership_stats table
create table if not exists brawler_gear_ownership_stats
(
    brawler_gear_ownership_stats_id bigint generated always as identity primary key,
    brawler_brawlstars_id           bigint      not null,
    trophy_range                    varchar(45) not null,
    gear_brawlstars_id              bigint      not null,
    owner_count                     bigint      not null,
    total_count                     bigint      not null,
    created_at                      timestamptz not null default now(),
    updated_at                      timestamptz not null default now()
);
create unique index if not exists uk_brawler_gear_ownership_stats_1 on brawler_gear_ownership_stats (brawler_brawlstars_id, trophy_range, gear_brawlstars_id);

-- brawler_gadget_ownership_stats table
create table if not exists brawler_gadget_ownership_stats
(
    brawler_gadget_ownership_stats_id bigint generated always as identity primary key,
    brawler_brawlstars_id             bigint      not null,
    trophy_range                      varchar(45) not null,
    gadget_brawlstars_id              bigint      not null,
    owner_count                       bigint      not null,
    total_count                       bigint      not null,
    created_at                        timestamptz not null default now(),
    updated_at                        timestamptz not null default now()
);
create unique index if not exists uk_brawler_gadget_ownership_stats_1 on brawler_gadget_ownership_stats (brawler_brawlstars_id, trophy_range, gadget_brawlstars_id);

-- brawler_star_power_ownership_stats table
create table if not exists brawler_star_power_ownership_stats
(
    brawler_star_power_ownership_stats_id bigint generated always as identity primary key,
    brawler_brawlstars_id                 bigint      not null,
    trophy_range                          varchar(45) not null,
    star_power_brawlstars_id              bigint      not null,
    owner_count                           bigint      not null,
    total_count                           bigint      not null,
    created_at                            timestamptz not null default now(),
    updated_at                            timestamptz not null default now()
);
create unique index if not exists uk_brawler_star_power_ownership_stats_1 on brawler_star_power_ownership_stats (brawler_brawlstars_id, trophy_range, star_power_brawlstars_id);

-- brawler_hyper_charge_ownership_stats table
create table if not exists brawler_hyper_charge_ownership_stats
(
    brawler_hyper_charge_ownership_stats_id bigint generated always as identity primary key,
    brawler_brawlstars_id                   bigint      not null,
    trophy_range                            varchar(45) not null,
    hyper_charge_brawlstars_id              bigint      not null,
    owner_count                             bigint      not null,
    total_count                             bigint      not null,
    created_at                              timestamptz not null default now(),
    updated_at                              timestamptz not null default now()
);
create unique index if not exists uk_brawler_hyper_charge_ownership_stats_1 on brawler_hyper_charge_ownership_stats (brawler_brawlstars_id, trophy_range, hyper_charge_brawlstars_id);

-- brawler_buffy_ownership_stats table
create table if not exists brawler_buffy_ownership_stats
(
    brawler_buffy_ownership_stats_id bigint generated always as identity primary key,
    brawler_brawlstars_id            bigint      not null,
    trophy_range                     varchar(45) not null,
    buffy_type                       varchar(25) not null,
    owner_count                      bigint      not null,
    total_count                      bigint      not null,
    created_at                       timestamptz not null default now(),
    updated_at                       timestamptz not null default now()
);
create unique index if not exists uk_brawler_buffy_ownership_stats_1 on brawler_buffy_ownership_stats (brawler_brawlstars_id, trophy_range, buffy_type);
