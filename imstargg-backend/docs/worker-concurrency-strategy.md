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
├─ playerRepository.findWithLockByBrawlStarsTag()   ← SELECT FOR UPDATE
│   └─ row 없으면 Player.create(response)           ← 도메인 팩터리로 새 Player
├─ player.update(response)                          ← 도메인 내부 변경/신규 추적
├─ playerRepository.save(player)                    ← PlayerEntity + brawlers + accessories 저장
└─ updateBattles(player.entity, battles)            ← battle 저장 및 중복 제거
@Transactional 커밋
```

`PlayerUpdater.update()` / `updatePlayer()` / `updateBattles()` 모두 `@Transactional`이며, `update()`는 내부적으로 `updateBattles()`를 호출해 단일 트랜잭션 안에서 player 락 → battle 저장까지 이어집니다.

### 2.2 INSERT Race 처리

`SELECT FOR UPDATE`는 존재하는 row만 lock할 수 있으므로, 신규 플레이어가 동시에 들어오면 두 Worker가 모두 `null`을 확인하고 INSERT를 시도할 수 있습니다.

```
Worker A: SELECT → null → INSERT player
Worker B: SELECT → null → INSERT player (race!)
```

이 경우 `uk_player__brawlstarstag` 유니크 제약 위반으로 한쪽이 `DataIntegrityViolationException`을 발생시키고 트랜잭션이 롤백됩니다. `PlayerUpdater.update()`/`ClubUpdater.update()`에 선언된 `@Retryable(retryFor = [DataIntegrityViolationException::class], maxAttempts = 2)`이 새 트랜잭션으로 1회 재시도하며(Spring Retry 인터셉터가 트랜잭션 인터셉터 바깥에 적용), 재시도 시점에는 이미 row가 존재하므로 `SELECT FOR UPDATE`가 정상 동작합니다. API 응답 객체는 이미 확보된 상태라 BrawlStars API 재호출 없이 DB 갱신만 재시도됩니다.

이 상황은 신규 플레이어/클럽이 처음 등록될 때만 발생하며, 재시도 1회로 흡수됩니다.

### 2.3 관련 코드

- `PlayerUpdater` (application): 얇은 `@Transactional` 오케스트레이터. `update` / `updatePlayer` / `updateBattles` 제공
- `Player` (domain): `PlayerEntity` + brawlers/accessory 엔티티들을 감싸는 쓰기 애그리거트. `create(response)` 팩터리 및 `update(response)` 로직 보유
- `PlayerRepository` (application): `findWithLockByBrawlStarsTag` + `save`. PlayerEntity, PlayerBrawler, 5종 accessory에 대한 `saveAll` 수행 (dirty checking으로 기존 엔티티는 변경 시에만 UPDATE, 신규는 INSERT)
- `PlayerRenewalService`: 배틀 빌드 및 `statsBattleAggregator.aggregate()` 호출 (트랜잭션 바깥, COMPLETED 후 예외 격리)

---

## 3. ClubUpdater — Pessimistic Lock

### 3.1 동작 구조

```
@Transactional 시작
├─ clubRepository.findWithLockByBrawlStarsTag()    ← SELECT FOR UPDATE
│   └─ row 없으면 Club.create(response)            ← 도메인 팩터리로 새 Club
├─ club.update(response)                           ← 엔티티 필드 갱신 + members 전체 재구성
└─ clubRepository.save(club)                       ← 기존 멤버 deleteAllInBatch → 신규 saveAll
@Transactional 커밋
```

PlayerUpdater와 동일한 Pessimistic Lock 패턴을 사용합니다. 차이점은 club member는 삭제될 수 있으므로, 저장 단계에서 기존 멤버를 일괄 삭제하고 응답 기준으로 재삽입합니다 (`(club_id, brawlstars_tag)` 유니크 인덱스 보존을 위해 `deleteAllInBatch`로 즉시 실행).

### 3.2 Pessimistic Lock 선택 이유

기존에는 Advisory Lock(`pg_advisory_lock`)을 사용했으나, 다음 이유로 Pessimistic Lock으로 전환했습니다.

- **단순성**: Advisory Lock은 별도 `lockDataSource`, `LockTemplate`, SHA-256 해시 키 변환 등 인프라 복잡도가 높습니다. Pessimistic Lock은 JPA 쿼리 하나(`SELECT FOR UPDATE`)로 대체됩니다.
- **트랜잭션 범위 일치**: Pessimistic Lock은 단일 `@Transactional` 안에서 모두 처리되어 원자성이 보장됩니다.
- **PlayerUpdater와 일관성**: 두 Updater가 동일한 패턴을 사용하여 코드 이해와 유지보수가 용이합니다.

### 3.3 관련 코드

- `ClubUpdater` (application): 얇은 `@Transactional` 오케스트레이터
- `Club` (domain): `ClubEntity` + members 목록을 감싸는 쓰기 애그리거트. `create(response)` 팩터리 및 `update(response)`에서 멤버 전체 재구성
- `ClubRepository` (application): `findWithLockByBrawlStarsTag` + `save` (기존 멤버 `deleteAllInBatch` 후 `saveAll`)

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
- `RenewalProcessor` (application/renewal): Player/Club 공통 상태 전이 + 트랜잭션 경계. NOT_FOUND 시 비활성화는 호출부가 deactivate 액션으로 주입
- `PlayerRenewalService.renew()` / `ClubRenewalService.renew()`: `executing()` 호출 시 `ObjectOptimisticLockingFailureException` catch (동일 패턴)
