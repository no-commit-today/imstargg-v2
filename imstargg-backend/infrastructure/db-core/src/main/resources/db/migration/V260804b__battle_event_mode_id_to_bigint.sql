-- battle.event_mode_id 를 int → bigint 로 넓혀 다른 모드 ID 컬럼과 타입을 통일한다.
-- 같은 값을 담는 다른 컬럼은 모두 bigint 다:
--   battle_event_mode.brawlstars_id, battle_event_rotation_item.event_mode_brawlstars_id,
--   stats_rank_battle.event_mode_brawlstars_id, stats_result_battle.event_mode_brawlstars_id
-- 이 컬럼만 int 라서 core-worker 가 StatsBattleAggregateApplier 에서 .toLong() 으로
-- 되돌리고 있었다. 앱 코드도 BattleEntityEvent.modeBrawlStarsId 를 Long? 으로 바꾼다.
--
-- ⚠️ 주의 — 실행 비용:
--   PostgreSQL 에서 int → bigint 는 저장 폭이 달라 **전체 테이블 재작성**이 필요하고,
--   그동안 battle 테이블에 ACCESS EXCLUSIVE 락이 걸린다 (읽기·쓰기 모두 차단).
--   battle 은 이 스키마에서 가장 큰 테이블이므로 반드시 점검 시간대에 적용한다.
--   적용 전 소요 시간 가늠: select count(*) from battle;
--   무중단이 필요하면 이 방식 대신 신규 bigint 컬럼 추가 → 백필 → 스왑 절차를 쓴다.
--
-- 배포 순서: DDL 선적용 → 앱 배포.
--   bigint 컬럼을 구버전 앱(Int 매핑)이 읽어도 실제 값이 int 범위 안이라 안전하다.
--
-- 멱등: 이미 bigint 면 아무것도 하지 않는다 (락도 잡지 않음).

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'battle'
          and column_name = 'event_mode_id'
          and data_type = 'integer'
    ) then
        alter table battle alter column event_mode_id type bigint;
    end if;
end $$;
