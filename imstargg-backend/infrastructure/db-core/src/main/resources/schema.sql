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
create index if not exists ix_player__name on player (name varchar_pattern_ops);
create index if not exists ix_player__brawlstars_tag on player (brawlstars_tag varchar_pattern_ops);

-- player brawler table
create sequence if not exists player_brawler_seq increment by 100;
create table if not exists player_brawler
(
    player_brawler_id     bigint primary key default nextval('player_brawler_seq'),
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
alter sequence player_brawler_seq owned by player_brawler.player_brawler_id;

-- player brawler gear table
create sequence if not exists player_brawler_gear_seq increment by 100;
create table if not exists player_brawler_gear
(
    player_brawler_gear_id bigint primary key default nextval('player_brawler_gear_seq'),
    player_brawler_id      bigint      not null,
    gear_brawlstars_id     bigint      not null,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);
create unique index if not exists uk_playerbrawlergear__playerbrawlerid_gearbrawlstarsid on player_brawler_gear (player_brawler_id, gear_brawlstars_id);
alter sequence player_brawler_gear_seq owned by player_brawler_gear.player_brawler_gear_id;

-- player brawler star power table
create sequence if not exists player_brawler_star_power_seq increment by 100;
create table if not exists player_brawler_star_power
(
    player_brawler_star_power_id bigint primary key default nextval('player_brawler_star_power_seq'),
    player_brawler_id            bigint      not null,
    star_power_brawlstars_id     bigint      not null,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_star_power__1 on player_brawler_star_power (player_brawler_id, star_power_brawlstars_id);
alter sequence player_brawler_star_power_seq owned by player_brawler_star_power.player_brawler_star_power_id;

-- player brawler hyper charge table
create sequence if not exists player_brawler_hyper_charge_seq increment by 100;
create table if not exists player_brawler_hyper_charge
(
    player_brawler_hyper_charge_id bigint primary key default nextval('player_brawler_hyper_charge_seq'),
    player_brawler_id              bigint      not null,
    hyper_charge_brawlstars_id     bigint      not null,
    created_at                     timestamptz not null default now(),
    updated_at                     timestamptz not null default now()
);
create unique index if not exists uk_player_brawler_hyper_charge__1 on player_brawler_hyper_charge (player_brawler_id, hyper_charge_brawlstars_id);
alter sequence player_brawler_hyper_charge_seq owned by player_brawler_hyper_charge.player_brawler_hyper_charge_id;

-- player brawler buffy table
create sequence if not exists player_brawler_buffy_seq increment by 100;
create table if not exists player_brawler_buffy
(
    player_brawler_buffy_id bigint primary key default nextval('player_brawler_buffy_seq'),
    player_brawler_id       bigint      not null,
    buffy_type              varchar(25) not null,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);
create unique index if not exists uk_playerbrawlerbuffy__playerbrawlerid_buffytype on player_brawler_buffy (player_brawler_id, buffy_type);
alter sequence player_brawler_buffy_seq owned by player_brawler_buffy.player_brawler_buffy_id;

-- player brawler gadget table
create sequence if not exists player_brawler_gadget_seq increment by 100;
create table if not exists player_brawler_gadget
(
    player_brawler_gadget_id bigint primary key default nextval('player_brawler_gadget_seq'),
    player_brawler_id        bigint      not null,
    gadget_brawlstars_id     bigint      not null,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);
create unique index if not exists uk_playerbrawlergadget__playerbrawlerid_gadgetbrawlstarsid on player_brawler_gadget (player_brawler_id, gadget_brawlstars_id);
alter sequence player_brawler_gadget_seq owned by player_brawler_gadget.player_brawler_gadget_id;

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
create index if not exists ix_club__name on club (name varchar_pattern_ops);
create index if not exists ix_club__brawlstars_tag on club (brawlstars_tag varchar_pattern_ops);

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
create sequence if not exists battle_seq increment by 100;
create table if not exists battle
(
    battle_id                  bigint primary key default nextval('battle_seq'),
    battle_key                 varchar(255)  not null,
    battle_at                  timestamptz   not null,
    event_brawlstars_id        bigint        null,
    event_mode                 varchar(65)   null,
    event_mode_id              bigint        null,
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
alter sequence battle_seq owned by battle.battle_id;

-- battle_team_player table
create sequence if not exists battle_team_player_seq increment by 100;
create table if not exists battle_team_player
(
    battle_team_player_id   bigint primary key default nextval('battle_team_player_seq'),
    battle_id               bigint        not null,
    team_idx                int           not null,
    player_idx              int           not null,
    brawlstars_tag          varchar(45)   not null,
    name                    varchar(105)  not null,
    brawler_brawlstars_id   bigint        not null,
    brawler_name            varchar(65)   null,
    brawler_power           int           not null,
    brawler_trophies        int           not null,
    brawler_trophy_change   int           null,
    created_at              timestamptz   not null default now(),
    updated_at              timestamptz   not null default now()
);
create index if not exists ix_battleteamplayer__battleid on battle_team_player (battle_id);
alter sequence battle_team_player_seq owned by battle_team_player.battle_team_player_id;

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

-- stats_rank_battle table (파티셔닝: battle_date 기준, 보관 기간 45일(PartitionManageJobConfig.RETENTION_DAYS, 패치일 등록 기한 30일보다 길게))
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
    player_brawlstars_tag      varchar(45)   not null,
    battle_rank                int           not null,
    teams                      jsonb         not null,
    created_at                 timestamptz   not null default now(),
    updated_at                 timestamptz   not null default now(),
    primary key (stats_rank_battle_id, battle_date)
) partition by range (battle_date);
create unique index if not exists uk_stats_rank_battle__battle_date_battle_key
    on stats_rank_battle (battle_date, battle_key);
create index if not exists ix_stats_rank_battle__battle_date_event
    on stats_rank_battle (battle_date, event_brawlstars_id);
create table if not exists stats_rank_battle_default partition of stats_rank_battle default;

-- stats_result_battle table (파티셔닝: battle_date 기준, 보관 기간 45일(PartitionManageJobConfig.RETENTION_DAYS, 패치일 등록 기한 30일보다 길게))
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
    status          varchar(25)  not null default 'ACTIVE',
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now()
);
create unique index if not exists uk_stats_player__brawlstars_tag on stats_player (brawlstars_tag);

create table if not exists stats_event_mode
(
    stats_event_mode_id      bigint generated always as identity primary key,
    event_mode_brawlstars_id bigint      not null,
    type                     varchar(25) not null,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);
create unique index if not exists uk_stats_event_mode__event_mode_brawlstars_id
    on stats_event_mode (event_mode_brawlstars_id);
-- 새 환경도 운영과 같은 12개로 시작한다. 비면 core-worker 가 아무것도 수집하지 않는다.
-- 첫 적용 뒤에는 다시 돌리지 않는다. 관리자가 지운(끈) 시드 모드가 되살아난다.
insert into stats_event_mode (event_mode_brawlstars_id, type)
values (0, 'RESULT_3VS3'),
       (2, 'RESULT_3VS3'),
       (3, 'RESULT_3VS3'),
       (5, 'RESULT_3VS3'),
       (6, 'SOLO_SHOWDOWN'),
       (9, 'DUO_SHOWDOWN'),
       (17, 'RESULT_3VS3'),
       (20, 'RESULT_3VS3'),
       (22, 'RESULT_3VS3'),
       (38, 'TRIO_SHOWDOWN'),
       (45, 'RESULT_3VS3'),
       (48, 'RESULT_3VS3')
on conflict (event_mode_brawlstars_id) do nothing;

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
    updated_at           timestamptz  not null default now(),
    deleted_at           timestamptz  null
);
create unique index if not exists uk_battle_event__brawlstarsid on battle_event (brawlstars_id);

-- =============================================================
-- 옛 통계 {table}_backup 5개(날짜 기준)
-- 패치 기준 통계로 전환하는 동안 남겨 두는 옛 통계. 2단계 릴리스에서 이 DDL 을 지운다(운영 테이블은 3단계에서 지운다). 월 파티션은 더 만들지 않는다.
-- 운영은 V261114a 가 원래 이름의 테이블·인덱스·제약·시퀀스에 _backup 을 붙여 만든 상태다.
-- =============================================================
-- (옛) 일별 브롤러별 전투 결과 통계
create sequence if not exists brawler_battle_result_stats_backup_seq increment by 100;
create table if not exists brawler_battle_result_stats_backup
(
    brawler_battle_result_stats_id bigint primary key default nextval('brawler_battle_result_stats_backup_seq'),
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
create index if not exists ix_brawler_battle_result_stats_backup_1 on brawler_battle_result_stats_backup (event_brawlstars_id, tier_range, battle_date);
create index if not exists ix_brawler_battle_result_stats_backup_2 on brawler_battle_result_stats_backup (brawler_brawlstars_id, tier_range, battle_date);
create unique index if not exists uk_brawler_battle_result_stats_backup__1
    on brawler_battle_result_stats_backup (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);
alter sequence brawler_battle_result_stats_backup_seq owned by brawler_battle_result_stats_backup.brawler_battle_result_stats_id;

-- (옛) 일별 브롤러 전투 랭크 통계
create sequence if not exists brawler_battle_rank_stats_backup_seq increment by 100;
create table if not exists brawler_battle_rank_stats_backup
(
    brawler_battle_rank_stats_id bigint primary key default nextval('brawler_battle_rank_stats_backup_seq'),
    event_brawlstars_id          bigint       not null,
    tier_range                   varchar(45)  not null,
    battle_date                  date         not null,
    brawler_brawlstars_id        bigint       not null,
    rank_counts                  jsonb        not null,
    total_count                  bigint       not null,
    created_at                   timestamptz  not null default now(),
    updated_at                   timestamptz  not null default now()
);
create index if not exists ix_brawler_battle_rank_stats_backup on brawler_battle_rank_stats_backup (event_brawlstars_id, tier_range, battle_date);
create unique index if not exists uk_brawler_battle_rank_stats_backup__1
    on brawler_battle_rank_stats_backup (event_brawlstars_id, tier_range, battle_date, brawler_brawlstars_id);
alter sequence brawler_battle_rank_stats_backup_seq owned by brawler_battle_rank_stats_backup.brawler_battle_rank_stats_id;

-- (옛) 브롤러 vs 적 브롤러(카운터) 전투 결과 통계 (파티셔닝: start_battle_date 월 범위)
-- 한 행 = (티어, 주 버킷)에서 brawler 가 enemy 를 상대로 만난 결과(brawler 기준). (X, Y)·(Y, X) 는 방향이 다른 별개 행이며 승↔패로 보완된다.
-- 월 파티션 {table}_p_YYYYMM 은 이름을 바꾸기 전에 만든 것만 남는다(이름은 원래 테이블 이름 그대로). default 파티션 없음.
-- 이 파일로 새로 만든 환경에는 파티션이 없다(옛 조합 잡은 그런 환경에서 실패한다).
-- PK 는 파티션 키를 포함해야 해서 (id, start_battle_date) 다. 유니크가 brawler 기준 조회를 겸하므로 조회 인덱스는 따로 두지 않는다.
create sequence if not exists brawler_enemy_battle_result_stats_backup_seq increment by 100;
create table if not exists brawler_enemy_battle_result_stats_backup
(
    brawler_enemy_battle_result_stats_id bigint       not null default nextval('brawler_enemy_battle_result_stats_backup_seq'),
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
    updated_at                           timestamptz  not null default now(),
    primary key (brawler_enemy_battle_result_stats_id, start_battle_date),
    constraint ck_brawler_enemy_battle_result_stats_backup_distinct_brawler
        check (brawler_brawlstars_id <> enemy_brawler_brawlstars_id)
) partition by range (start_battle_date);
create unique index if not exists uk_brawler_enemy_battle_result_stats_backup__1
    on brawler_enemy_battle_result_stats_backup (
        tier_range, brawler_brawlstars_id, start_battle_date, enemy_brawler_brawlstars_id
    );
alter sequence brawler_enemy_battle_result_stats_backup_seq owned by brawler_enemy_battle_result_stats_backup.brawler_enemy_battle_result_stats_id;

-- (옛) 브롤러 pair(같은 팀 2인 조합) 전투 결과 통계 (파티셔닝: start_battle_date 월 범위)
-- 한 행 = (티어, 주 버킷)에서 이 조합이 성립한 팀의 승·패·무. 조합은 brawler1 < brawler2 로 정렬한 1행이고 티어는 멤버 전원 기준(최저 trophies)이다.
-- 월 파티션 {table}_p_YYYYMM 은 이름을 바꾸기 전에 만든 것만 남는다(이름은 원래 테이블 이름 그대로). default 파티션 없음.
-- 이 파일로 새로 만든 환경에는 파티션이 없다(옛 조합 잡은 그런 환경에서 실패한다).
-- PK 는 파티션 키를 포함해야 해서 (id, start_battle_date) 다. 위치 인덱스는 "브롤러 X 가 어느 위치에 있든" 조회를 맡는다.
create sequence if not exists brawler_pair_battle_result_stats_backup_seq increment by 100;
create table if not exists brawler_pair_battle_result_stats_backup
(
    brawler_pair_battle_result_stats_id bigint      not null default nextval('brawler_pair_battle_result_stats_backup_seq'),
    tier_range                          varchar(45) not null,
    start_battle_date                   date        not null,
    end_battle_date                     date        not null,
    brawler1_brawlstars_id              bigint      not null,
    brawler2_brawlstars_id              bigint      not null,
    victory_count                       bigint      not null,
    defeat_count                        bigint      not null,
    draw_count                          bigint      not null,
    total_count                         bigint      not null,
    created_at                          timestamptz not null default now(),
    updated_at                          timestamptz not null default now(),
    primary key (brawler_pair_battle_result_stats_id, start_battle_date),
    constraint ck_brawler_pair_battle_result_stats_backup_ordered
        check (brawler1_brawlstars_id < brawler2_brawlstars_id)
) partition by range (start_battle_date);
create unique index if not exists uk_brawler_pair_battle_result_stats_backup__1
    on brawler_pair_battle_result_stats_backup (
        tier_range, start_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id
    );
create index if not exists ix_brawler_pair_battle_result_stats_backup__brawler1
    on brawler_pair_battle_result_stats_backup (tier_range, brawler1_brawlstars_id, start_battle_date);
create index if not exists ix_brawler_pair_battle_result_stats_backup__brawler2
    on brawler_pair_battle_result_stats_backup (tier_range, brawler2_brawlstars_id, start_battle_date);
alter sequence brawler_pair_battle_result_stats_backup_seq owned by brawler_pair_battle_result_stats_backup.brawler_pair_battle_result_stats_id;

-- (옛) 브롤러 trio(같은 팀 3인 조합 = 3v3 한 팀 전체) 전투 결과 통계 (파티셔닝: start_battle_date 월 범위)
-- 한 행 = (티어, 주 버킷)에서 이 조합이 성립한 팀의 승·패·무. 조합은 brawler1 < brawler2 < brawler3 로 정렬한 1행이고 티어는 멤버 전원 기준(최저 trophies)이다.
-- 월 파티션 {table}_p_YYYYMM 은 이름을 바꾸기 전에 만든 것만 남는다(이름은 원래 테이블 이름 그대로). default 파티션 없음.
-- 이 파일로 새로 만든 환경에는 파티션이 없다(옛 조합 잡은 그런 환경에서 실패한다).
-- PK 는 파티션 키를 포함해야 해서 (id, start_battle_date) 다. 위치 인덱스는 "브롤러 X 가 어느 위치에 있든" 조회를 맡는다.
create sequence if not exists brawler_trio_battle_result_stats_backup_seq increment by 100;
create table if not exists brawler_trio_battle_result_stats_backup
(
    brawler_trio_battle_result_stats_id bigint      not null default nextval('brawler_trio_battle_result_stats_backup_seq'),
    tier_range                          varchar(45) not null,
    start_battle_date                   date        not null,
    end_battle_date                     date        not null,
    brawler1_brawlstars_id              bigint      not null,
    brawler2_brawlstars_id              bigint      not null,
    brawler3_brawlstars_id              bigint      not null,
    victory_count                       bigint      not null,
    defeat_count                        bigint      not null,
    draw_count                          bigint      not null,
    total_count                         bigint      not null,
    created_at                          timestamptz not null default now(),
    updated_at                          timestamptz not null default now(),
    primary key (brawler_trio_battle_result_stats_id, start_battle_date),
    constraint ck_brawler_trio_battle_result_stats_backup_ordered
        check (brawler1_brawlstars_id < brawler2_brawlstars_id and brawler2_brawlstars_id < brawler3_brawlstars_id)
) partition by range (start_battle_date);
create unique index if not exists uk_brawler_trio_battle_result_stats_backup__1
    on brawler_trio_battle_result_stats_backup (
        tier_range, start_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id, brawler3_brawlstars_id
    );
create index if not exists ix_brawler_trio_battle_result_stats_backup__brawler1
    on brawler_trio_battle_result_stats_backup (tier_range, brawler1_brawlstars_id, start_battle_date);
create index if not exists ix_brawler_trio_battle_result_stats_backup__brawler2
    on brawler_trio_battle_result_stats_backup (tier_range, brawler2_brawlstars_id, start_battle_date);
create index if not exists ix_brawler_trio_battle_result_stats_backup__brawler3
    on brawler_trio_battle_result_stats_backup (tier_range, brawler3_brawlstars_id, start_battle_date);
alter sequence brawler_trio_battle_result_stats_backup_seq owned by brawler_trio_battle_result_stats_backup.brawler_trio_battle_result_stats_id;

-- =============================================================
-- 패치별 통계 5개와 체크포인트
-- 한 줄 = 패치 × 키. 잡이 SQL 로 직접 쓴다. id 는 identity, 외래 키 제약과 파티션은 두지 않는다.
-- =============================================================

-- 패치별 브롤러 전투 결과 통계
create table if not exists brawler_battle_result_stats
(
    brawler_battle_result_stats_id bigint generated always as identity primary key,
    brawlstars_patch_id            bigint      not null,
    tier_range                     varchar(45) not null,
    event_brawlstars_id            bigint      not null,
    brawler_brawlstars_id          bigint      not null,
    victory_count                  bigint      not null,
    defeat_count                   bigint      not null,
    draw_count                     bigint      not null,
    total_count                    bigint      not null,
    star_player_count              bigint      not null,
    created_at                     timestamptz not null default now(),
    updated_at                     timestamptz not null default now()
);
create unique index if not exists uk_brawler_battle_result_stats__1
    on brawler_battle_result_stats (brawlstars_patch_id, tier_range, event_brawlstars_id, brawler_brawlstars_id);

-- 패치별 브롤러 전투 순위 통계. 순위별 횟수는 rank1_count ~ rank10_count 컬럼 10개다.
create table if not exists brawler_battle_rank_stats
(
    brawler_battle_rank_stats_id bigint generated always as identity primary key,
    brawlstars_patch_id          bigint      not null,
    tier_range                   varchar(45) not null,
    event_brawlstars_id          bigint      not null,
    brawler_brawlstars_id        bigint      not null,
    rank1_count                  bigint      not null,
    rank2_count                  bigint      not null,
    rank3_count                  bigint      not null,
    rank4_count                  bigint      not null,
    rank5_count                  bigint      not null,
    rank6_count                  bigint      not null,
    rank7_count                  bigint      not null,
    rank8_count                  bigint      not null,
    rank9_count                  bigint      not null,
    rank10_count                 bigint      not null,
    total_count                  bigint      not null,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);
create unique index if not exists uk_brawler_battle_rank_stats__1
    on brawler_battle_rank_stats (brawlstars_patch_id, tier_range, event_brawlstars_id, brawler_brawlstars_id);

-- 패치별 브롤러 vs 상대 브롤러 전투 결과 통계
create table if not exists brawler_enemy_battle_result_stats
(
    brawler_enemy_battle_result_stats_id bigint generated always as identity primary key,
    brawlstars_patch_id                  bigint      not null,
    tier_range                           varchar(45) not null,
    brawler_brawlstars_id                bigint      not null,
    enemy_brawler_brawlstars_id          bigint      not null,
    victory_count                        bigint      not null,
    defeat_count                         bigint      not null,
    draw_count                           bigint      not null,
    total_count                          bigint      not null,
    created_at                           timestamptz not null default now(),
    updated_at                           timestamptz not null default now(),
    constraint ck_brawler_enemy_battle_result_stats_distinct_brawler
        check (brawler_brawlstars_id <> enemy_brawler_brawlstars_id)
);
create unique index if not exists uk_brawler_enemy_battle_result_stats__1
    on brawler_enemy_battle_result_stats (brawlstars_patch_id, tier_range, brawler_brawlstars_id, enemy_brawler_brawlstars_id);

-- 패치별 브롤러 pair(같은 팀 2인 조합) 전투 결과 통계. 조합은 brawler1 < brawler2 로 정렬한 한 줄이다.
-- 브롤러가 어느 자리에 있든 찾도록 둘째 브롤러 인덱스를 둔다(첫째는 유니크의 앞부분으로 찾는다).
create table if not exists brawler_pair_battle_result_stats
(
    brawler_pair_battle_result_stats_id bigint generated always as identity primary key,
    brawlstars_patch_id                 bigint      not null,
    tier_range                          varchar(45) not null,
    brawler1_brawlstars_id              bigint      not null,
    brawler2_brawlstars_id              bigint      not null,
    victory_count                       bigint      not null,
    defeat_count                        bigint      not null,
    draw_count                          bigint      not null,
    total_count                         bigint      not null,
    created_at                          timestamptz not null default now(),
    updated_at                          timestamptz not null default now(),
    constraint ck_brawler_pair_battle_result_stats_ordered
        check (brawler1_brawlstars_id < brawler2_brawlstars_id)
);
create unique index if not exists uk_brawler_pair_battle_result_stats__1
    on brawler_pair_battle_result_stats (brawlstars_patch_id, tier_range, brawler1_brawlstars_id, brawler2_brawlstars_id);
create index if not exists ix_brawler_pair_battle_result_stats__brawler2
    on brawler_pair_battle_result_stats (brawlstars_patch_id, tier_range, brawler2_brawlstars_id);

-- 패치별 브롤러 trio(같은 팀 3인 조합 = 3v3 한 팀 전체) 전투 결과 통계. 조합은 brawler1 < brawler2 < brawler3 로 정렬한 한 줄이다.
-- 브롤러가 어느 자리에 있든 찾도록 둘째·셋째 브롤러 인덱스를 둔다.
create table if not exists brawler_trio_battle_result_stats
(
    brawler_trio_battle_result_stats_id bigint generated always as identity primary key,
    brawlstars_patch_id                 bigint      not null,
    tier_range                          varchar(45) not null,
    brawler1_brawlstars_id              bigint      not null,
    brawler2_brawlstars_id              bigint      not null,
    brawler3_brawlstars_id              bigint      not null,
    victory_count                       bigint      not null,
    defeat_count                        bigint      not null,
    draw_count                          bigint      not null,
    total_count                         bigint      not null,
    created_at                          timestamptz not null default now(),
    updated_at                          timestamptz not null default now(),
    constraint ck_brawler_trio_battle_result_stats_ordered
        check (brawler1_brawlstars_id < brawler2_brawlstars_id and brawler2_brawlstars_id < brawler3_brawlstars_id)
);
create unique index if not exists uk_brawler_trio_battle_result_stats__1
    on brawler_trio_battle_result_stats (
        brawlstars_patch_id, tier_range, brawler1_brawlstars_id, brawler2_brawlstars_id, brawler3_brawlstars_id
    );
create index if not exists ix_brawler_trio_battle_result_stats__brawler2
    on brawler_trio_battle_result_stats (brawlstars_patch_id, tier_range, brawler2_brawlstars_id);
create index if not exists ix_brawler_trio_battle_result_stats__brawler3
    on brawler_trio_battle_result_stats (brawlstars_patch_id, tier_range, brawler3_brawlstars_id);

-- 패치별 통계의 체크포인트. 한 줄 = 통계 유형 × 패치.
-- 한 패치의 통계는 "전투 날짜가 applied_start_date ~ applied_end_date 안이고 id 가 last_read_battle_id 이하"인 원본 전투를 집계한 값과 같다.
-- applied_end_date 는 다음 패치가 없으면 비운다. last_battle_at 은 반영된 전투가 없으면 비어 있다. version 은 쓸 때마다 1 올린다.
create table if not exists stats_checkpoint
(
    stats_checkpoint_id bigint generated always as identity primary key,
    stats_type          varchar(45) not null,
    brawlstars_patch_id bigint      not null,
    last_read_battle_id bigint      not null,
    applied_start_date  date        not null,
    applied_end_date    date        null,
    last_battle_at      timestamptz null,
    version             bigint      not null,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);
create unique index if not exists uk_stats_checkpoint__stats_type_brawlstars_patch_id
    on stats_checkpoint (stats_type, brawlstars_patch_id);

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
    updated_at        timestamptz  not null default now(),
    deleted_at        timestamptz  null
);
create unique index if not exists uk_brawler__brawlstarsid on brawler (brawlstars_id);

-- brawler_gear table
create table if not exists brawler_gear
(
    brawler_gear_id       bigint generated always as identity primary key,
    brawler_brawlstars_id bigint       not null,
    gear_brawlstars_id    bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    deleted_at            timestamptz  null
);
create unique index if not exists uk_brawlergear__brawlerbrawlstarsid_gearbrawlstarsid on brawler_gear (brawler_brawlstars_id, gear_brawlstars_id);

-- gadget table
create table if not exists gadget
(
    gadget_id             bigint generated always as identity primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    deleted_at            timestamptz  null
);
create unique index if not exists uk_gadget__brawlstarsid on gadget (brawlstars_id);

-- gear table
create table if not exists gear
(
    gear_id           bigint generated always as identity primary key,
    brawlstars_id     bigint       not null,
    rarity            varchar(45)  not null,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now(),
    deleted_at        timestamptz  null
);
create unique index if not exists uk_gear__brawlstarsid on gear (brawlstars_id);

-- hyper_charge table
create table if not exists hyper_charge
(
    hyper_charge_id       bigint generated always as identity primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    deleted_at            timestamptz  null
);
create unique index if not exists uk_hypercharge__brawlstarsid on hyper_charge (brawlstars_id);

-- star_power table
create table if not exists star_power
(
    star_power_id         bigint generated always as identity primary key,
    brawlstars_id         bigint       not null,
    brawler_brawlstars_id bigint       not null,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    deleted_at            timestamptz  null
);
create unique index if not exists uk_starpower__brawlstarsid on star_power (brawlstars_id);

-- buffy table
create table if not exists buffy
(
    buffy_id              bigint generated always as identity primary key,
    brawler_brawlstars_id bigint      not null,
    type                  varchar(25) not null,
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now()
);
create unique index if not exists uk_buffy__brawlerbrawlstarsid_type on buffy (brawler_brawlstars_id, type);

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

-- brawlstars_api_spec table
create table if not exists brawlstars_api_spec
(
    brawlstars_api_spec_id bigint generated always as identity primary key,
    content                text        not null,
    collected_at           timestamptz not null,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now()
);

-- brawlstars_news_article table
create table if not exists brawlstars_news_article
(
    brawlstars_news_article_id bigint generated always as identity primary key,
    article_key                varchar(255) not null,
    link_url                   varchar(500) not null,
    title                      varchar(255) not null,
    published_at               timestamptz  not null,
    type                       varchar(25)  not null,
    raw_body                   text         not null,
    content                    text         not null,
    created_at                 timestamptz  not null default now(),
    updated_at                 timestamptz  not null default now()
);

-- brawlstars_patch table
create table if not exists brawlstars_patch
(
    brawlstars_patch_id bigint generated always as identity primary key,
    patch_date          date         not null,
    memo                varchar(255) null,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now()
);
create unique index if not exists uk_brawlstars_patch__patch_date on brawlstars_patch (patch_date);

-- brawler_gear_ownership_stats table
create sequence if not exists brawler_gear_ownership_stats_seq increment by 100;
create table if not exists brawler_gear_ownership_stats
(
    brawler_gear_ownership_stats_id bigint primary key default nextval('brawler_gear_ownership_stats_seq'),
    brawler_brawlstars_id           bigint      not null,
    trophy_range                    varchar(45) not null,
    gear_brawlstars_id              bigint      not null,
    owner_count                     bigint      not null,
    total_count                     bigint      not null,
    created_at                      timestamptz not null default now(),
    updated_at                      timestamptz not null default now()
);
create unique index if not exists uk_brawler_gear_ownership_stats_1 on brawler_gear_ownership_stats (brawler_brawlstars_id, trophy_range, gear_brawlstars_id);
alter sequence brawler_gear_ownership_stats_seq owned by brawler_gear_ownership_stats.brawler_gear_ownership_stats_id;

-- brawler_gadget_ownership_stats table
create sequence if not exists brawler_gadget_ownership_stats_seq increment by 100;
create table if not exists brawler_gadget_ownership_stats
(
    brawler_gadget_ownership_stats_id bigint primary key default nextval('brawler_gadget_ownership_stats_seq'),
    brawler_brawlstars_id             bigint      not null,
    trophy_range                      varchar(45) not null,
    gadget_brawlstars_id              bigint      not null,
    owner_count                       bigint      not null,
    total_count                       bigint      not null,
    created_at                        timestamptz not null default now(),
    updated_at                        timestamptz not null default now()
);
create unique index if not exists uk_brawler_gadget_ownership_stats_1 on brawler_gadget_ownership_stats (brawler_brawlstars_id, trophy_range, gadget_brawlstars_id);
alter sequence brawler_gadget_ownership_stats_seq owned by brawler_gadget_ownership_stats.brawler_gadget_ownership_stats_id;

-- brawler_star_power_ownership_stats table
create sequence if not exists brawler_star_power_ownership_stats_seq increment by 100;
create table if not exists brawler_star_power_ownership_stats
(
    brawler_star_power_ownership_stats_id bigint primary key default nextval('brawler_star_power_ownership_stats_seq'),
    brawler_brawlstars_id                 bigint      not null,
    trophy_range                          varchar(45) not null,
    star_power_brawlstars_id              bigint      not null,
    owner_count                           bigint      not null,
    total_count                           bigint      not null,
    created_at                            timestamptz not null default now(),
    updated_at                            timestamptz not null default now()
);
create unique index if not exists uk_brawler_star_power_ownership_stats_1 on brawler_star_power_ownership_stats (brawler_brawlstars_id, trophy_range, star_power_brawlstars_id);
alter sequence brawler_star_power_ownership_stats_seq owned by brawler_star_power_ownership_stats.brawler_star_power_ownership_stats_id;

-- brawler_hyper_charge_ownership_stats table
create sequence if not exists brawler_hyper_charge_ownership_stats_seq increment by 100;
create table if not exists brawler_hyper_charge_ownership_stats
(
    brawler_hyper_charge_ownership_stats_id bigint primary key default nextval('brawler_hyper_charge_ownership_stats_seq'),
    brawler_brawlstars_id                   bigint      not null,
    trophy_range                            varchar(45) not null,
    hyper_charge_brawlstars_id              bigint      not null,
    owner_count                             bigint      not null,
    total_count                             bigint      not null,
    created_at                              timestamptz not null default now(),
    updated_at                              timestamptz not null default now()
);
create unique index if not exists uk_brawler_hyper_charge_ownership_stats_1 on brawler_hyper_charge_ownership_stats (brawler_brawlstars_id, trophy_range, hyper_charge_brawlstars_id);
alter sequence brawler_hyper_charge_ownership_stats_seq owned by brawler_hyper_charge_ownership_stats.brawler_hyper_charge_ownership_stats_id;

-- brawler_buffy_ownership_stats table
create sequence if not exists brawler_buffy_ownership_stats_seq increment by 100;
create table if not exists brawler_buffy_ownership_stats
(
    brawler_buffy_ownership_stats_id bigint primary key default nextval('brawler_buffy_ownership_stats_seq'),
    brawler_brawlstars_id            bigint      not null,
    trophy_range                     varchar(45) not null,
    buffy_type                       varchar(25) not null,
    owner_count                      bigint      not null,
    total_count                      bigint      not null,
    created_at                       timestamptz not null default now(),
    updated_at                       timestamptz not null default now()
);
create unique index if not exists uk_brawler_buffy_ownership_stats_1 on brawler_buffy_ownership_stats (brawler_brawlstars_id, trophy_range, buffy_type);
alter sequence brawler_buffy_ownership_stats_seq owned by brawler_buffy_ownership_stats.brawler_buffy_ownership_stats_id;

-- feedback table
create table if not exists feedback
(
    id           bigserial   primary key,
    rating       varchar(32) not null,
    content      text        not null,
    checked      boolean     not null default false,
    submitted_at timestamptz not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
create index if not exists ix_feedback__checked__submittedat
    on feedback (checked, submitted_at desc);
