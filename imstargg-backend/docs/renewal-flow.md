# Renewal 갱신 흐름

플레이어/클럽 데이터 갱신 요청의 생명주기를 설명합니다.

## 1. 상태 머신

```text
(신규) → REQUESTED → PENDING → EXECUTING → COMPLETED
                                          → NOT_FOUND
                                          → FAILED
       ← ─ ─ ─ ─  reRequest()  ─ ─ ─ ─ ─←
```

`reRequest()`는 모든 상태에서 호출 가능하며, `REQUESTED`로 리셋합니다.
단, 도메인 레이어(`Renewal.canRenew()`)에서 시간 기반으로 재요청 가능 여부를 제어합니다.

## 2. 상태 전환 규칙

| 전환                          | 호출 위치       | 조건                    |
| ----------------------------- | --------------- | ----------------------- |
| `(신규)` → `REQUESTED`        | core-api        | 최초 갱신 요청          |
| `REQUESTED` → `PENDING`       | core-api        | SQS 메시지 발행 후      |
| `PENDING` → `EXECUTING`       | core-worker     | 워커가 처리 시작        |
| `EXECUTING` → `COMPLETED`     | core-worker     | API 호출 및 DB 갱신 성공 |
| `EXECUTING` → `NOT_FOUND`     | core-worker     | API 404 응답            |
| `EXECUTING` → `FAILED`        | core-worker     | 예외 발생               |
| `*` → `REQUESTED`             | core-api        | `reRequest()` 재요청    |

## 3. 전체 흐름

```text
[Client]
  │
  ▼
[core-api] POST /v1/players/{tag}/renewal
  ├─ 유지보수 중인지 확인
  ├─ 기존 renewal 조회 → canRenew(now) 확인
  ├─ RenewalEntity 생성 또는 reRequest() → REQUESTED
  ├─ SQS 메시지 발행 (PlayerRenewalMessage)
  └─ toPending() → PENDING
       │
       ▼
[SQS Queue]
       │
       ▼
[core-worker] PlayerRenewalMessageConsumer
  ├─ PlayerRenewalService.renew(tag)
  │   ├─ renewal 조회 → status == PENDING 확인
  │   ├─ 유지보수 중인지 확인
  │   ├─ toExecuting() → EXECUTING (낙관적 락 보호)
  │   ├─ BrawlStars API 호출 (player 정보 + 최근 배틀)
  │   ├─ PlayerUpdater.update() (비관적 락 보호)
  │   ├─ toCompleted() → COMPLETED
  │   └─ statsBattleAggregator.aggregate() (동기 — 예외 격리, 실패해도 COMPLETED 유지)
  └─ 완료
```

통계 집계는 COMPLETED 확정 이후의 부가 작업이다. 집계 예외는 `PlayerRenewalService` 가 error
로그만 남기고 삼킨다 — 완료된 renewal 에 FAILED 전이를 시도해 SQS 재전달로 번지는 것을 막기
위한 격리다. 재시도(`@Retryable`, 유니크 충돌·일시 오류 대상) 소진 후의 실패는 허용된 드문
유실이며, 해당 플레이어가 `stats_player` 대상이면 축적 스케줄러가 7일 내 재수집할 수 있다.

## 4. 재요청 타임아웃

`canRenew(now)`는 완료 여부에 따라 재요청 가능 여부를 판단합니다.

| 상태                                         | 재요청 가능 조건                   |
| -------------------------------------------- | ---------------------------------- |
| 미완료 (`REQUESTED`, `PENDING`, `EXECUTING`) | `requestedAt`으로부터 10초 경과 후 |
| 완료 (`COMPLETED`, `NOT_FOUND`, `FAILED`)    | 즉시 가능                          |

워커 크래시 등으로 renewal이 미완료 상태에 고착되더라도, 10초 후 클라이언트가 재요청하면 `reRequest()`로 리셋되어 새로운 갱신 사이클이 시작됩니다.

## 5. 동시성 보호

### 5.1 낙관적 락 (`@Version`)

`RenewalEntity`에 JPA `@Version` 필드를 적용합니다. SQS Standard Queue의 at-least-once 전달로 인해 동일 tag에 대한 중복 메시지가 도착할 수 있습니다.

두 워커가 동시에 PENDING → EXECUTING 전환을 시도하면, 한쪽이 `ObjectOptimisticLockingFailureException`을 받고 WARN 로깅 후 처리를 건너뜁니다.

### 5.2 비관적 락 (Player/Club 업데이트)

`PlayerUpdater`와 `ClubUpdater`는 `SELECT FOR UPDATE`로 동일 row에 대한 동시 업데이트를 방지합니다. 자세한 내용은 `worker-concurrency-strategy.md`를 참조하세요.
