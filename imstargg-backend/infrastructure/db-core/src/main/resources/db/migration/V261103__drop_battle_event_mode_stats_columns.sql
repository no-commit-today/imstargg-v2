-- 이벤트 모드의 통계 관련 컬럼(result_type, player_counts, color)을 없앤다.
-- 대상: battle_event_mode
--
-- 배경: 스펙 A(통계 수집 모드 허용 목록) 결정 5. 결과형/순위형은 코드 enum(StatsEventMode)이 단일 출처이고,
--   결과형 게임 수는 3v3 상수(슬롯 합 ÷ 6)로 계산하며, 카드 배경색은 프론트 상수로 옮겼다.
--   이 컬럼들을 읽고 쓰는 곳은 더 이상 없다.
--
-- 배포 순서: 앱 배포 → DDL.
--   세 컬럼 모두 DB 기본값이 있어, 앱 배포 뒤 DDL 전까지 새 코드의 INSERT 는 실패하지 않는다.
--   반대로 DDL 이 먼저면 구버전 앱의 조회가 실패한다(결정 10 으로 감수).
--   core-admin 은 Deploy 워크플로 밖에서 로컬로 띄우므로, DDL 뒤에는 새 코드로 다시 띄운다
--   (예전 체크아웃의 core-admin 을 운영 DB 에 붙이면 모드 목록·대시보드가 실패한다).
--
-- 적용: lock_timeout 3s 로 적용한다. 락을 못 잡아 실패하면 통계 배치 시간대(02:00~09:00 KST)를 피해 다시 시도한다.
--   컬럼 삭제는 메타데이터 변경이라 테이블 재작성이 없지만 잠깐 ACCESS EXCLUSIVE 락을 잡는다.
--
-- 멱등: drop column if exists.

set lock_timeout = '3s';

alter table battle_event_mode
    drop column if exists result_type,
    drop column if exists player_counts,
    drop column if exists color;
