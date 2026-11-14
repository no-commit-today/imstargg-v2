-- 통계를 날짜 기준에서 패치 기준으로 바꾸는 1단계 DDL. 옛 통계 테이블 5개를 _backup 으로 옮기고, 원래 이름으로 패치별 통계 테이블
-- 5개와 체크포인트 테이블을 만든다.
-- 대상: brawler_battle_result_stats, brawler_battle_rank_stats, brawler_enemy_battle_result_stats,
--   brawler_pair_battle_result_stats, brawler_trio_battle_result_stats (기존 → {t}_backup 으로 이름을 바꾸고 같은 이름으로 새로 만든다),
--   stats_checkpoint (신규)
--
-- 배경: 패치 기준 통계 설계(docs/superpowers/specs/2026-11-13-patch-based-statistics-design.md 4장, 10장).
--   - 새 통계 테이블은 날짜 컬럼(battle_date, start_battle_date, end_battle_date) 자리에 brawlstars_patch_id 를 두고 패치마다 키별 한 줄이다.
--     id 는 identity, 외래 키 제약과 파티션은 두지 않는다. 1인 순위는 JSON 배열 대신 rank1_count ~ rank10_count 컬럼 10개다.
--   - stats_checkpoint 는 통계 유형 × 패치 한 줄이다. 한 패치의 통계는 "전투 날짜가 적용한 범위 안이고 id 가 last_read_battle_id 이하"인
--     원본 전투를 집계한 값과 같다(스펙 4.2).
--   - 옛 통계는 전환(2단계)까지 _backup 에서 옛 잡이 계속 쓰고 옛 조회가 계속 읽는다. 3단계(전환 1주 뒤)에서 지운다.
--
-- 바꾸는 이름({t} 는 위 다섯 테이블):
--   - 테이블 {t} → {t}_backup, PK 인덱스 {t}_pkey → {t}_backup_pkey (PK 제약 이름도 함께 바뀐다),
--     id 시퀀스 {t}_seq → {t}_backup_seq, 유니크 uk_{t}__1 → uk_{t}_backup__1.
--   - 인덱스: ix_brawler_battle_result_stats_1·_2, ix_brawler_battle_rank_stats, ix_brawler_pair_battle_result_stats__brawler1·2,
--     ix_brawler_trio_battle_result_stats__brawler1·2·3, ix_brawler_enemy_battle_result_stats_1 (운영에만 있을 수 있다)
--     → 테이블 이름 자리에 _backup 을 넣은 이름.
--   - CHECK: ck_brawler_pair_battle_result_stats_ordered, ck_brawler_trio_battle_result_stats_ordered,
--     ck_brawler_enemy_battle_result_stats_distinct_brawler → 테이블 이름 자리에 _backup 을 넣은 이름.
--     새 테이블이 원래 이름의 CHECK 를 다시 갖는다.
--   - 그대로 두는 것: 조합 3개의 월 파티션 {t}_p_YYYYMM 과 그 인덱스·PK(자동 이름이라 _backup 이 붙지 않는다. 겹치는 이름이 없다).
--     파티션의 CHECK 는 부모의 이름 바꾸기를 따라 함께 바뀐다(rename constraint 가 상속된 CHECK 에 재귀한다).
--     시퀀스 이름을 바꿔도 id 기본값(nextval(regclass))과 owned by 는 oid 로 이어져 옛 잡의 nextval 이 그대로 산다.
--
-- 배포 순서(스펙 10장 1단계):
--   ① 이 파일을 운영 DB 에 적용한다.
--   ② 이어서 core-api·core-batch 를 배포한다(Deploy 워크플로). 이 릴리스의 옛 조회·옛 잡은 _backup 테이블을 읽고 쓴다.
--      ① 과 ② 를 붙여서 한다.
--   ③ 배포 설정(imstargg-deploy)에 새 CronJob 5개를 추가한다. ② 보다 먼저 추가하면 옛 이미지가 모르는 잡 이름으로 뜬다.
--
-- 끊기는 구간(① 커밋 ~ ② 배포 끝):
--   - 옛 core-api 가 구조가 바뀐 테이블(원래 이름의 새 테이블)을 읽어, 통계 엔드포인트 8개가 캐시에 없는 요청에서 500 을 준다.
--     지금 프론트는 통계 조회가 실패하면 페이지 렌더가 실패하므로, 그런 요청에서는 브롤러 목록·브롤러 상세·이벤트 상세 페이지가 오류가 된다.
--   - 옛 통계 잡과 파티션 관리 잡(create_month_partitions 는 조합 테이블을 원래 이름으로 찾는다)도 그 사이에 돌면 실패한다.
--     ② 의 core-batch 는 월 파티션 관리를 부르지 않는다. 옛 조합 테이블에는 이번 달·다음 달 파티션이 이미 있어
--     옛 잡은 다음 달 말일까지 쓸 수 있다. 전환은 그 전에 끝낸다.
--   - 길이는 이 SQL 적용 시간에 core-api 배포 시간을 더한 값이다(재지 않았다).
--   - 그래서 트래픽이 적고 통계 배치 시간대(02:00~09:00 KST) 밖에서 적용한다.
--
-- 적용: psql -v ON_ERROR_STOP=1 -f V261114a__rename_stats_to_backup_and_create_patch_stats.sql
--   - 한 트랜잭션이다. 이름 바꾸기와 새 테이블 만들기가 함께 커밋되거나 함께 되돌아가, "이름은 바꿨는데 새 테이블은 없는" 상태가 남지 않는다.
--   - lock_timeout 3s(세션 설정). 다섯 테이블에 ACCESS EXCLUSIVE 를 잡는다. 데이터를 옮기지 않아 잡는 시간은 짧다.
--     락을 못 잡아 실패하면 아무것도 바뀌지 않으니 배치 시간대를 피해 다시 시도한다.
--   - 있어야 하는 것(테이블, PK 인덱스, 유니크, 시퀀스, CHECK)이 없으면 오류로 멈추고 아무것도 바꾸지 않는다.
--     운영에 없을 수 있는 인덱스(ix_…)만 alter index if exists 로 바꾼다.
--   - 마지막 점검: 다섯 테이블이 새 모양(brawlstars_patch_id)이고 _backup 이 옛 모양(날짜 컬럼)이 아니면 오류를 내 트랜잭션 전체를 되돌린다.
--
-- 멱등: 같은 파일을 다시 적용해도 오류 없이 아무것도 바꾸지 않는다.
--   - 이름 바꾸기는 {t}_backup 이 없고, {t} 가 있고, {t} 에 옛 날짜 컬럼(1인은 battle_date, 조합은 start_battle_date)이 있을 때만 한다.
--   - 새 테이블·인덱스는 if not exists.
--   - _backup 을 지운 뒤(3단계)에는 다시 적용하지 않는다. 마지막 점검이 _backup 없음으로 실패한다(의도).
--
-- 되돌리기(스펙 10장): 1단계 안에서, _backup 을 지우기 전까지 가능하다. 배포 설정에서 새 CronJob 을 빼고, 아래 SQL 을 적용한 뒤,
--   core-api·core-batch 를 이전 버전으로 다시 올린다. 이때도 SQL 과 배포 사이에 통계가 끊긴다.
--   새 테이블 5개와 stats_checkpoint 를 지우므로 그동안 쌓은 패치별 통계와 체크포인트는 사라진다(원본에서 다시 만든다).
--   되돌린 뒤 이 파일을 다시 적용하면 처음처럼 이름을 바꾸고 새 테이블을 만든다.
--   되돌리는 SQL(실행문이 아니라 배포 담당자가 고르는 절차다. 한 트랜잭션으로 돌린다):
--     set lock_timeout = '3s';
--     begin;
--     drop table brawler_battle_result_stats;
--     drop table brawler_battle_rank_stats;
--     drop table brawler_enemy_battle_result_stats;
--     drop table brawler_pair_battle_result_stats;
--     drop table brawler_trio_battle_result_stats;
--     drop table stats_checkpoint;
--
--     alter table brawler_battle_result_stats_backup rename to brawler_battle_result_stats;
--     alter index brawler_battle_result_stats_backup_pkey rename to brawler_battle_result_stats_pkey;
--     alter index uk_brawler_battle_result_stats_backup__1 rename to uk_brawler_battle_result_stats__1;
--     alter index if exists ix_brawler_battle_result_stats_backup_1 rename to ix_brawler_battle_result_stats_1;
--     alter index if exists ix_brawler_battle_result_stats_backup_2 rename to ix_brawler_battle_result_stats_2;
--     alter sequence brawler_battle_result_stats_backup_seq rename to brawler_battle_result_stats_seq;
--
--     alter table brawler_battle_rank_stats_backup rename to brawler_battle_rank_stats;
--     alter index brawler_battle_rank_stats_backup_pkey rename to brawler_battle_rank_stats_pkey;
--     alter index uk_brawler_battle_rank_stats_backup__1 rename to uk_brawler_battle_rank_stats__1;
--     alter index if exists ix_brawler_battle_rank_stats_backup rename to ix_brawler_battle_rank_stats;
--     alter sequence brawler_battle_rank_stats_backup_seq rename to brawler_battle_rank_stats_seq;
--
--     alter table brawler_enemy_battle_result_stats_backup rename to brawler_enemy_battle_result_stats;
--     alter index brawler_enemy_battle_result_stats_backup_pkey rename to brawler_enemy_battle_result_stats_pkey;
--     alter index uk_brawler_enemy_battle_result_stats_backup__1 rename to uk_brawler_enemy_battle_result_stats__1;
--     alter index if exists ix_brawler_enemy_battle_result_stats_backup_1 rename to ix_brawler_enemy_battle_result_stats_1;
--     alter sequence brawler_enemy_battle_result_stats_backup_seq rename to brawler_enemy_battle_result_stats_seq;
--     alter table brawler_enemy_battle_result_stats rename constraint ck_brawler_enemy_battle_result_stats_backup_distinct_brawler
--         to ck_brawler_enemy_battle_result_stats_distinct_brawler;
--
--     alter table brawler_pair_battle_result_stats_backup rename to brawler_pair_battle_result_stats;
--     alter index brawler_pair_battle_result_stats_backup_pkey rename to brawler_pair_battle_result_stats_pkey;
--     alter index uk_brawler_pair_battle_result_stats_backup__1 rename to uk_brawler_pair_battle_result_stats__1;
--     alter index if exists ix_brawler_pair_battle_result_stats_backup__brawler1 rename to ix_brawler_pair_battle_result_stats__brawler1;
--     alter index if exists ix_brawler_pair_battle_result_stats_backup__brawler2 rename to ix_brawler_pair_battle_result_stats__brawler2;
--     alter sequence brawler_pair_battle_result_stats_backup_seq rename to brawler_pair_battle_result_stats_seq;
--     alter table brawler_pair_battle_result_stats rename constraint ck_brawler_pair_battle_result_stats_backup_ordered
--         to ck_brawler_pair_battle_result_stats_ordered;
--
--     alter table brawler_trio_battle_result_stats_backup rename to brawler_trio_battle_result_stats;
--     alter index brawler_trio_battle_result_stats_backup_pkey rename to brawler_trio_battle_result_stats_pkey;
--     alter index uk_brawler_trio_battle_result_stats_backup__1 rename to uk_brawler_trio_battle_result_stats__1;
--     alter index if exists ix_brawler_trio_battle_result_stats_backup__brawler1 rename to ix_brawler_trio_battle_result_stats__brawler1;
--     alter index if exists ix_brawler_trio_battle_result_stats_backup__brawler2 rename to ix_brawler_trio_battle_result_stats__brawler2;
--     alter index if exists ix_brawler_trio_battle_result_stats_backup__brawler3 rename to ix_brawler_trio_battle_result_stats__brawler3;
--     alter sequence brawler_trio_battle_result_stats_backup_seq rename to brawler_trio_battle_result_stats_seq;
--     alter table brawler_trio_battle_result_stats rename constraint ck_brawler_trio_battle_result_stats_backup_ordered
--         to ck_brawler_trio_battle_result_stats_ordered;
--     commit;
--
-- 참고: PG18 은 NOT NULL 제약에도 이름({table}_{column}_not_null)을 붙이고 같은 스키마에서 겹치지 않게 고른다.
--   이름을 바꿔도 _backup 테이블의 NOT NULL 제약은 옛 이름({t}_…_not_null)을 그대로 가져서, 이 파일이 만드는 새 테이블의 NOT NULL 제약은
--   끝에 1 이 붙는다(…_not_null1). schema.sql 로 새로 만든 환경과 이 이름만 다르다. 앱은 이 이름을 쓰지 않는다.

set lock_timeout = '3s';

begin;

-- -------------------------------------------------------------
-- 1. 옛 통계 테이블 5개를 _backup 으로 옮긴다.
--    인덱스·제약·시퀀스 이름은 테이블 이름을 따라 바뀌지 않으므로 함께 바꾼다.
--    PK 인덱스(…_pkey)·유니크(uk_…__1)·위치 인덱스(…__brawlerN)는 새 테이블의 이름과 충돌한다.
-- -------------------------------------------------------------
do $$
begin
    -- 1인 승률
    if to_regclass('brawler_battle_result_stats_backup') is null
        and to_regclass('brawler_battle_result_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_battle_result_stats'
              and column_name = 'battle_date'
        ) then
        alter table brawler_battle_result_stats rename to brawler_battle_result_stats_backup;
        alter index brawler_battle_result_stats_pkey rename to brawler_battle_result_stats_backup_pkey;
        alter index uk_brawler_battle_result_stats__1 rename to uk_brawler_battle_result_stats_backup__1;
        alter index if exists ix_brawler_battle_result_stats_1 rename to ix_brawler_battle_result_stats_backup_1;
        alter index if exists ix_brawler_battle_result_stats_2 rename to ix_brawler_battle_result_stats_backup_2;
        alter sequence brawler_battle_result_stats_seq rename to brawler_battle_result_stats_backup_seq;
    end if;

    -- 1인 순위
    if to_regclass('brawler_battle_rank_stats_backup') is null
        and to_regclass('brawler_battle_rank_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_battle_rank_stats'
              and column_name = 'battle_date'
        ) then
        alter table brawler_battle_rank_stats rename to brawler_battle_rank_stats_backup;
        alter index brawler_battle_rank_stats_pkey rename to brawler_battle_rank_stats_backup_pkey;
        alter index uk_brawler_battle_rank_stats__1 rename to uk_brawler_battle_rank_stats_backup__1;
        alter index if exists ix_brawler_battle_rank_stats rename to ix_brawler_battle_rank_stats_backup;
        alter sequence brawler_battle_rank_stats_seq rename to brawler_battle_rank_stats_backup_seq;
    end if;

    -- 상대. ix_brawler_enemy_battle_result_stats_1 은 schema.sql 에 없고 운영에만 있을 수 있다.
    if to_regclass('brawler_enemy_battle_result_stats_backup') is null
        and to_regclass('brawler_enemy_battle_result_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_enemy_battle_result_stats'
              and column_name = 'start_battle_date'
        ) then
        alter table brawler_enemy_battle_result_stats rename to brawler_enemy_battle_result_stats_backup;
        alter index brawler_enemy_battle_result_stats_pkey rename to brawler_enemy_battle_result_stats_backup_pkey;
        alter index uk_brawler_enemy_battle_result_stats__1 rename to uk_brawler_enemy_battle_result_stats_backup__1;
        alter index if exists ix_brawler_enemy_battle_result_stats_1 rename to ix_brawler_enemy_battle_result_stats_backup_1;
        alter sequence brawler_enemy_battle_result_stats_seq rename to brawler_enemy_battle_result_stats_backup_seq;
        alter table brawler_enemy_battle_result_stats_backup rename constraint ck_brawler_enemy_battle_result_stats_distinct_brawler
            to ck_brawler_enemy_battle_result_stats_backup_distinct_brawler;
    end if;

    -- 2인 조합
    if to_regclass('brawler_pair_battle_result_stats_backup') is null
        and to_regclass('brawler_pair_battle_result_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_pair_battle_result_stats'
              and column_name = 'start_battle_date'
        ) then
        alter table brawler_pair_battle_result_stats rename to brawler_pair_battle_result_stats_backup;
        alter index brawler_pair_battle_result_stats_pkey rename to brawler_pair_battle_result_stats_backup_pkey;
        alter index uk_brawler_pair_battle_result_stats__1 rename to uk_brawler_pair_battle_result_stats_backup__1;
        alter index if exists ix_brawler_pair_battle_result_stats__brawler1 rename to ix_brawler_pair_battle_result_stats_backup__brawler1;
        alter index if exists ix_brawler_pair_battle_result_stats__brawler2 rename to ix_brawler_pair_battle_result_stats_backup__brawler2;
        alter sequence brawler_pair_battle_result_stats_seq rename to brawler_pair_battle_result_stats_backup_seq;
        alter table brawler_pair_battle_result_stats_backup rename constraint ck_brawler_pair_battle_result_stats_ordered
            to ck_brawler_pair_battle_result_stats_backup_ordered;
    end if;

    -- 3인 조합
    if to_regclass('brawler_trio_battle_result_stats_backup') is null
        and to_regclass('brawler_trio_battle_result_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_trio_battle_result_stats'
              and column_name = 'start_battle_date'
        ) then
        alter table brawler_trio_battle_result_stats rename to brawler_trio_battle_result_stats_backup;
        alter index brawler_trio_battle_result_stats_pkey rename to brawler_trio_battle_result_stats_backup_pkey;
        alter index uk_brawler_trio_battle_result_stats__1 rename to uk_brawler_trio_battle_result_stats_backup__1;
        alter index if exists ix_brawler_trio_battle_result_stats__brawler1 rename to ix_brawler_trio_battle_result_stats_backup__brawler1;
        alter index if exists ix_brawler_trio_battle_result_stats__brawler2 rename to ix_brawler_trio_battle_result_stats_backup__brawler2;
        alter index if exists ix_brawler_trio_battle_result_stats__brawler3 rename to ix_brawler_trio_battle_result_stats_backup__brawler3;
        alter sequence brawler_trio_battle_result_stats_seq rename to brawler_trio_battle_result_stats_backup_seq;
        alter table brawler_trio_battle_result_stats_backup rename constraint ck_brawler_trio_battle_result_stats_ordered
            to ck_brawler_trio_battle_result_stats_backup_ordered;
    end if;
end $$;

-- -------------------------------------------------------------
-- 2. 패치별 통계 테이블 5개와 체크포인트 테이블(schema.sql 의 정의와 같다).
-- -------------------------------------------------------------

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

-- 패치별 브롤러 전투 순위 통계
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

-- 패치별 브롤러 pair(같은 팀 2인 조합) 전투 결과 통계
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

-- 패치별 브롤러 trio(같은 팀 3인 조합 = 3v3 한 팀 전체) 전투 결과 통계
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

-- 패치별 통계의 체크포인트(통계 유형 × 패치 한 줄)
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

-- -------------------------------------------------------------
-- 3. 점검: 원래 이름은 새 모양, _backup 은 옛 모양이어야 한다.
--    1 의 조건이 맞지 않아 이름을 못 바꿨는데 2 의 if not exists 가 옛 테이블을 보고 건너뛴 경우를 여기서 막는다.
--    (그런 경우 대개 그 전에 2 의 유니크 인덱스 만들기가 brawlstars_patch_id 없음으로 실패한다. 이 점검은 그 밖의 경우를 위한 것이다.)
-- -------------------------------------------------------------
do $$
declare
    t text;
    old_date_column text;
begin
    foreach t in array array[
        'brawler_battle_result_stats',
        'brawler_battle_rank_stats',
        'brawler_enemy_battle_result_stats',
        'brawler_pair_battle_result_stats',
        'brawler_trio_battle_result_stats'
    ] loop
        old_date_column := case when t in ('brawler_battle_result_stats', 'brawler_battle_rank_stats')
            then 'battle_date' else 'start_battle_date' end;
        if not exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema() and table_name = t and column_name = 'brawlstars_patch_id'
        ) then
            raise exception '% 이 패치별 통계 모양이 아니다(brawlstars_patch_id 없음)', t;
        end if;
        if not exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema() and table_name = t || '_backup' and column_name = old_date_column
        ) then
            raise exception '%_backup 이 없거나 옛 통계 모양이 아니다(% 없음)', t, old_date_column;
        end if;
    end loop;
end $$;

commit;
