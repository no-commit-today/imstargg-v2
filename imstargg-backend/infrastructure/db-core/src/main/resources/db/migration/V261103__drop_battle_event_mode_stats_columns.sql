-- 이벤트 모드의 통계 관련 컬럼(result_type, player_counts, color)을 없앤다.
-- 대상: battle_event_mode
--
-- 배경: 스펙 A(통계 수집 모드 허용 목록) 결정 5. 결과형/순위형은 수집 유형 enum(StatsEventModeType)과 stats_event_mode 테이블이 단일 출처이고,
--   결과형 게임 수는 3v3 상수(슬롯 합 ÷ 6)로 계산하며, 카드 배경색은 프론트 상수로 옮겼다.
--   이 컬럼들을 읽고 쓰는 곳은 더 이상 없다.
--
-- 배포 순서: 앱 배포 → (확인) → DDL.
--   세 컬럼 모두 DB 기본값이 있어, 앱 배포 뒤 DDL 전까지 새 코드의 INSERT 는 실패하지 않는다.
--   반대로 DDL 이 먼저면 구버전 앱의 조회가 실패한다(결정 10 으로 감수).
--   - DDL 전에 core-api(Deployment 의 모든 파드)와 core-batch(CronJob, values.yaml 의 이미지 태그)가 새 이미지여야 한다.
--     Deploy 워크플로 성공은 이미지 태그 커밋일 뿐이므로, 파드와 태그가 실제로 새 이미지로 동기화됐는지 확인한 뒤 DDL 을 적용한다.
--   - 구 core-batch 가 남아 있으면 08:00 KST 의 battleEventModeSyncJob·battleEventSyncJob 이 그날 실패한다.
--     그러면 미등록 알림과 경쟁전 맵 목록 갱신이 하루 동안 빠진다.
--   - core-worker 는 battle_event_mode 를 읽지 않아 이 순서와 무관하다.
--   - DDL 뒤에는 지금처럼 core-admin 을 새 코드로 다시 띄운다. core-admin 은 Deploy 워크플로 밖에서 로컬로 띄우고,
--     예전 체크아웃의 core-admin 을 운영 DB 에 붙이면 모드 목록·대시보드가 실패한다.
--
-- 적용: lock_timeout 3s 로 적용한다. 락을 못 잡아 실패하면 battle_event_mode 를 읽는 배치
--   (08:00 KST battleEventModeSyncJob·battleEventSyncJob)가 도는 시각을 피해 다시 시도한다.
--   컬럼 삭제는 메타데이터 변경이라 테이블 재작성이 없지만 잠깐 ACCESS EXCLUSIVE 락을 잡는다.
--
-- 롤백: 이 마이그레이션은 forward-only 다.
--   - DDL 뒤에 구 이미지로 롤백하면 구 엔티티가 지워진 컬럼을 SELECT 하다 실패한다.
--     이벤트·로테이션·전투 기록 응답이 그렇다.
--   - 컬럼을 다시 만드는 마이그레이션(V260426b·V260519a·V260605)을 다시 돌려도 값은 돌아오지 않는다.
--     result_type = 'RANK' 는 시드된 적이 없고 관리자 PATCH 로만 들어갔다. 그래서 복구하면 모든 모드가 RESULT 가 된다.
--     시드 밖의 관리자 색과 player_counts 도 사라진다.
--   - 값을 살리려면 DDL 전에 아래 백업을 만들어 둔다. 실행문이 아니라 배포 담당자가 고르는 선택 사항이다.
--       create table if not exists battle_event_mode_stats_backup_261103 as select brawlstars_id, result_type, player_counts, color from battle_event_mode;
--   - 롤백 순서: 컬럼 재생성 → 백업에서 값 복원 → 구 이미지 배포.
--
-- 멱등: drop column if exists.

set lock_timeout = '3s';

alter table battle_event_mode
    drop column if exists result_type,
    drop column if exists player_counts,
    drop column if exists color;
