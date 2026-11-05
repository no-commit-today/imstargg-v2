-- 조합 통계를 재구성한다. 2인 조합·카운터를 이벤트 키 없는 월 파티션 테이블로 바꾸고, 3인 조합 테이블을 더하고,
-- 쇼다운 조합 순위 테이블을 지운다.
-- 대상: brawler_pair_battle_result_stats, brawler_enemy_battle_result_stats (기존 → _legacy 로 이름을 바꾸고 같은 이름으로 새로 만든다),
--   brawler_trio_battle_result_stats (신규), brawler_pair_battle_rank_stats (삭제)
--
-- 배경: 스펙 B(조합 통계 재구성, docs/superpowers/specs/2026-11-04-combo-stats-redesign-design.md).
--   - 결정 1: 2인 조합·카운터에서 이벤트 키를 뺀다. 맵에 따른 조합 승률 차이는 거의 다 개인 브롤러의 맵 효과이고,
--     이벤트별 칸은 표본이 얇아 예측에 보탬이 없다.
--   - 결정 3: 3인 조합(3v3 한 팀 전체)을 전 티어로 더한다. 이관할 데이터는 없고, 첫 집계가 원본(보존 10일)에서 시작한다(⑤).
--   - 결정 6: 보존 정책. 세 테이블을 start_battle_date 월 범위 파티션({table}_p_YYYYMM, default 파티션 없음)으로 만들고
--     partitionManageJob 이 지난달 1일 이전 파티션을 지운다. API 최대 범위 SINCE_LAST_MONTH 와 같은 경계다.
--   - 결정 7: 기존 2인·카운터 행을 전 티어 이관한다. 범위는 보존 정책과 같은 지난달 1일 이후 버킷이다.
--   - 쇼다운 조합 순위(brawler_pair_battle_rank_stats)는 결정 2 로 기능을 없애므로 이관 없이 지운다.
--
-- 이관 근사(결정 7):
--   - 2인: legacy 는 기준 브롤러 관점의 방향성 행((X,Y)·(Y,X))이다. 이벤트를 합산한 뒤 두 방향 중 total_count 합이 작은 쪽을
--     택한다(같으면 brawler < pair 쪽).
--     기본 티어(TROPHY_1000_PLUS, SOLO_RANK_MYTHIC_PLUS)는 수집 단계가 전투 전원 1000+(경쟁전 신화+)를 요구해 두 방향 값이 같아 정확하다.
--     상위 티어는 "기준 플레이어만 티어 이상" 행이라 새 정의("전원 티어 이상")의 상한이고, 둘 중 작은 쪽이 가장 좁은 상한이다.
--     보존 2개월 안에 소멸한다.
--   - 카운터: 이벤트만 합산한다. 방향성 행(기준 플레이어 트로피)의 뜻이 그대로라 정확하다.
--   - 과거 행에 섞인 허용 목록 밖 모드(스펙 A 결정 9)도 합산 뒤에는 분리할 수 없다. 같은 기간(2개월) 안에 소멸한다.
--
-- 배포 순서(스펙 §3.8):
--   ① DDL 1차: 이 파일(V261104a)을 끝까지(2차 트랜잭션 커밋과 analyze) 적용한다.
--      1차 트랜잭션이 커밋되는 순간부터 구버전 core-api·core-batch 가 조합·카운터·쇼다운 조합 순위에서 실패한다(아래 "영향 범위").
--   ② core-api·core-batch 배포(Deploy 워크플로)를 ① 이 끝나자마자 이어서 한다. ② 를 ① 이 끝나기 전에 하지 않는다.
--      2차 트랜잭션 커밋 전에 새 core-api 가 뜨면 아직 빈 새 테이블을 읽어 빈 조합·카운터 응답을 Redis 에 캐시한다.
--      1차 커밋 ~ ② 사이가 조합·카운터·쇼다운 조합 순위 응답과 그 잡들만 끊기는 구간이다(2차 트랜잭션 시간 + 배포 시간).
--   ③ imstargg-deploy: core-batch CronJob trio-battle-result-stats 추가, pair-battle-rank-stats 삭제.
--   ④ 프론트(Amplify) 배포. core-api 뒤에 올린다.
--   ⑤ 3인 잡(brawlerTrioBattleResultStatsJob) 수동 실행 — date 를 바꿔 두 번: date = KST 전일(이번 주), date = 지난주 일요일(지난주).
--      잡 한 번은 StatsWeekRange.currentAndPrevious(date) 버킷(전날이 같은 주면 하나)만 채워 SINCE_LAST_WEEK·LAST_WEEK 의 지난주가 빈다.
--      지난주 월요일 ~ KST 전일 사이에 월 1일이 있으면 date = 그 1일도 돌린다(1일 버킷과 말일 버킷을 함께 채운다). 같은 date 는 생략.
--      원본은 date - 10일 보다 이른 일 파티션이 지워져 지난주 버킷은 월~목 실행일 때만 온전하다(금~일은 앞쪽 며칠이 빠진다).
--      같은 (잡, date) 가 이미 COMPLETED 면 JobInstanceAlreadyCompleteException — 다시 돌릴 때는 rerun=1 을 더한다(스펙 §3.8 ⑤).
--   ⑥ 배포 후 확인: 새 테이블 버킷·행 수, 기본 티어에서 _legacy 방향 합계와 새 행 수 일치(파일 끝 검증 쿼리), 3인 잡 성공,
--      trio API(/v1/statistics/trio/result, /v1/statistics/brawler/trio/result) 응답, 화면, Redis 에 새 키 버전만 생기는지,
--      다음 날 02:00 KST partitionManageJob 의 월 파티션 스텝 성공.
--   ⑦ DDL 2차: V261104b(⑥ 확인 뒤). _legacy 두 테이블을 지운다.
--
-- 영향 범위(1차 트랜잭션 커밋 ~ ② 배포):
--   - 구 core-api 의 /v1/statistics/brawler/pair/result, /v1/statistics/brawler/enemy/result, /v1/statistics/brawler/pair/rank 가
--     캐시 미스에서 500 을 준다(새 테이블에는 event 컬럼이 없고 pair rank 테이블은 지워졌다). 기다리지 않고 바로 실패한다.
--   - 구 core-batch 의 CronJob enemy-battle-result-stats(04:00 KST)·pair-battle-rank-stats(06:00 KST, ③ 전까지)·
--     pair-battle-result-stats(07:00 KST)는 그 사이에 돌면 실패한다.
--   - 다른 엔드포인트와 페이지는 영향이 없다.
--   - 그래서 트래픽이 가장 적은 시간대, 그리고 통계 배치 시간대(02:00~09:00 KST) 밖에서 적용한다.
--
-- 적용: psql -v ON_ERROR_STOP=1 -f 로 파일 전체를 적용한다. lock_timeout 3s(세션 설정, 두 트랜잭션 모두에 걸린다).
--   (문장 하나짜리인 기존 마이그레이션과 달리 단계가 여럿이라 트랜잭션 두 개로 묶는다.)
--   - 1차 트랜잭션: 이름 변경·pair rank 삭제·새 시퀀스·테이블·인덱스·파티션. ACCESS EXCLUSIVE 를 몇 초만 잡는다.
--     락을 못 잡아 실패하면(lock_timeout) 아무것도 바뀌지 않으니 배치 시간대를 피해 다시 시도한다.
--   - 2차 트랜잭션: 이관 insert 두 개. _legacy 에는 ACCESS SHARE, 새 테이블에는 ROW EXCLUSIVE 만 잡아 기다리는 쪽이 없다.
--     start_battle_date 로 시작하는 인덱스가 없어 legacy 전체를 읽는다(2026-11-05 운영: 2인 약 4.7GB·1,415만 행,
--     카운터 약 6.0GB·1,677만 행). 이 트랜잭션 시간이 곧 ① 의 대부분이다.
--   - 2차 커밋 뒤 analyze: 새 core-api 의 첫 조회가 통계 없는 파티션을 만나지 않게 두 이관 테이블의 통계를 만든다.
--
-- 사전 점검·측정(배포 전, 한가한 시간에. 아래 SELECT 는 모두 ACCESS SHARE 만 잡는다. 이름 변경 전이라 원래 테이블 이름을 쓴다):
--   - 미러 행 점검: 0 이어야 한다. 있으면 2차 트랜잭션 끝에서 새 카운터 테이블의 CHECK(brawler <> enemy)에 걸려 2차가 실패한다.
--       select count(*) from brawler_enemy_battle_result_stats where brawler_brawlstars_id = enemy_brawler_brawlstars_id and start_battle_date >= date_trunc('month', current_date) - interval '1 month';
--   - 소요 시간 측정: 이관 insert 의 SELECT 부분만 돌려(psql \timing on) 2차 트랜잭션 시간을 가늠하고 캐시를 데운다.
--     결과 count 는 2차 트랜잭션의 INSERT 행 수와 거의 같아야 한다(그 사이 잡이 다시 쓴 버킷만큼 차이).
--       select count(*) from (
--         select distinct on (tier_range, start_battle_date, end_battle_date, b1, b2)
--                tier_range, start_battle_date, end_battle_date, b1, b2, v, d, dr, t
--         from (
--           select tier_range, start_battle_date, end_battle_date,
--                  least(brawler_brawlstars_id, pair_brawler_brawlstars_id) b1,
--                  greatest(brawler_brawlstars_id, pair_brawler_brawlstars_id) b2,
--                  (brawler_brawlstars_id < pair_brawler_brawlstars_id) forward,
--                  sum(victory_count) v, sum(defeat_count) d, sum(draw_count) dr, sum(total_count) t
--           from brawler_pair_battle_result_stats
--           where start_battle_date >= (date_trunc('month', current_date) - interval '1 month')::date
--           group by 1, 2, 3, 4, 5, 6
--         ) g
--         order by tier_range, start_battle_date, end_battle_date, b1, b2, t asc, forward desc
--       ) s;
--       select count(*) from (
--         select tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id,
--                sum(victory_count), sum(defeat_count), sum(draw_count), sum(total_count)
--         from brawler_enemy_battle_result_stats
--         where start_battle_date >= (date_trunc('month', current_date) - interval '1 month')::date
--         group by tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id
--       ) s;
--
-- 중단: 어느 시점이든 psql 세션을 취소(Ctrl+C·종료)하면 진행 중인 트랜잭션만 되돌아간다.
--   1차가 이미 커밋됐으면 (새 테이블은 빈 채로) 나중에 이 파일 전체를 다시 적용한다. 아래 멱등 규칙으로 이어서 끝난다.
--   그동안은 "영향 범위"의 상태이므로 ② 를 미루고 되도록 빨리 다시 적용한다.
--
-- 멱등: 같은 상태에서 다시 적용해도 바뀌는 것이 없다. 1차만 커밋된 상태에서 파일 전체를 다시 적용해도 된다.
--   - 이름 변경은 _legacy 가 없고, 원래 이름의 테이블이 있고, 그 테이블에 event_brawlstars_id 컬럼이 있을 때(아직 legacy 모양일 때)만 한다.
--   - 시퀀스·테이블·인덱스·파티션은 if not exists, 삭제는 if exists.
--   - 이관 insert 는 on conflict do nothing 이라 다시 돌리면 이미 있는 행은 건너뛴다(시퀀스 값만 앞으로 간다).
--     유니크 인덱스에 파티션 키(start_battle_date)가 들어 있어 파티션 테이블에서도 동작한다.
--     다만 다시 적용해도 legacy 를 다시 전체 읽으므로 필요할 때만 적용한다.
--   - ② 뒤 재적용 금지: 새 잡이 한 번이라도 돈 뒤에는 이 파일을 다시 적용하지 않는다. 잡이 다시 쓴 버킷에서 새 집계(전원 티어)에
--     없는 조합이 legacy 에서 되살아난다(on conflict do nothing 은 잡이 쓴 행만 지킨다).
--   - V261104b 적용 뒤에는 _legacy 가 없어 2차 트랜잭션의 이관 insert 가 실패한다(의도). 그 뒤에도 다시 적용하지 않는다.
--
-- 재실행 금지: 지난달 1일 ~ 배포일-7일 사이 date 로 brawlerPairBattleResultStatsJob·brawlerEnemyBattleResultStatsJob 을
--   수동 재실행하지 않는다 — 이관 버킷을 원본 10일치로 덮어쓴다.
--
-- 롤백: _legacy 를 지우기 전(V261104b 전)까지 가능하다.
--   새 테이블을 _v2 로 치우고(인덱스·시퀀스 이름도) _legacy 를 원래 이름으로 되돌린 뒤(인덱스·시퀀스 이름도) 구 이미지를 배포한다.
--   전환 기간 데이터는 _legacy 에 없지만 구버전 일일 잡이 현재·직전 주를 다시 집계해 하루 안에 최대 2주치가 메워진다.
--   3인 조합·전체 정렬·3인 CronJob 은 사라지고, 캐시는 키 버전이 달라 섞이지 않는다.
--   brawler_pair_battle_rank_stats 는 이 파일에서 지워지므로 롤백 뒤 구 이미지의 쇼다운 조합 순위 응답·잡은 테이블 없음으로 실패한다.
--   _v2 로 치워도 파티션({t}_p_YYYYMM)·위치 인덱스(ix_…__brawlerN) 이름은 그대로 남는다. 롤백 뒤 V261104a 를 다시 적용하려면
--   먼저 _v2 테이블을 지운다(파티션·인덱스·시퀀스가 함께 떨어진다). 그대로 두면 if not exists 가 그 이름들을 보고 건너뛰어
--   새 테이블이 파티션 없이 만들어진다.
--   V261104b(2차 DDL) 뒤에는 되돌릴 수 없다.
--   롤백 SQL(실행문이 아니라 배포 담당자가 고르는 절차다. 한 트랜잭션으로 돌린다):
--     begin;
--     alter table brawler_pair_battle_result_stats rename to brawler_pair_battle_result_stats_v2;
--     alter index brawler_pair_battle_result_stats_pkey rename to brawler_pair_battle_result_stats_v2_pkey;
--     alter index uk_brawler_pair_battle_result_stats__1 rename to uk_brawler_pair_battle_result_stats_v2__1;
--     alter sequence brawler_pair_battle_result_stats_seq rename to brawler_pair_battle_result_stats_seq_v2;
--     alter table brawler_enemy_battle_result_stats rename to brawler_enemy_battle_result_stats_v2;
--     alter index brawler_enemy_battle_result_stats_pkey rename to brawler_enemy_battle_result_stats_v2_pkey;
--     alter index uk_brawler_enemy_battle_result_stats__1 rename to uk_brawler_enemy_battle_result_stats_v2__1;
--     alter sequence brawler_enemy_battle_result_stats_seq rename to brawler_enemy_battle_result_stats_seq_v2;
--     alter table brawler_pair_battle_result_stats_legacy rename to brawler_pair_battle_result_stats;
--     alter index brawler_pair_battle_result_stats_legacy_pkey rename to brawler_pair_battle_result_stats_pkey;
--     alter index uk_brawler_pair_battle_result_stats_legacy__1 rename to uk_brawler_pair_battle_result_stats__1;
--     alter index ix_brawler_pair_battle_result_stats_legacy_2 rename to ix_brawler_pair_battle_result_stats_2;
--     alter sequence brawler_pair_battle_result_stats_seq_legacy rename to brawler_pair_battle_result_stats_seq;
--     alter table brawler_enemy_battle_result_stats_legacy rename to brawler_enemy_battle_result_stats;
--     alter index brawler_enemy_battle_result_stats_legacy_pkey rename to brawler_enemy_battle_result_stats_pkey;
--     alter index uk_brawler_enemy_battle_result_stats_legacy__1 rename to uk_brawler_enemy_battle_result_stats__1;
--     alter index ix_brawler_enemy_battle_result_stats_legacy_2 rename to ix_brawler_enemy_battle_result_stats_2;
--     alter index if exists ix_brawler_enemy_battle_result_stats_legacy_1 rename to ix_brawler_enemy_battle_result_stats_1;
--     alter sequence brawler_enemy_battle_result_stats_seq_legacy rename to brawler_enemy_battle_result_stats_seq;
--     commit;
--     -- 롤백 뒤 V261104a 를 다시 적용하기 전에만:
--     --   drop table if exists brawler_pair_battle_result_stats_v2;
--     --   drop table if exists brawler_enemy_battle_result_stats_v2;
--
-- 참고: PG18 은 NOT NULL 제약에도 이름을 붙이고 같은 스키마에서 겹치지 않게 고른다. _legacy 가 함께 있는 동안 만드는 새 2인·카운터
--   테이블의 NOT NULL 제약은 …_not_null1 처럼 끝에 1 이 붙고, V261104b 뒤에도 그대로 남는다(앱은 이 이름을 쓰지 않는다).

set lock_timeout = '3s';

-- =============================================================
-- 1차 트랜잭션: 이름 변경·pair rank 삭제·새 테이블·파티션 (ACCESS EXCLUSIVE 는 몇 초)
-- =============================================================
begin;

-- -------------------------------------------------------------
-- 1. 기존 2인 조합·카운터 테이블을 _legacy 로 옮긴다(이관 소스, V261104b 에서 지운다).
--    인덱스 이름은 테이블 이름을 따라 바뀌지 않으므로 함께 바꾼다. 특히 PK 인덱스(…_pkey)와 유니크(uk_…__1)는
--    새 테이블의 이름과 충돌한다. PK 인덱스 이름을 바꾸면 PK 제약 이름도 함께 바뀐다.
--    시퀀스 이름을 바꿔도 legacy id 컬럼 기본값(nextval(regclass))과 owned by 는 oid 로 이어진다.
--    운영의 카운터 테이블에는 schema.sql 에 없는 ix_brawler_enemy_battle_result_stats_1 (event, tier, start, end) 이 있어
--    if exists 로 함께 바꾼다.
--    CHECK ck_brawler_pair_battle_result_stats_distinct_brawler 는 그대로 둔다(제약 이름은 테이블마다 따로이고
--    새 테이블은 ck_brawler_pair_battle_result_stats_ordered 를 쓴다).
-- -------------------------------------------------------------
do $$
begin
    if to_regclass('brawler_pair_battle_result_stats_legacy') is null
        and to_regclass('brawler_pair_battle_result_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_pair_battle_result_stats'
              and column_name = 'event_brawlstars_id'
        ) then
        alter table brawler_pair_battle_result_stats rename to brawler_pair_battle_result_stats_legacy;
        alter index uk_brawler_pair_battle_result_stats__1 rename to uk_brawler_pair_battle_result_stats_legacy__1;
        alter index ix_brawler_pair_battle_result_stats_2 rename to ix_brawler_pair_battle_result_stats_legacy_2;
        alter index brawler_pair_battle_result_stats_pkey rename to brawler_pair_battle_result_stats_legacy_pkey;
        alter sequence brawler_pair_battle_result_stats_seq rename to brawler_pair_battle_result_stats_seq_legacy;
    end if;

    if to_regclass('brawler_enemy_battle_result_stats_legacy') is null
        and to_regclass('brawler_enemy_battle_result_stats') is not null
        and exists (
            select 1
            from information_schema.columns
            where table_schema = current_schema()
              and table_name = 'brawler_enemy_battle_result_stats'
              and column_name = 'event_brawlstars_id'
        ) then
        alter table brawler_enemy_battle_result_stats rename to brawler_enemy_battle_result_stats_legacy;
        alter index uk_brawler_enemy_battle_result_stats__1 rename to uk_brawler_enemy_battle_result_stats_legacy__1;
        alter index ix_brawler_enemy_battle_result_stats_2 rename to ix_brawler_enemy_battle_result_stats_legacy_2;
        alter index if exists ix_brawler_enemy_battle_result_stats_1 rename to ix_brawler_enemy_battle_result_stats_legacy_1;
        alter index brawler_enemy_battle_result_stats_pkey rename to brawler_enemy_battle_result_stats_legacy_pkey;
        alter sequence brawler_enemy_battle_result_stats_seq rename to brawler_enemy_battle_result_stats_seq_legacy;
    end if;
end $$;

-- -------------------------------------------------------------
-- 2. 쇼다운 조합 순위를 지운다(결정 2). 시퀀스는 owned by 로 함께 떨어지지만 남은 경우를 위해 한 번 더 지운다.
-- -------------------------------------------------------------
drop table if exists brawler_pair_battle_rank_stats;
drop sequence if exists brawler_pair_battle_rank_stats_seq;

-- -------------------------------------------------------------
-- 3. 새 테이블 3개(schema.sql 의 정의와 같다)와 월 파티션(지난달·이번 달·다음 달).
-- -------------------------------------------------------------

-- 브롤러 vs 적 브롤러(카운터) 전투 결과 통계 (파티셔닝: start_battle_date 월 범위)
create sequence if not exists brawler_enemy_battle_result_stats_seq increment by 100;
create table if not exists brawler_enemy_battle_result_stats
(
    brawler_enemy_battle_result_stats_id bigint       not null default nextval('brawler_enemy_battle_result_stats_seq'),
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
    constraint ck_brawler_enemy_battle_result_stats_distinct_brawler
        check (brawler_brawlstars_id <> enemy_brawler_brawlstars_id)
) partition by range (start_battle_date);
create unique index if not exists uk_brawler_enemy_battle_result_stats__1
    on brawler_enemy_battle_result_stats (
        tier_range, brawler_brawlstars_id, start_battle_date, enemy_brawler_brawlstars_id
    );
alter sequence brawler_enemy_battle_result_stats_seq owned by brawler_enemy_battle_result_stats.brawler_enemy_battle_result_stats_id;

-- 브롤러 pair(같은 팀 2인 조합) 전투 결과 통계 (파티셔닝: start_battle_date 월 범위)
create sequence if not exists brawler_pair_battle_result_stats_seq increment by 100;
create table if not exists brawler_pair_battle_result_stats
(
    brawler_pair_battle_result_stats_id bigint      not null default nextval('brawler_pair_battle_result_stats_seq'),
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
    constraint ck_brawler_pair_battle_result_stats_ordered
        check (brawler1_brawlstars_id < brawler2_brawlstars_id)
) partition by range (start_battle_date);
create unique index if not exists uk_brawler_pair_battle_result_stats__1
    on brawler_pair_battle_result_stats (
        tier_range, start_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id
    );
create index if not exists ix_brawler_pair_battle_result_stats__brawler1
    on brawler_pair_battle_result_stats (tier_range, brawler1_brawlstars_id, start_battle_date);
create index if not exists ix_brawler_pair_battle_result_stats__brawler2
    on brawler_pair_battle_result_stats (tier_range, brawler2_brawlstars_id, start_battle_date);
alter sequence brawler_pair_battle_result_stats_seq owned by brawler_pair_battle_result_stats.brawler_pair_battle_result_stats_id;

-- 브롤러 trio(같은 팀 3인 조합 = 3v3 한 팀 전체) 전투 결과 통계 (파티셔닝: start_battle_date 월 범위)
create sequence if not exists brawler_trio_battle_result_stats_seq increment by 100;
create table if not exists brawler_trio_battle_result_stats
(
    brawler_trio_battle_result_stats_id bigint      not null default nextval('brawler_trio_battle_result_stats_seq'),
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
    constraint ck_brawler_trio_battle_result_stats_ordered
        check (brawler1_brawlstars_id < brawler2_brawlstars_id and brawler2_brawlstars_id < brawler3_brawlstars_id)
) partition by range (start_battle_date);
create unique index if not exists uk_brawler_trio_battle_result_stats__1
    on brawler_trio_battle_result_stats (
        tier_range, start_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id, brawler3_brawlstars_id
    );
create index if not exists ix_brawler_trio_battle_result_stats__brawler1
    on brawler_trio_battle_result_stats (tier_range, brawler1_brawlstars_id, start_battle_date);
create index if not exists ix_brawler_trio_battle_result_stats__brawler2
    on brawler_trio_battle_result_stats (tier_range, brawler2_brawlstars_id, start_battle_date);
create index if not exists ix_brawler_trio_battle_result_stats__brawler3
    on brawler_trio_battle_result_stats (tier_range, brawler3_brawlstars_id, start_battle_date);
alter sequence brawler_trio_battle_result_stats_seq owned by brawler_trio_battle_result_stats.brawler_trio_battle_result_stats_id;

-- 월 파티션 {table}_p_YYYYMM: 지난달·이번 달·다음 달. 이후 달은 partitionManageJob(create_month_partitions)이 만든다.
-- 이관 범위(지난달 1일 이후)의 버킷은 모두 이 세 파티션 안에 든다(주 버킷은 월 경계에서 잘린다).
do $$
declare
    t text;
    m date;
begin
    foreach t in array array[
        'brawler_enemy_battle_result_stats',
        'brawler_pair_battle_result_stats',
        'brawler_trio_battle_result_stats'
    ] loop
        for i in -1..1 loop
            m := (date_trunc('month', current_date) + make_interval(months => i))::date;
            execute format(
                'create table if not exists %I partition of %I for values from (%L) to (%L)',
                t || '_p_' || to_char(m, 'YYYYMM'), t, m, (m + interval '1 month')::date
            );
        end loop;
    end loop;
end $$;

commit;

-- =============================================================
-- 2차 트랜잭션: 이관 (_legacy 는 ACCESS SHARE, 새 테이블은 ROW EXCLUSIVE 만)
-- =============================================================
begin;

-- -------------------------------------------------------------
-- 4. 이관(지난달 1일 이후 버킷). 그 이전 legacy 행은 옮기지 않는다(보존 정책과 같은 경계).
-- -------------------------------------------------------------

-- 2인: 이벤트를 합산한 뒤 두 방향 중 total_count 합이 작은 쪽, 같으면 brawler < pair(forward) 쪽을 택한다.
insert into brawler_pair_battle_result_stats
  (tier_range, start_battle_date, end_battle_date, brawler1_brawlstars_id, brawler2_brawlstars_id,
   victory_count, defeat_count, draw_count, total_count)
select distinct on (tier_range, start_battle_date, end_battle_date, b1, b2)
       tier_range, start_battle_date, end_battle_date, b1, b2, v, d, dr, t
from (
  select tier_range, start_battle_date, end_battle_date,
         least(brawler_brawlstars_id, pair_brawler_brawlstars_id) b1,
         greatest(brawler_brawlstars_id, pair_brawler_brawlstars_id) b2,
         (brawler_brawlstars_id < pair_brawler_brawlstars_id) forward,
         sum(victory_count) v, sum(defeat_count) d, sum(draw_count) dr, sum(total_count) t
  from brawler_pair_battle_result_stats_legacy
  where start_battle_date >= (date_trunc('month', current_date) - interval '1 month')::date
  group by 1, 2, 3, 4, 5, 6
) g
order by tier_range, start_battle_date, end_battle_date, b1, b2, t asc, forward desc
on conflict do nothing;

-- 카운터: 이벤트만 합산한다.
insert into brawler_enemy_battle_result_stats
  (tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id,
   victory_count, defeat_count, draw_count, total_count)
select tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id,
       sum(victory_count), sum(defeat_count), sum(draw_count), sum(total_count)
from brawler_enemy_battle_result_stats_legacy
where start_battle_date >= (date_trunc('month', current_date) - interval '1 month')::date
group by tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id
on conflict do nothing;

commit;

-- 이관한 두 테이블의 통계를 만든다(파티션 테이블이면 부모와 각 파티션이 함께 갱신된다).
analyze brawler_pair_battle_result_stats;
analyze brawler_enemy_battle_result_stats;

-- -------------------------------------------------------------
-- 5. 검증(적용 뒤 수동 실행, V261104b 전).
--    적용한 달 안에 실행한다(범위 경계가 current_date 기준이라 달이 바뀌면 범위가 바뀐다).
--    새 이미지의 잡(카운터 04:00, 2인 07:00 KST)이 한 번이라도 돌면 현재 주 버킷(그 사이 월요일·월 1일이 지났으면 직전 버킷도)을
--    원본에서 다시 집계해 덮어쓴다(이벤트 없음·전원 티어). 그 버킷은 legacy 와 달라지는 것이 정상이므로
--    버킷별로 비교하고, 잡이 다시 쓴 버킷은 빼고 본다(적용 직후 첫 잡 전에 보면 모든 버킷이 같아야 한다).
-- -------------------------------------------------------------
-- (a) 2인 — 기본 티어, 버킷별: legacy 의 brawler < pair 방향 합산 행 수·Σ total 과 새 테이블 행 수·Σ total 이 같아야 한다.
--   select tier_range, start_battle_date, count(*) as row_count, sum(t) as total
--   from (
--     select tier_range, start_battle_date, sum(total_count) t
--     from brawler_pair_battle_result_stats_legacy
--     where start_battle_date >= (date_trunc('month', current_date) - interval '1 month')::date
--       and tier_range in ('TROPHY_1000_PLUS', 'SOLO_RANK_MYTHIC_PLUS')
--       and brawler_brawlstars_id < pair_brawler_brawlstars_id
--     group by tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, pair_brawler_brawlstars_id
--   ) g
--   group by tier_range, start_battle_date
--   order by tier_range, start_battle_date;
--   select tier_range, start_battle_date, count(*) as row_count, sum(total_count) as total
--   from brawler_pair_battle_result_stats
--   where tier_range in ('TROPHY_1000_PLUS', 'SOLO_RANK_MYTHIC_PLUS')
--   group by tier_range, start_battle_date
--   order by tier_range, start_battle_date;
--
-- (b) 카운터 — 전 티어, 버킷별: legacy 합산 행 수·Σ total 과 새 테이블 행 수·Σ total 이 같아야 한다.
--   select start_battle_date, count(*) as row_count, sum(t) as total
--   from (
--     select start_battle_date, sum(total_count) t
--     from brawler_enemy_battle_result_stats_legacy
--     where start_battle_date >= (date_trunc('month', current_date) - interval '1 month')::date
--     group by tier_range, start_battle_date, end_battle_date, brawler_brawlstars_id, enemy_brawler_brawlstars_id
--   ) g
--   group by start_battle_date
--   order by start_battle_date;
--   select start_battle_date, count(*) as row_count, sum(total_count) as total
--   from brawler_enemy_battle_result_stats
--   group by start_battle_date
--   order by start_battle_date;
--
-- (c) 파티션: 세 테이블 각각 지난달·이번 달·다음 달 3개.
--   select p.relname as parent, c.relname as partition, pg_get_expr(c.relpartbound, c.oid) as bound
--   from pg_inherits i
--   join pg_class c on c.oid = i.inhrelid
--   join pg_class p on p.oid = i.inhparent
--   where p.relname in ('brawler_pair_battle_result_stats', 'brawler_trio_battle_result_stats', 'brawler_enemy_battle_result_stats')
--   order by 1, 2;
