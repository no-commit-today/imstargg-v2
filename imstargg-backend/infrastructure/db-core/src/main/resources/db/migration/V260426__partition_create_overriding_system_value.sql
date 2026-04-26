-- create_partitions 함수 재정의
-- stats_rank_battle / stats_result_battle 의 ID 컬럼이 GENERATED ALWAYS AS IDENTITY 라
-- default 파티션에서 새 파티션으로 데이터 이동 시 OVERRIDING SYSTEM VALUE 가 필요.
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

            IF NOT EXISTS (
                SELECT 1 FROM pg_class c
                JOIN pg_inherits i ON c.oid = i.inhrelid
                WHERE c.relname = part_name
            ) THEN
                EXECUTE format(
                    'ALTER TABLE %I DETACH PARTITION %I',
                    tbl_name, tbl_name || '_default'
                );
                EXECUTE format(
                    'CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                    part_name, tbl_name, target_date, bound_date
                );
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
