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
    brawlstars_club_tag                   varchar(45)  null,
    created_at                            datetime(6)  not null,
    updated_at                            datetime(6)  not null,
    deleted                               boolean      not null default false,
    constraint uk_player__brawlstarstag unique (brawlstars_tag)
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
    created_at            datetime(6) not null,
    updated_at            datetime(6) not null,
    deleted               boolean     not null default false,
    constraint uk_playerbrawler__playerid_brawlerbrawlstarsid unique (player_id, brawler_brawlstars_id)
);

-- player brawler gear table
create table if not exists player_brawler_gear
(
    player_brawler_gear_id bigint auto_increment primary key,
    player_brawler_id      bigint      not null,
    gear_brawlstars_id     bigint      not null,
    created_at             datetime(6) not null,
    updated_at             datetime(6) not null,
    deleted                boolean     not null default false,
    constraint uk_playerbrawlergear__playerbrawlerid_gearbrawlstarsid unique (player_brawler_id, gear_brawlstars_id)
);

-- player brawler star power table
create table if not exists player_brawler_star_power
(
    player_brawler_star_power_id bigint auto_increment primary key,
    player_brawler_id            bigint      not null,
    star_power_brawlstars_id     bigint      not null,
    created_at                   datetime(6) not null,
    updated_at                   datetime(6) not null,
    deleted                      boolean     not null default false,
    constraint uk_playerbrawlerstarpower__playerbrawlerid_starpowerbrawlstarsid unique (player_brawler_id, star_power_brawlstars_id)
);

-- player brawler gadget table
create table if not exists player_brawler_gadget
(
    player_brawler_gadget_id bigint auto_increment primary key,
    player_brawler_id        bigint      not null,
    gadget_brawlstars_id     bigint      not null,
    created_at               datetime(6) not null,
    updated_at               datetime(6) not null,
    deleted                  boolean     not null default false,
    constraint uk_playerbrawlergadget__playerbrawlerid_gadgetbrawlstarsid unique (player_brawler_id, gadget_brawlstars_id)
);
