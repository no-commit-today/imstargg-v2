-- 브롤스타즈 자원 테이블에 soft-delete 컬럼(deleted_at)을 추가한다.
-- 대상: brawler, brawler_gear, gear, gadget, star_power, hyper_charge, battle_event
--
-- 배경: 게임에서 제거된 기어를 brawler_gear 에서 행 삭제했지만 브롤스타즈 API 는 계속 응답해,
--   brawlerSyncJob 이 매번 "기어 추가"로 감지·알림하고 있었다.
--   행을 남기고 deleted_at 으로 삭제를 표시하면 동기화 배치가 "이미 알고 있는 항목"으로 취급한다.
--
-- 배포 순서: DDL 선적용 → 앱 배포.
--   nullable 컬럼 추가라 구버전 앱이 새 스키마를 읽어도 안전하다 (반대 순서는 신버전 앱이 컬럼 부재로 실패).
--   소규모 테이블(수백 행 이하)이고 default 없는 nullable 컬럼 추가는 메타데이터 변경뿐이라 테이블 재작성이 없다.
--
-- 멱등: add column if not exists, insert ... on conflict do nothing.

alter table brawler add column if not exists deleted_at timestamptz null;
alter table brawler_gear add column if not exists deleted_at timestamptz null;
alter table gear add column if not exists deleted_at timestamptz null;
alter table gadget add column if not exists deleted_at timestamptz null;
alter table star_power add column if not exists deleted_at timestamptz null;
alter table hyper_charge add column if not exists deleted_at timestamptz null;
alter table battle_event add column if not exists deleted_at timestamptz null;

-- 게임에서 제거됐지만 API 가 계속 응답하는 브롤러별 기어를 삭제 상태로 복원한다.
-- 삭제일시는 최종 업데이트일시와 같게 둔다 (now() 는 트랜잭션 시작 시각이라 세 컬럼이 동일).
insert into brawler_gear (brawler_brawlstars_id, gear_brawlstars_id, created_at, updated_at, deleted_at)
values (16000003, 62000005, now(), now(), now()), -- BROCK   RELOAD SPEED
       (16000004, 62000005, now(), now(), now()), -- RICO    RELOAD SPEED
       (16000005, 62000010, now(), now(), now()), -- SPIKE   STICKY SPIKES
       (16000014, 62000005, now(), now(), now()), -- BO      RELOAD SPEED
       (16000023, 62000011, now(), now(), now()), -- LEON    LINGERING SMOKE
       (16000040, 62000005, now(), now(), now()), -- AMBER   RELOAD SPEED
       (16000040, 62000013, now(), now(), now()), -- AMBER   STICKY OIL
       (16000050, 62000005, now(), now(), now())  -- GRIFF   RELOAD SPEED
on conflict (brawler_brawlstars_id, gear_brawlstars_id) do nothing;
