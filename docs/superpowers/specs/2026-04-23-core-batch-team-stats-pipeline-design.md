# core-batch 팀 통계 파이프라인 재설계서 (스켈레톤)

- 작성일: 2026-04-23
- 상태: **스켈레톤** — 세부 설계는 스펙 A (데이터 정확성 번들) 완료 후 본문을 채운다.
- 선행 문서: core-batch 코드 리뷰 최종 보고서 (2026-04-23), `2026-04-23-core-batch-data-integrity-design.md`

## 1. 목적

스펙 A 가 "값 자체의 정확성과 멱등성" 을 확보했다는 전제 위에서, 팀 통계 파이프라인의 **메모리 풋프린트, 트랜잭션 길이, 외부 API 가시 공백** 을 구조적으로 해결한다.

## 2. 범위 (후보)

- **2-A** 팀 통계 피크 메모리: aggregator map + `toEntities()` List + EM 1차 캐시 동시 생존
- **1-B 구조적 해결**: tasklet → chunk-oriented step 전환 또는 DB 집계(`GROUP BY`) 이관
- **2-B 구조적 해결**: `delete → insert` 공백을 upsert(`ON CONFLICT DO UPDATE`) 전환으로 제거
- 참고: 2-C unique 인덱스는 본 스펙의 upsert 전환에 **선결 조건**이다. 스펙 A 에서 추가된 인덱스를 그대로 활용한다.

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
