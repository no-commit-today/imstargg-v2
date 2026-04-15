# Worker 동시성 제어 전략

이 문서는 Worker에서 Player/Club 데이터를 업데이트할 때 적용되는 동시성 제어 방식과 그 설계 배경을 설명합니다.

## 1. 개요

Worker는 같은 플레이어/클럽에 대해 여러 Job이 동시에 실행될 수 있습니다. 동시 실행 시 PlayerBrawler, Battle 등의 중복 저장을 방지하기 위해 동시성 제어가 필요합니다.

| 대상             | 방식                                      |
| ---------------- | ----------------------------------------- |
| `PlayerUpdater`  | Pessimistic Lock (`SELECT FOR UPDATE`)    |
| `ClubUpdater`    | Pessimistic Lock (`SELECT FOR UPDATE`)    |
| `RenewalEntity`  | Optimistic Lock (`@Version`)              |

---

## 2. PlayerUpdater — Pessimistic Lock

### 2.1 동작 구조

```
@Transactional 시작
├─ findWithLockByBrawlStarsTag()    ← SELECT FOR UPDATE
│   └─ row 없으면 createPlayer()   ← INSERT (IDENTITY 전략으로 즉시 실행)
├─ playerUpdateApplier.apply()     ← 트랜잭션 합류 (REQUIRED)
└─ battleUpdateApplier.apply()     ← 트랜잭션 합류 (REQUIRED)
@Transactional 커밋
```

`PlayerUpdater.update()` 및 `updatePlayer()`는 `@Transactional`로 감싸져 있고,
`PlayerUpdateApplier`, `BattleUpdateApplier`의 내부 `@Transactional`은 `REQUIRED` 전파로 외부 트랜잭션에 합류합니다.

### 2.2 INSERT Race 처리

`SELECT FOR UPDATE`는 존재하는 row만 lock할 수 있으므로, 신규 플레이어가 동시에 들어오면 두 Worker가 모두 `null`을 확인하고 INSERT를 시도할 수 있습니다.

```
Worker A: SELECT → null → INSERT player
Worker B: SELECT → null → INSERT player (race!)
```

이 경우 `uk_player__brawlstarstag` 유니크 제약 위반으로 한쪽이 `DataIntegrityViolationException`을 발생시키고 트랜잭션이 롤백됩니다. 재시도는 상위 호출자(큐 재처리)에서 처리하며, 다음 시도에서는 이미 row가 존재하므로 `SELECT FOR UPDATE`가 정상 동작합니다.

이 상황은 신규 플레이어가 처음 등록될 때만 발생하며 빈도가 낮아 수용 가능한 트레이드오프로 판단했습니다.

### 2.3 관련 코드

- `PlayerUpdater`: `@Transactional` + `findWithLockByBrawlStarsTag()`
- `PlayerUpdateApplier`: player 정보 및 brawler 동기화 (`@Transactional`)
- `BattleUpdateApplier`: battle 저장 및 중복 제거 (`@Transactional`)
- `PlayerRenewalService`: 배틀 빌드 및 `statsBattleAggregator.asyncAggregate()` 호출 (트랜잭션 바깥)

---

## 3. ClubUpdater — Pessimistic Lock

### 3.1 동작 구조

```
@Transactional 시작
├─ findWithLockByBrawlStarsTag()    ← SELECT FOR UPDATE
│   └─ row 없으면 createClub()
└─ clubUpdateApplier.apply()        ← 트랜잭션 합류 (REQUIRED)
@Transactional 커밋
```

PlayerUpdater와 동일한 Pessimistic Lock 패턴을 사용합니다.

### 3.2 Pessimistic Lock 선택 이유

기존에는 Advisory Lock(`pg_advisory_lock`)을 사용했으나, 다음 이유로 Pessimistic Lock으로 전환했습니다.

- **단순성**: Advisory Lock은 별도 `lockDataSource`, `LockTemplate`, SHA-256 해시 키 변환 등 인프라 복잡도가 높습니다. Pessimistic Lock은 JPA 쿼리 하나(`SELECT FOR UPDATE`)로 대체됩니다.
- **트랜잭션 범위 일치**: Pessimistic Lock은 단일 `@Transactional` 안에서 모두 처리되어 원자성이 보장됩니다.
- **PlayerUpdater와 일관성**: 두 Updater가 동일한 패턴을 사용하여 코드 이해와 유지보수가 용이합니다.

### 3.3 관련 코드

- `ClubUpdater`: `@Transactional` + `findWithLockByBrawlStarsTag()`
- `ClubUpdateApplier`: club 정보 및 member 동기화 (`@Transactional`)

---

## 4. RenewalEntity — Optimistic Lock

### 4.1 배경

SQS Standard Queue는 at-least-once 전달을 보장하므로, 동일 tag에 대한 중복 메시지가 도착할 수 있습니다. 두 Worker가 동시에 같은 renewal의 PENDING → EXECUTING 전환을 시도하면 BrawlStars API를 불필요하게 중복 호출합니다.

### 4.2 동작 구조

RenewalEntity에 JPA `@Version` 필드를 추가하여 낙관적 락을 적용합니다.

```text
Worker A: SELECT renewal (version=0) → toExecuting() → UPDATE (version=0→1) ✅
Worker B: SELECT renewal (version=0) → toExecuting() → UPDATE (version=0→?) ← ObjectOptimisticLockingFailureException
```

Worker B는 `ObjectOptimisticLockingFailureException`을 catch하고, WARN 로깅 후 처리를 건너뜁니다.

### 4.3 관련 코드

- `RenewalEntity`: `@Version` 필드
- `PlayerRenewalService.renew()`: `executing()` 호출 시 `ObjectOptimisticLockingFailureException` catch
- `ClubRenewalService.renew()`: 동일 패턴
