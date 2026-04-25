# core-batch 팀 통계 파이프라인 재설계서 (스켈레톤)

- 작성일: 2026-04-23 (선행 작업 반영 갱신: 2026-04-25)
- 상태: **스켈레톤** — 본 스펙의 진입 전제(스펙 A 완료)는 충족됨. 본문 설계는 스펙 A 결과 위에서 메모리·트랜잭션 길이 실측 후 옵션 택일하여 채운다.
- 선행 문서:
  - core-batch 코드 리뷰 최종 보고서 (2026-04-23)
  - `2026-04-23-core-batch-data-integrity-design.md` — **구현 완료, 머지됨** (1-A rank battle_key 재정의, 1-B 수동 EM 트랜잭션 합류, 2-C 자연키 unique, 3-A 재실행 runbook)

## 1. 목적

스펙 A 가 "값 자체의 정확성과 멱등성" 을 확보했다는 전제 위에서, 팀 통계 파이프라인의 **메모리 풋프린트, 트랜잭션 길이, 이벤트 단위 원자성** 을 구조적으로 해결한다.

> 스펙 A 의 단일-트랜잭션 합류(`§4.2`)로 "delete→insert 사이 외부 가시 공백" 자체는 사라졌다. 본 스펙이 다루는 잔여 문제는 그 결과로 길어진 트랜잭션과, 이벤트 1 건 실패 시 step 전체가 롤백되는 의도된 미해결 한계다.

## 2. 범위 (후보)

- **2-A** 팀 통계 피크 메모리: aggregator map + `toEntities()` List + EM 1차 캐시 동시 생존
- **트랜잭션 길이 단축 + 이벤트 단위 원자성 회복** (스펙 A §4.2 의 의도된 미해결 한계 해소): tasklet → chunk-oriented step 전환 또는 DB 집계(`GROUP BY`) 이관
  - 스펙 A 에서 1-B(수동 EM) / 2-B(가시 공백) 의 표면 증상은 step 단일 트랜잭션 합류로 해결됨. 남은 문제는 그 결과 step 1 회 = tx 1 회 가 되어 트랜잭션이 길어지고 이벤트 단위 부분 진행이 불가하다는 점.
  - chunk-oriented step 또는 upsert(`ON CONFLICT DO UPDATE`) 도입 시 chunk·이벤트 단위 commit 으로 길이를 줄이고 부분 진행성을 회복한다.
- 참고: 2-C 자연키 unique 인덱스는 본 스펙의 upsert 전환에 **선결 조건**이며, 스펙 A 의 `V260423b__stats_natural_key_unique.sql` 마이그레이션으로 이미 추가되어 그대로 활용한다.

## 3. 설계 옵션 (플랜 단계에서 택일)

### 옵션 1: chunk-oriented step + streaming aggregator

- ItemReader 는 배틀을 읽는다.
- Processor 또는 Writer 에서 aggregator 가 key 단위로 누적하고, 임계치 초과 시 flush.
- toEntities 를 Sequence 로 변경해 materialization 을 막는다.
- upsert writer (`INSERT ... ON CONFLICT ... DO UPDATE SET counts = stats.counts + excluded.counts`).
- 장점: Spring Batch 기본 구조에 잘 맞음, 재시작 가능.
- 단점: rank counts 병합 UPDATE 표현이 복잡 (JSONB 또는 개별 칼럼 합산).

### 옵션 2: 완전한 DB 집계(`GROUP BY`) 이관

- 집계를 SQL 로 이관: `INSERT INTO brawler_team_battle_*_stats (...) SELECT ... FROM stats_*_battle GROUP BY ... ON CONFLICT ... DO UPDATE`.
- 장점: 메모리 상한이 DB 측에서 제어됨. 코드 단순화.
- 단점: `teams` JSON unnest + combination (2·3조합) 을 SQL 로 표현해야 함. PostgreSQL 특화 쿼리가 필요. Rank 가중합 계산을 SQL 로 옮기는 복잡성.

### 옵션 3 (점진): 옵션 1 먼저 → 필요 시 옵션 2 로 이관

- 현실 데이터 규모에서 옵션 1 이 충분하면 거기서 멈춘다.
- 관측 후 한계가 드러나면 옵션 2 로 이관.

## 4. 결정해야 할 사항

- 기본 옵션(플랜 단계 결정): 옵션 1 권장.
- upsert 시 rank counts 병합 전략: 개별 칼럼 합산 vs JSONB merge.
- `StatsTierRange.resolveAll` 의 다중 구간 집계를 reader 쪽에서 fan-out 할지 aggregator 에서 fan-out 할지.
- 스트리밍 flush 임계치(예: 10,000 키마다 flush).

## 5. 비목표

- 스펙 A 범위의 데이터 정확성 수정 (이미 완료 전제).
- 팀 통계 외의 잡 (Owning, Sync 등).
- 운영/알림/복원력 (스펙 C).
