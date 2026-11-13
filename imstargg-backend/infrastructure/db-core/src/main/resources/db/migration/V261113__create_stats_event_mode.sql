-- 통계 수집 모드 테이블(stats_event_mode)을 만들고 현재 수집 모드 12개를 시드한다.
-- 대상: stats_event_mode (신규)
--
-- 배경: 통계 수집 모드를 코드 enum 대신 DB 로 관리해, 배포 없이 켜고 끈다.
--   행이 있는 모드만 수집하고, 유형(type)은 전투 형태(결과형 3v3·솔로/듀오/트리오 쇼다운)를 뜻한다.
--   설계: docs/superpowers/specs/2026-11-13-stats-event-mode-db-design.md
--
-- 배포 순서: DDL 선적용 → 앱 배포. 새 core-worker·core-api 가 이 테이블을 읽는다.
--   같은 릴리즈의 V261103(통계 컬럼 삭제)은 반대로 앱 배포·확인 뒤에 적용한다(설계 §3.9).
--
-- 영향: 새 테이블이라 구버전 앱은 이 테이블을 모르고, 영향이 없다.
--
-- 주의: 첫 적용 뒤에는 다시 돌리지 않는다. 관리자가 지운(끈) 시드 모드가 되살아난다(on conflict do nothing 은 남아 있는 행만 지킨다).
--   로컬·개발 DB 에도 적용해야 한다. 테이블이 없으면 core-worker 가 기동하지 않고 core-api 의 이벤트·로테이션·전적 응답이 실패한다.
--
-- 롤백: 테이블은 남겨도 무해하다(구버전 앱은 읽지 않는다).
--
-- 멱등: create ... if not exists, on conflict do nothing.

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
