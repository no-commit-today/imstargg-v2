# Advisory Lock 전략

이 문서는 Player/Club 업데이트 시 PostgreSQL Advisory Lock을 사용하는 이유와 Pessimistic Lock(SELECT FOR UPDATE) 대비 장점을 설명합니다.

## 1. 개요

Worker에서 Player/Club 데이터를 업데이트할 때, 같은 대상에 대한 동시 실행이 발생할 수 있습니다. 동시 실행 시 PlayerBrawler, Battle 등의 중복 저장을 방지하기 위해 동시성 제어가 필요합니다.

### 현재 방식

PostgreSQL Advisory Lock(`pg_advisory_lock`)을 사용하여 대상(player tag, club tag) 단위로 직렬화합니다.

```
Advisory Lock 획득 (별도 lock 커넥션)
├─ find or create entity
├─ playerUpdateApplier.apply()    ← @Transactional #1
├─ battleUpdateApplier.apply()    ← @Transactional #2
Advisory Lock 해제
```

## 2. Pessimistic Lock을 사용하지 않는 이유

### 2.1 신규 엔티티 INSERT Race

Advisory Lock의 가장 중요한 장점은 **row가 존재하지 않아도 lock을 획득할 수 있다**는 것입니다.

```kotlin
// advisory lock 안에서 실행
val player = playerRepository.findByBrawlStarsTag(tag) ?: createPlayer(response)
```

`SELECT FOR UPDATE`는 존재하는 row만 lock할 수 있으므로, 신규 플레이어가 처음 들어올 때 두 워커가 동시에 null을 확인하고 중복 INSERT를 시도하는 race condition이 발생합니다.

### 2.2 트랜잭션 범위 문제

현재 구조는 advisory lock 하나 아래에 **별도 트랜잭션 2개**(PlayerUpdateApplier, BattleUpdateApplier)가 있습니다. Pessimistic Lock이 유효하려면 lock 획득부터 모든 write가 하나의 트랜잭션 안에 있어야 하므로, 전체 트랜잭션 구조를 재설계해야 합니다.

### 2.3 커넥션 풀 격리

Advisory Lock은 전용 `lockDataSource`(별도 HikariCP pool)를 사용하여 main 커넥션 풀에 영향을 주지 않습니다. Pessimistic Lock은 main pool 커넥션을 lock 대기/유지 동안 점유하므로, 동시 요청이 많을 때 main pool 고갈 위험이 있습니다.

### 2.4 데드락 위험

Player 업데이트는 여러 테이블에 write합니다(`player` -> `player_brawler` -> `gear/gadget/starpower/...` -> `battle` -> `battle_team_player`). 이를 하나의 긴 트랜잭션으로 묶으면 다른 트랜잭션과의 데드락 가능성이 높아집니다. Advisory Lock은 JPA 트랜잭션 바깥에서 동작하므로 row-level 데드락과 무관합니다.

## 3. 구현 구조

### Lock 키 생성

Lock name(`player:{tag}`, `club:{tag}`)을 SHA-256 해시하여 64비트 정수로 변환합니다.

### 타임아웃

`SET lock_timeout`으로 blocking lock에 타임아웃을 설정합니다(현재 3초). 타임아웃 시 `SQLException` → `IllegalStateException`으로 변환됩니다.

### 관련 코드

- `PostgresAdvisoryLockTemplate`: lock 획득/해제 구현
- `LockConfig`: 전용 `lockDataSource` 및 bean 설정 (`@Profile("db-lock")`)
- `PlayerUpdater`, `ClubUpdater`: lock 사용처