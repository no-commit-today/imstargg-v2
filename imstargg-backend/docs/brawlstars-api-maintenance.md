# 브롤스타즈 API 점검 대응

브롤스타즈 API는 종종 점검 상태가 되며, 이 기간 동안 API 호출 시 503 ServiceUnavailable이 반환된다. 시스템은 점검 상태를 DB에 저장하고, 각 모듈이 적절히 대응한다.

## 점검 상태 저장

- 테이블: `brawlstars_api_maintenance` (시작 시간, 종료 시간)
- 엔티티: `BrawlStarsApiMaintenanceEntity.isActive(now)` 로 활성 여부 판단

## 모듈별 점검 대응

### core-api — DB 사전 확인

| 위치 | 동작 |
|------|------|
| `RenewalService.doRenew()` | 점검 중이면 즉시 503 예외 |

- SQS 메시지를 보내기 전에 차단하여 큐 플러딩 방지
- 사용자에게 즉시 점검 중임을 알림

### core-worker — DB 사전 확인 + 503 자동 등록

| 위치 | 사전 확인 | 503 시 자동 등록 |
|------|----------|----------------|
| `PlayerRenewalService.renew()` | O | O |
| `ClubRenewalService.renew()` | O | O |
| `StatsBattleAccumulationScheduler.accumulate()` | O | O |

- **사전 확인**: `BrawlStarsApiMaintenanceChecker.isUnderMaintenance()` 로 DB 조회. 점검 중이면 API 호출 없이 종료
- **503 자동 등록**: DB에 점검 상태가 아직 없을 때 API 호출 중 503 발생 시, `BrawlStarsApiMaintenanceAutoRegistrar.register()` 로 점검 상태 저장 (기본 1시간)
- 갱신 서비스는 점검 시 FAILED 처리 후 return (throw 시 SQS 재시도 발생하므로)
- 스케줄러는 503 감지 시 나머지 플레이어 처리 중단 (break)

### core-batch — DB 확인 없음, API 직접 호출

| 위치 | 사전 확인 | 503 처리 |
|------|----------|---------|
| `BrawlerSyncJobConfig` | X | 로그 + 정상 종료 |
| `BattleEventRotationSyncJobConfig` | X | 로그 + 정상 종료 |
| `BattleEventModeSyncJobConfig` | X | 로그 + 정상 종료 |

- DB 점검 상태를 확인하지 않는 이유:
  - 배치는 호출 빈도가 낮아 매번 DB를 조회할 필요 없음
  - DB 상태가 실제 API 상태와 비동기화될 수 있어 직접 호출이 더 정확
- `BrawlStarsApiTasklet` 이 `ServiceUnavailable` 을 catch하여 로그 남기고 정상 종료 (예외를 던지지 않아 잡이 실패로 처리되지 않음)

### core-admin — 수동 등록/관리

| 위치 | 동작 |
|------|------|
| `BrawlStarsApiMaintenanceListView` | 점검 목록 조회, 등록, 삭제 |

- 브롤스타즈 공지에 예고된 점검을 미리 등록 가능

## 점검 등록 방식

| 방식 | 모듈 | 설명 |
|------|------|------|
| 수동 등록 | core-admin | 관리자가 시작/종료 시간 지정하여 등록 |
| 자동 등록 | core-worker | 503 감지 시 현재 시각 + 1시간으로 자동 등록 |

### 자동 등록 동시성

- 모든 워커에서 스케줄러가 실행되므로 여러 워커가 동시에 503을 감지할 수 있음
- check-then-insert 패턴으로 기존 활성 점검이 있으면 건너뜀
- 레이스 컨디션 시 중복 레코드가 생길 수 있으나, `existsBy...` 쿼리가 하나라도 있으면 true를 반환하므로 무해