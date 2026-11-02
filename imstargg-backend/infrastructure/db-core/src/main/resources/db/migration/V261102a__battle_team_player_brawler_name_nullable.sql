-- 전투 팀 플레이어의 브롤러 이름(brawler_name)을 nullable 로 바꾼다.
-- 대상: battle_team_player
--
-- 배경: 브롤스타즈 API battlelog 가 일부 브롤러(숨김 브롤러)의 name 을 null 로 내려준다.
--   응답 DTO 가 이를 받아들이게 되면 그 전투를 저장할 때 not null 제약에 걸린다.
--   brawler_name 을 읽는 곳은 없다 (브롤러 이름은 brawler 테이블에서 가져온다).
--
-- 배포 순서: DDL 선적용 → 앱 배포.
--   not null 제약을 푸는 변경이라 구버전 앱도 그대로 동작한다 (반대 순서는 신버전 앱이 null 저장에 실패).
--   컬럼 타입을 바꾸지 않으므로 테이블 재작성이 없다. 메타데이터 변경이지만 잠깐 ACCESS EXCLUSIVE 락을 잡는다.
--
-- 적용: lock_timeout 3s 로 적용한다. 락을 못 잡아 실패하면 통계 배치 시간대(02:00~09:00 KST)를 피해 다시 시도한다.
--
-- 멱등: alter column ... drop not null 은 이미 nullable 이어도 오류 없이 끝나므로 반복 실행해도 안전하다.

set lock_timeout = '3s';

alter table battle_team_player alter column brawler_name drop not null;
