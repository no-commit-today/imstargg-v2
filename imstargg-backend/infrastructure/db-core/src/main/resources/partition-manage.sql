-- =============================================================
-- 파티션 관리 함수 (PostgreSQL)
-- =============================================================
-- 사용법:
--   SELECT create_partitions('2025-01-24', 7);  -- 2025-01-24부터 7일간 파티션 생성
--   SELECT drop_old_partitions('2025-01-24', 7); -- 2025-01-24 기준 7일 초과 파티션 삭제
--   SELECT create_month_partitions('2026-11-01', 1); -- 조합 통계 3종의 2026-11 ~ 2026-12 월 파티션 생성
--   SELECT drop_old_month_partitions('2026-11-01');  -- 2026-11-01 기준 2026-10 이전(= 2026-09 까지) 월 파티션 삭제
-- =============================================================

-- -------------------------------------------------------------
-- 파티션 생성 함수
-- base_date부터 days_ahead일 뒤까지 일별 파티션을 미리 생성
-- 이미 존재하는 파티션은 건너뜀
-- 파티션명: {table}_p_YYYYMMDD (해당 날짜 데이터 포함)
-- -------------------------------------------------------------
DROP FUNCTION IF EXISTS create_partitions(date, int);

CREATE OR REPLACE FUNCTION create_partitions(base_date date, days_ahead int)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    i int := 0;
    target_date date;
    bound_date date;
    part_name text;
    tbl_name text;
    tbl_names text[] := ARRAY['stats_rank_battle', 'stats_result_battle'];
BEGIN
    WHILE i <= days_ahead LOOP
        target_date := base_date + i;
        bound_date := target_date + 1;
        FOREACH tbl_name IN ARRAY tbl_names LOOP
            part_name := tbl_name || '_p_' || to_char(target_date, 'YYYYMMDD');

            -- 파티션이 이미 존재하는지 확인
            IF NOT EXISTS (
                SELECT 1 FROM pg_class c
                JOIN pg_inherits i ON c.oid = i.inhrelid
                WHERE c.relname = part_name
            ) THEN
                -- default 파티션에서 데이터를 새 파티션으로 이동하기 위해
                -- default 파티션을 detach하고, 새 파티션을 생성한 후, default 파티션을 다시 attach
                EXECUTE format(
                    'ALTER TABLE %I DETACH PARTITION %I',
                    tbl_name, tbl_name || '_default'
                );
                EXECUTE format(
                    'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                    part_name, tbl_name, target_date, bound_date
                );
                -- default 파티션에서 새 파티션 범위에 해당하는 데이터 이동
                -- ID 컬럼이 GENERATED ALWAYS AS IDENTITY 이므로 OVERRIDING SYSTEM VALUE 로 기존 ID 보존
                EXECUTE format(
                    'INSERT INTO %I OVERRIDING SYSTEM VALUE SELECT * FROM %I WHERE battle_date >= %L AND battle_date < %L',
                    part_name, tbl_name || '_default', target_date, bound_date
                );
                EXECUTE format(
                    'DELETE FROM %I WHERE battle_date >= %L AND battle_date < %L',
                    tbl_name || '_default', target_date, bound_date
                );
                EXECUTE format(
                    'ALTER TABLE %I ATTACH PARTITION %I DEFAULT',
                    tbl_name, tbl_name || '_default'
                );
            END IF;
        END LOOP;
        i := i + 1;
    END LOOP;
END;
$$;

-- -------------------------------------------------------------
-- 오래된 파티션 삭제 함수
-- base_date 기준 retention_days일 이전 파티션 삭제
-- -------------------------------------------------------------
DROP FUNCTION IF EXISTS drop_old_partitions(date);
DROP FUNCTION IF EXISTS drop_old_partitions(date, int);

CREATE OR REPLACE FUNCTION drop_old_partitions(base_date date, retention_days int)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    cutoff_date date := base_date - retention_days;
    rec record;
BEGIN
    FOR rec IN
        SELECT c.relname AS partition_name, p.relname AS parent_name
        FROM pg_inherits i
        JOIN pg_class c ON c.oid = i.inhrelid
        JOIN pg_class p ON p.oid = i.inhparent
        WHERE p.relname IN ('stats_rank_battle', 'stats_result_battle')
          AND c.relname ~ '_p_\d{8}$'
          AND to_date(substring(c.relname from '_p_(\d{8})$'), 'YYYYMMDD') < cutoff_date
    LOOP
        EXECUTE format('ALTER TABLE %I DETACH PARTITION %I', rec.parent_name, rec.partition_name);
        EXECUTE format('DROP TABLE %I', rec.partition_name);
    END LOOP;
END;
$$;

-- =============================================================
-- 조합 통계 월 파티션 (brawler_pair/trio/enemy_battle_result_stats)
-- =============================================================
-- 위 일별 함수는 원본 배틀(stats_*_battle)을 관리하고, 아래 함수는 조합 통계 3종을 관리한다.
-- 세 테이블은 start_battle_date 기준 월 단위 range 파티션이며 default 파티션이 없다.
-- 파티션명: {table}_p_YYYYMM (6자리). 일별 파티션명은 _p_YYYYMMDD (8자리)라 두 삭제 함수의 정규식이 서로 겹치지 않는다.

-- -------------------------------------------------------------
-- 월 파티션 생성 함수
-- base_date 가 속한 달부터 months_ahead 달 뒤까지(양끝 포함) 월 파티션을 미리 생성
-- 이미 존재하는 파티션은 건너뜀(IF NOT EXISTS) — 여러 번 실행해도 안전
-- default 파티션이 없으므로 데이터 이동 절차 없이 바로 생성
-- -------------------------------------------------------------
DROP FUNCTION IF EXISTS create_month_partitions(date, int);

CREATE OR REPLACE FUNCTION create_month_partitions(base_date date, months_ahead int)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    i int := 0;
    month_start date;
    month_end date;
    tbl_name text;
    tbl_names text[] := ARRAY[
        'brawler_pair_battle_result_stats',
        'brawler_trio_battle_result_stats',
        'brawler_enemy_battle_result_stats'
    ];
BEGIN
    WHILE i <= months_ahead LOOP
        month_start := (date_trunc('month', base_date::timestamp) + make_interval(months => i))::date;
        month_end := (date_trunc('month', base_date::timestamp) + make_interval(months => i + 1))::date;
        FOREACH tbl_name IN ARRAY tbl_names LOOP
            EXECUTE format(
                'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                tbl_name || '_p_' || to_char(month_start, 'YYYYMM'), tbl_name, month_start, month_end
            );
        END LOOP;
        i := i + 1;
    END LOOP;
END;
$$;

-- -------------------------------------------------------------
-- 오래된 월 파티션 삭제 함수
-- base_date 가 속한 달의 1일에서 한 달을 뺀 날짜(지난달 1일)보다 이전 달의 파티션 삭제
--   예) 2026-11-01 실행 → 경계 2026-10-01 → _p_202609 이하 삭제, _p_202610 부터 보존
-- 이 경계는 API 최대 조회 범위(StatsDateRange.SINCE_LAST_MONTH, 지난달 1일부터)와 같다.
-- Kotlin 쪽 쌍둥이: PartitionManageJobConfig.monthRetentionCutoff (가드: MonthPartitionRetentionTest)
-- -------------------------------------------------------------
DROP FUNCTION IF EXISTS drop_old_month_partitions(date);

CREATE OR REPLACE FUNCTION drop_old_month_partitions(base_date date)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    cutoff_month date := (date_trunc('month', base_date::timestamp) - interval '1 month')::date;
    rec record;
BEGIN
    FOR rec IN
        SELECT c.relname AS partition_name, p.relname AS parent_name
        FROM pg_inherits i
        JOIN pg_class c ON c.oid = i.inhrelid
        JOIN pg_class p ON p.oid = i.inhparent
        WHERE p.relname IN (
                'brawler_pair_battle_result_stats',
                'brawler_trio_battle_result_stats',
                'brawler_enemy_battle_result_stats'
            )
          AND c.relname ~ '_p_\d{6}$'
          AND to_date(substring(c.relname from '_p_(\d{6})$'), 'YYYYMM') < cutoff_month
    LOOP
        EXECUTE format('ALTER TABLE %I DETACH PARTITION %I', rec.parent_name, rec.partition_name);
        EXECUTE format('DROP TABLE %I', rec.partition_name);
    END LOOP;
END;
$$;
