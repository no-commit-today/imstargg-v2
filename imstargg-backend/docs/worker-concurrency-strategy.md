# Worker 동시성 제어 전략

이 문서는 Worker에서 Player/Club 데이터를 업데이트할 때 적용되는 동시성 제어 방식과 그 설계 배경을 설명합니다.

## 1. 개요

Worker는 같은 플레이어/클럽에 대해 여러 Job이 동시에 실행될 수 있습니다. 동시 실행 시 PlayerBrawler, Battle 등의 중복 저장을 방지하기 위해 동시성 제어가 필요합니다.

현재 두 가지 방식이 혼용됩니다.

| 대상 | 방식 |
|------|------|
| `PlayerUpdater` | Pessimistic Lock (`SELECT FOR UPDATE`) |
| `ClubUpdater` | Advisory Lock (`pg_advisory_lock`) |

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

### 2.3 Advisory Lock 대신 Pessimistic Lock을 선택한 이유

Advisory Lock은 row가 없어도 lock을 획득할 수 있고 전용 커넥션 풀을 사용하는 장점이 있지만, PlayerUpdater의 경우 아래 이유로 Pessimistic Lock이 더 적합합니다.

- **단순성**: Advisory Lock은 별도 `lockDataSource`, `LockTemplate`, SHA-256 해시 키 변환 등 인프라 복잡도가 높습니다. Pessimistic Lock은 JPA 쿼리 하나(`SELECT FOR UPDATE`)로 대체됩니다.
- **트랜잭션 범위 일치**: 기존 구조에서 Advisory Lock 아래에 별도 트랜잭션 2개(`PlayerUpdateApplier`, `BattleUpdateApplier`)가 있었습니다. Pessimistic Lock은 단일 `@Transactional` 안에서 모두 처리되어 원자성이 보장됩니다.
- **Worker 특성**: core-worker는 API 서버와 달리 대규모 동시 요청이 없으므로, 트랜잭션이 길어져 main 커넥션 풀을 점유하더라도 풀 고갈 위험이 낮습니다.
- **데드락 위험 수용 가능**: Player 업데이트는 `player → player_brawler → gear/gadget/... → battle → battle_team_player` 순서로 항상 동일한 순서로 write하므로, 데드락 발생 가능성이 낮습니다.

### 2.4 관련 코드

- `PlayerUpdater`: `@Transactional` + `findWithLockByBrawlStarsTag()`
- `PlayerUpdateApplier`: player 정보 및 brawler 동기화 (`@Transactional`)
- `BattleUpdateApplier`: battle 저장 및 중복 제거 (`@Transactional`)
- `PlayerRenewalService`: 배틀 빌드 및 `statsBattleAggregator.asyncAggregate()` 호출 (트랜잭션 바깥)

---

## 3. ClubUpdater — Advisory Lock

### 3.1 동작 구조

```
Advisory Lock 획득 (별도 lock 커넥션)
├─ find or create ClubEntity
└─ clubUpdateApplier.apply()    ← @Transactional
Advisory Lock 해제
```

### 3.2 Advisory Lock을 유지하는 이유

Club 업데이트의 경우 Pessimistic Lock으로 전환하지 않았습니다. 현재 구조상 `ClubUpdateApplier`가 별도 트랜잭션으로 동작하며, 전환 시 트랜잭션 구조 재설계가 필요합니다. 또한 Club 업데이트 빈도가 상대적으로 낮아 현행 유지가 적절합니다.

### 3.3 관련 코드

- `PostgresAdvisoryLockTemplate`: lock 획득/해제 구현
- `LockConfig`: 전용 `lockDataSource` 및 bean 설정 (`@Profile("db-lock")`)
- `LockName`: lock 키 생성 (`club:{tag}` → SHA-256 → 64비트 정수)
- `ClubUpdater`: lock 사용처

### 3.4 구현 상세

- Lock 키: `club:{tag}`를 SHA-256 해시하여 64비트 정수로 변환
- 타임아웃: `SET lock_timeout`으로 3초 설정. 초과 시 `SQLException` → `IllegalStateException` 변환
