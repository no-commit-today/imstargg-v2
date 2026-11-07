-- 통계 수집 플레이어에 상태 컬럼(status)을 추가한다.
-- 대상: stats_player
--
-- 배경: 브롤스타즈 API battlelog 는 /players 가 살아 있는 계정에서도 404 를 준다.
--   통계 축적이 이 404 를 계정 삭제로 보고 stats_player 행을 지우면, 동기화 배치가 같은 태그를 다시 넣어
--   재등록·재삭제가 반복된다. 행을 지우지 않고 BATTLE_LOG_UNAVAILABLE 로 표시해 무작위 추출에서만 뺀다.
--
-- 배포 순서: DDL 선적용 → 앱 배포.
--   구버전 앱은 컬럼을 무시한다 — insert 에 status 를 넣지 않아 DB 기본값 'ACTIVE' 가 들어간다
--   (반대 순서는 신버전 앱이 컬럼 부재로 실패).
--   상수 기본값의 not null 컬럼 추가라 Postgres 11+ 에서는 메타데이터 변경뿐이고 테이블 재작성이 없다(행 약 555만 개).
--   메타데이터 변경이지만 잠깐 ACCESS EXCLUSIVE 락을 잡는다.
--
-- 적용: lock_timeout 3s 로 적용한다. 락을 못 잡아 실패하면 통계 배치 시간대(02:00~09:00 KST)를 피해 다시 시도한다.
--
-- 멱등: add column if not exists.

set lock_timeout = '3s';

alter table stats_player add column if not exists status varchar(25) not null default 'ACTIVE';
