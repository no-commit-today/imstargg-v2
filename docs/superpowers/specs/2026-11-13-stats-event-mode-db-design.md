# 통계 수집 모드 DB 관리 설계

- 작성일: 2026-11-13
- 대상: `core/core-enum`, `infrastructure/db-core`, `core/core-worker`(통계 전투 수집), `core/core-batch`(봇 판정·순위 집계), `core/core-api`(이벤트 모드 응답), `core/core-admin`, `imstargg-admin-frontend`, `imstargg-frontend`(주석), `imstargg-backend/docs`, `docs/superdomain/DOMAIN.md`
- 상태: 설계 확정 (2026-11-13 사용자 승인, 구현 전)
- 관련: [스펙 A — 통계 대상 모드 정리](2026-11-03-stats-event-mode-whitelist-design.md). 이 설계는 스펙 A 의 결정 2(허용 목록은 코드 enum)를 대체한다. 나머지 결정(수집 시점 필터, 팀 구성 검사, `battle_event_mode` 의 통계 컬럼 제거, 색은 프론트 상수)은 그대로 둔다.
- 운영 상태(2026-11-13, git 기준): 스펙 A 는 develop 에만 있고 운영 main 에는 아직 없다. 운영 이미지는 core-worker `0.3.0-73053da`, core-api `0.5.1-7aef2d1`, core-batch `0.4.3-7aef2d1` 로 스펙 A 이전이다. 사용자 결정으로 이 설계는 스펙 A 와 같은 릴리즈로 운영에 나간다(U6, §3.9).

## 1. 의도

통계를 수집하는 모드를 **코드 배포 없이** 관리자 화면에서 바꾸고, 그 정보를 **한 곳**에서 관리한다.

지금은 수집 모드 목록과 모드별 유형·팀 구성이 코드 enum `StatsEventMode` 에 있다. 모드를 켜거나 끄려면 enum 을 고쳐 core-api·core-worker·core-batch 를 다시 배포해야 한다. 모드 자체(이름·아이콘)는 DB 와 관리자 화면에서 관리하므로, 모드 정보가 두 곳으로 나뉘어 있다.

### 1.1 사용자가 정한 것

| # | 결정 | 이유 |
|---|---|---|
| U1 | 수집 모드를 DB 로 관리한다 | 배포 없이 바꾸고, 모드 정보를 한 곳에서 관리하기 위해서다 |
| U2 | `battle_event_mode` 에 통계용 컬럼을 두지 않고, **통계 쪽 새 테이블**에 둔다 | 모드가 통계를 아는 모양을 피한다. 수집 여부는 표본량 같은 통계 판단이라 모드의 사실이 아니다 |
| U3 | DB 조회 부하는 로컬 캐시로 흡수한다 | 수집 경로에서 매번 DB 를 읽지 않기 위해서다 |
| U4 | 모드별 팀 인원 숫자(옛 `player_counts` 같은 자유 값)는 두지 않는다 | 통계가 3대3 결과형과 쇼다운 3종만 수집하므로, 필요한 구성은 몇 가지로 고정된다 |
| U5 | 처음 넣는 데이터에서 와이프아웃(25)과 와이프아웃 5v5(31)는 뺀다 | 판 수가 적다. 25 는 3대3 이라 나중에 관리자가 켤 수 있다. 31 은 5대5 라 새 유형이 필요해 이번 범위 밖이다(§1.3) |
| U6 | 스펙 A 와 같은 develop → main 릴리즈로 함께 운영에 낸다 | 사용자 결정(2026-11-13). 배포 순서와 롤백은 §3.9 |

### 1.2 목표

1. 관리자가 배포 없이 모드의 통계 수집을 켜고 끈다. 이미 있는 수집 유형(§3.1)의 새 모드도 배포 없이 추가한다.
2. 수집 모드 정보는 DB 의 통계 수집 모드 테이블 한 곳에 있고, 관리자는 기존 모드 편집 화면에서 고친다. 코드에는 유형별 규칙만 남는다.
3. 배포 직후의 수집 결과가 스펙 A(develop)가 정한 것과 같다. 같은 12개 모드를 같은 유형·팀 구성으로 수집한다. 운영은 지금 모든 모드를 수집하므로, 운영 기준으로는 수집 모드가 이때 처음 줄어든다(U6).

### 1.3 비목표

- 모드 카드 색(프론트 `MODE_COLORS`)을 DB 로 옮기는 일. **(Claude 판단)** 색은 표시 데이터이고 통계와 무관하다. 스펙 A 가 일부러 프론트로 옮겼다. 그래서 관리자가 새 모드를 켜면 그 카드는 프론트에 색을 더하기 전까지 기본 회색으로 보인다. 색까지 한 곳에서 관리하려면 별도 작업으로 다룬다.
- 새 수집 유형(예: 5대5 통계). 결과형 게임 수를 `슬롯 합 ÷ 6` 으로 세는 전제가 바뀌는 일이라 코드와 배포를 거친다.
- 봇 판정 임계값·감시 기준을 DB 로 옮기는 일.
- 과거 통계 행 정리, 집계 규칙 변경.

### 1.4 반목표 (기술적으로 돌아가도 실패인 것)

- **worker 가 조용히 수집을 멈춘다.** 테이블이 비었거나 조회에 실패했는데 아무 신호 없이 수집이 0 이 되면 실패다.
- **`battle_event_mode` 에 통계를 아는 컬럼이 생긴다.**
- **미설정을 가리는 기본값.** 지난번 `player_counts` 는 기본값 `[3, 3]` 때문에 68개 중 하나만 빼고 아무도 값을 정하지 않은 사실이 드러나지 않았다. 여기서는 "행이 없음 = 수집 안 함" 이다.

## 2. 접근 비교

| 안 | 내용 | 장점 | 단점 | 결과 |
|---|---|---|---|---|
| 1 | **통계 쪽 새 테이블**에 (모드 ID, 수집 유형)을 두고 관리자 모드 편집 화면에서 함께 고친다 | 이름이 정직하다. statistics → brawlstars 를 ID 로 가리키는 방향이라 DOMAIN.md 와 맞는다(읽는 경로는 §3.4·§3.6). 모드 테이블이 통계를 모른다 | 테이블이 하나 는다 | **선택 (U2)** |
| 2 | `battle_event_mode` 에 컬럼을 더한다 | 테이블 하나로 끝난다 | 수집 여부는 통계 판단이다. 이름을 중립으로 지으면 뜻을 숨기고, 정직하게 지으면 모드가 통계를 안다 | 제외 |
| 3 | 모드에는 중립적인 "플레이 형식" 컬럼, 수집 여부는 통계 테이블 | 형식이 게임 사실로 남는다 | 지금 형식을 쓰는 곳이 통계뿐이라 테이블만 둘로 나뉜다. 모드 하나를 켜려면 두 곳을 맞춰야 한다 | 제외 |
| 4 | enum 유지 | 바꿀 것이 없다 | 배포 없이 바꾼다는 목표를 못 이룬다 | 제외 |

## 3. 설계

### 3.1 수집 유형 enum (core-enum)

모드마다 다르던 `resultType`·`teamCount`·`teamSize` 를 고정된 수집 유형 넷으로 묶는다. 유형별 규칙은 코드에, 모드 → 유형 대응은 DB 에 둔다.

| 유형 | 결과형/순위형 | 팀 수 × 팀당 인원 |
|---|---|---|
| 3대3 결과형 (`RESULT_3VS3`) | `RESULT` | 2 × 3 |
| 솔로 쇼다운 (`SOLO_SHOWDOWN`) | `RANK` | 10 × 1 |
| 듀오 쇼다운 (`DUO_SHOWDOWN`) | `RANK` | 5 × 2 |
| 트리오 쇼다운 (`TRIO_SHOWDOWN`) | `RANK` | 4 × 3 |

- 이름 제안: `StatsEventModeType`. 용어 사전에 등록된 단어(통계 stats·이벤트 event·모드 mode·유형 type·3대3 3vs3·솔로·듀오·트리오·쇼다운)만 쓴다. 최종 이름은 구현 때 `/superglossary:check` 로 확인한다.
- 지금의 `StatsEventMode`(모드별 목록)는 지운다. 목록은 DB 로 간다.
- `RESULT_SLOTS_PER_GAME = 6` 은 이 enum 옆으로 옮긴다. 결과형 유형이 `RESULT_3VS3` 하나뿐이라 "결과형은 모두 2×3" 전제가 구조로 지켜진다. 그래도 단위 테스트로 고정한다.
- 팀 구성 → 유형 조회 함수를 둔다(결과형/순위형 + 팀 수 + 팀당 인원 → 유형 또는 없음). 이 대응은 유일하다. core-batch 가 쓴다(§3.5).
- `BattleEventModeResultType`(RESULT/RANK)은 그대로 둔다.
- KDoc 에 스펙 A 의 결정 이력을 옮기고, 모드를 추가하는 절차를 새로 적는다.
  1. battlelog 표본으로 팀 구성을 확인한다.
  2. 관리자 화면에서 유형을 지정한다.
  3. 필요하면 프론트 `MODE_COLORS` 에 색을 더한다.
  4. 기존 유형에 맞지 않으면 새 유형이 필요하다. 이때는 코드 변경이다.

### 3.2 테이블 — `stats_event_mode` (db-core, statistics 소유)

```sql
create table if not exists stats_event_mode
(
    stats_event_mode_id      bigint generated always as identity primary key,
    event_mode_brawlstars_id bigint      not null,
    type                     varchar(25) not null,
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now()
);
create unique index if not exists uk_stats_event_mode__event_mode_brawlstars_id
    on stats_event_mode (event_mode_brawlstars_id);
```

- **행이 있으면 수집하고, 없으면 수집하지 않는다.** 유형 컬럼은 not null 이다. "수집 안 함" 을 뜻하는 값이나 기본값은 두지 않는다(§1.4).
- 컬럼 이름은 통계 전투 테이블의 `event_mode_brawlstars_id` 와 맞춘다.
- `battle_event_mode` 에 외래 키를 걸지 않는다. **(Claude 판단)** statistics 는 brawlstars 를 ID 로만 가리킨다. 지금도 enum 에 있으면 모드 행이 없어도 수집되므로 동작이 같다.
- 엔티티는 db-core `statistics` 패키지에 둔다. `BaseTimeEntity` 를 상속하고 `@Table` 에 유니크 인덱스를 적는다. 유형은 `@Enumerated(STRING)` 이다.
- `schema.sql` 에도 같은 정의를 더한다. **(Claude 판단)** 아래 12행 시드도 `schema.sql` 의 이 테이블 정의 바로 뒤에 같은 `insert ... on conflict do nothing` 으로 둔다.
  - 지금 `schema.sql` 에는 INSERT 가 없다. 다른 기준정보(모드·이벤트)는 관리자가 등록하기 때문이다.
  - 이 테이블이 비면 worker 가 아무것도 수집하지 않는다(§3.3). `schema.sql` 로 새 환경(로컬 포함)을 만들면 경고만 남고 수집이 0 이 된다.
  - 그래서 새 환경도 운영과 같은 12개로 시작하게 한다.

증분 마이그레이션 `V{구현일}__create_stats_event_mode.sql` 에 위 DDL 과 시드를 함께 둔다. 시드는 지금 허용 목록 12개다(`on conflict (event_mode_brawlstars_id) do nothing`).

| 모드 ID | 모드 | 유형 |
|---|---|---|
| 0 | 젬그랩 | RESULT_3VS3 |
| 2 | 하이스트 | RESULT_3VS3 |
| 3 | 바운티 | RESULT_3VS3 |
| 5 | 브롤볼 | RESULT_3VS3 |
| 17 | 핫존 | RESULT_3VS3 |
| 20 | 녹아웃 | RESULT_3VS3 |
| 22 | 농구 | RESULT_3VS3 |
| 45 | 하키 | RESULT_3VS3 |
| 48 | 아레나 | RESULT_3VS3 |
| 6 | 솔로 쇼다운 | SOLO_SHOWDOWN |
| 9 | 듀오 쇼다운 | DUO_SHOWDOWN |
| 38 | 트리오 쇼다운 | TRIO_SHOWDOWN |

와이프아웃(25)·와이프아웃 5v5(31)·태그팀/듀얼(24)·57 BRAWL HOCKEY 는 넣지 않는다(U5, 스펙 A 결정 1).

### 3.3 수집 필터 — core-worker

- `StatsBattleAggregateApplier` 의 `StatsEventMode.find(modeId)` 를 "로컬 스냅샷에서 모드 ID → 수집 유형 조회" 로 바꾼다. 팀 구성 검사와 결과형/순위형 경로 일치 검사는 유형이 가진 값으로 지금과 같이 한다. 다른 수집 조건(7일·친선전·브롤러 중복·최저 트로피)은 그대로 둔다. 여기에 "플레이어 한 명이 브롤러 여러 개를 낸 전투(듀얼처럼 BattleBuilder 가 펼친 전투, `Battle.brawlersExpanded`)는 유형과 관계없이 거른다" 를 더한다. 관리자가 듀얼에 3대3 유형을 매겨도 펼친 전투가 통계에 들어가지 않게 하려는 것이다.
- **로컬 스냅샷**
  - 테이블 전체(수십 행 이하)를 메모리 맵으로 들고 있다가 주기적으로 통째로 바꾼다.
  - 기동할 때 한 번 읽는다. **기동 때 못 읽으면 기동을 실패시킨다.** 조용히 빈 목록으로 수집을 0 으로 만드는 것보다 파드 재시작이 눈에 띈다(§1.4).
  - 주기 갱신이 실패하면 마지막 스냅샷을 유지하고 경고 로그를 남긴다.
  - 읽은 결과가 비어 있으면 경고 로그를 남긴다. 비어 있어도 동작은 "아무 모드도 수집 안 함" 이다.
  - 기동할 때와 내용이 바뀔 때 읽은 모드 수와 목록을 INFO 로그로 남긴다. 배포 후 확인과 관리자 변경 반영 확인에 쓴다.
  - 주기는 설정값으로 둔다. 기본값 제안은 5분이다(core-api 로컬 캐시 만료와 같다). worker 는 이미 `@EnableScheduling` 을 쓴다.
- 관리자가 바꾼 값은 파드마다 최대 한 주기 뒤에 반영된다. 반영 시점이 파드마다 조금씩 달라도 받아들인다. 그 사이 수집되는 전투는 각각 그 시점의 규칙으로 판정되며 오염이 아니다.
- 구현 방식(Caffeine 같은 캐시 라이브러리, `@Scheduled` + 원자적 교체 등)은 구현자가 정한다.

### 3.4 이벤트 모드 응답 — core-api

**(Claude 판단)** 이벤트 모드 응답의 `resultType` 은 통계 쪽 사실이다. 그래서 brawlstars 코드가 통계 테이블을 읽지 않게 한다. 대신 여러 도메인을 합쳐 응답을 만드는 api 계층에서 붙인다.

- DOMAIN.md 는 statistics → brawlstars 한 방향만 둔다. 지금 brawlstars 의 `BattleEventRepository` 가 `StatsEventMode` 를 읽는 것은 "core-enum 의 공유 규칙을 읽는 것은 의존으로 세지 않는다" 는 예외로 허용된다.
- 목록이 statistics 의 테이블로 가면 이 예외가 더는 적용되지 않는다. 반면 "여러 도메인의 데이터를 합쳐 응답을 만드는 계층(QueryComposer)" 은 의존으로 세지 않는다.
- 사용자가 처음에 모드 테이블이 통계를 아는 모양을 피하려 한 것과 같은 이유다.

바뀌는 곳:

- **statistics(core-api `application.statistics`)**
  - 통계 수집 모드 조회를 둔다(모드 ID → 수집 유형).
  - 테이블이 작으므로 전체를 로컬 캐시(Caffeine)에 담는다. 만료는 `BATTLE_EVENT` 와 같은 5분을 제안한다.
  - 새 캐시 이름은 `CacheConfig` 의 `CaffeineCacheManager` 이름 목록(지금 `BRAWLER`·`BATTLE_EVENT`)에 반드시 더한다.
    - 목록이 고정이라 빠진 이름은 Caffeine 이 만들지 않는다.
    - Redis 매니저는 `disableCreateOnMissingCache` 이다.
    - 합성 매니저는 `setFallbackToNoOpCache(false)` 다.
    - 그래서 목록에서 빠지면 캐시를 찾지 못해 런타임에 실패한다. CI 에는 Redis 가 없다는 점도 같이 고려한다.
- **brawlstars**
  - 도메인 `BattleEventMode` 에서 `resultType` 을 지운다.
  - `BattleEventRepository` 의 두 생성 지점에서 `StatsEventMode.find` 호출을 지운다.
  - 이로써 brawlstars 의 이벤트 모델은 통계를 모른다.
- **api 계층(controller·query)**
  - `BattleEventModeResponse` 를 만들 때 위 조회로 `resultType` 을 채운다.
  - `BattleEventModeResponse` 는 이벤트 응답(`BattleEventResponse`), 로테이션 응답(`RotationBattleEventResponse`), 전적 응답(`BattleResponse`) 안에서 만들어진다. 이 응답들을 만드는 경로(`BattleEventController` 와 전적 컨트롤러)가 조회를 받아 넘긴다.
  - QueryComposer 를 새로 둘지, 컨트롤러에서 합칠지는 구현자가 정한다.
- **응답 모양과 의미는 바뀌지 않는다.**
  - `resultType` 은 통계 수집 유형이고, 수집하지 않는 모드는 null 이다(스펙 A 결정 6).
  - 사용자 프론트의 API 연동은 바뀌지 않는다.
- `StatsRate.resultGameCount` 는 옮긴 `RESULT_SLOTS_PER_GAME` 을 참조하도록 import 만 바꾼다.

### 3.5 봇 판정·순위 집계 — core-batch (테이블을 읽지 않는다)

**(Claude 판단)** core-batch 는 테이블을 읽지 않고, 원본 통계 전투의 **팀 구성으로 유형을 정한다**. 이유는 셋이다.

- worker 가 유형과 팀 구성이 맞는 전투만 원본에 넣는다. 그래서 원본의 팀 구성은 유형을 정확히 가리킨다. 쇼다운 3종은 구성(10×1·5×2·4×3)이 서로 다르고, 결과형은 2×3 하나다.
- `StatsBotBattlePolicy` 는 전투 하나만 받는 순수 함수다. 집계기 5종(결과·적·2인·3인·순위)과 감시기 2종이 이 함수를 부른다. 스냅샷을 넘기려면 이 호출부를 모두 바꿔야 한다. 구성으로 정하면 시그니처가 그대로다.
- 스펙 A 결정 3(수집 시점에서 거르면 다운스트림은 자동 제한)과 맞는다.

바뀌는 곳:

- `StatsBotBattlePolicy.resultTeams`·`rankLobby`: `StatsEventMode.find` 대신 팀 구성 → 유형 조회(§3.1)를 쓴다. 쇼다운 임계값 `when` 은 모드 상수 대신 유형으로 분기한다. 그 밖의 판정은 그대로다.
- `StatsBotLobbyMonitor.SPECS`: 키를 모드 상수에서 유형으로 바꾼다.
- `BrawlerBattleRankStatsAggregator`: 순위형 판정을 구성 기반 유형 조회로 바꾼다. 이 필터는 옛 수집기가 넣은 20인 솔로 쇼다운(75, 20×1)과 10팀 듀오 쇼다운(86, 10×2)을 막으려고 있다. 두 구성 모두 어느 유형에도 맞지 않아 계속 걸러진다.
- KDoc 의 `[StatsEventMode]` 참조(`UnregisteredRotationEventAlertTask` 등)를 새 위치로 고친다.

이 판단의 결과로 생기는 동작:

- 관리자가 모드를 끄면 새 전투만 수집되지 않는다. 이미 원본에 들어간 그 모드의 전투는 보관 기간이 지나 사라질 때까지 집계에 계속 들어간다. 수집할 때는 유효했던 전투이므로 받아들인다. 보관 기간은 develop 이 10일이고, main 에 병합된 hotfix #85 는 45일이다.
- 원본에 허용 목록 밖 모드의 행이 있을 때(모드를 거르지 않던 옛 수집기가 쓴 행)
  - **결과형은 바뀌는 것이 없다.** 결과형 집계기 4종은 지금도 모드로 거르지 않는다. 2×3 이면 와이프아웃(25) 같은 목록 밖 모드도 집계된다. 바뀐 뒤 달라지는 점은 그런 행에도 3대3 봇 규칙이 걸린다는 것뿐이다. 지금은 enum 이 봇 규칙의 적용 범위만 정한다.
  - **순위형은 지금 enum 이 거른다**(`BrawlerBattleRankStatsAggregator`). 바뀐 뒤에는 구성이 유형과 같은 행만 들어간다. 20인 솔로(75, 20×1)와 10팀 듀오(86, 10×2)는 계속 걸러진다.
  - **(Claude 판단)** 별도 필터를 남기거나 배포를 미루지 않는다. 대신 배포 전에 운영 원본에서 순위형 행의 모드별 팀 구성을 한 번 조회한다. 시드 밖 모드인데 10×1·5×2·4×3 인 것이 없음을 확인하는 조회다(운영 DB 읽기라 사용자 허락을 받고 한다). 2026-11-09 원본의 순위형 모드는 6·75·9·86·38 뿐이었다. 그런 모드가 나오면 그때 대응을 다시 정한다.

### 3.6 관리자 — core-admin

- **(Claude 판단)** 통계 수집 모드를 읽고 쓰는 코드는 core-admin 의 `statistics` 패키지에 둔다. DOMAIN.md 는 지금 core-admin 전체를 brawlstars 로 보므로(operation 제외), 코드 목록에 "core-admin 의 statistics 패키지" 를 더한다(§5). 목록 응답은 모드(brawlstars)와 수집 유형(statistics)을 합치는 일이라 §3.4 와 같이 합치는 계층에서 붙인다.
- **목록 응답** `BattleEventModeResponse`: `resultType`(enum 에서 꺼낸 값) 대신 수집 유형(nullable)을 준다. 필드 이름은 구현자가 정한다. 관리자 화면은 "3대3 결과형 / 솔로 쇼다운 / 듀오 쇼다운 / 트리오 쇼다운 / 수집 안 함" 으로 읽는다.
- **유형 지정 API** (Claude 판단): `PUT /event-modes/{brawlStarsId}/stats-type`
  - 본문의 유형이 null 이면 행을 지우고, 값이 있으면 넣거나 바꾼다.
  - `battle_event_mode` 에 없는 모드면 404 다. 관리자 화면으로는 등록된 모드만 다루게 한다. 시드 12개는 이 검사를 거치지 않는다.
- core-admin 은 Deploy 워크플로 밖에서 로컬로 띄운다. 새 API 를 쓰려면 새 코드로 다시 띄운다.

### 3.7 관리자 프론트 — imstargg-admin-frontend

- `lib/types.ts`·`lib/api.ts`: 응답의 `resultType` 을 수집 유형으로 바꾸고, 유형 지정 API 함수를 더한다.
- `app/event-modes/page.tsx`: 목록의 유형 열이 수집 유형을 보여 준다.
- `app/event-modes/[brawlStarsId]/edit/page.tsx`: "통계 수집" 선택 영역을 더한다.
  - 선택지는 수집 안 함과 유형 넷이다.
  - **(Claude 판단)** 선택지마다 팀 구성을 함께 보여 준다(예: "3대3 결과형 (2팀 × 3명)"). 구성이 다른 모드(예: 5대5 인 31)에 유형을 잘못 매기면, worker 의 구성 검사가 그 모드의 전투를 모두 버려 수집이 0 이 된다. 듀얼처럼 한 명이 브롤러 여러 개를 내는 모드도 펼친 전투를 worker 가 걸러 수집이 0 이 된다. 그래서 선택할 때 구성을 보게 한다.
  - 저장한 뒤 "수집 서버에는 최대 몇 분 뒤 반영되고, 켠 모드의 통계는 다음 날 배치 뒤부터 보인다" 를 알린다(§3.8a, 문구는 구현 때 정한다).

### 3.8 사용자 프론트 — imstargg-frontend

- 코드 변경은 없다.
- `app/events/_components/modeStyles.ts` 의 "백엔드 `StatsEventMode` 에 모드를 추가할 때 여기에도 함께 추가" 주석을 "관리자 화면에서 통계 수집을 켠 모드는 여기에 색을 더한다. 없으면 기본 회색" 으로 고친다.

### 3.8a 관리자가 켜고 끌 때 화면에서 일어나는 일

**(Claude 판단)** 아래를 의도한 동작으로 둔다.

| 동작 | 이벤트 상세 (core-api 캐시 주기 5분 안) | "전체 이벤트" 맥락 (`/brawlers` 티어, 브롤러 상세의 조합·카운터) |
|---|---|---|
| **끈다** | `resultType` 이 null 이 되어 통계 패널 대신 "통계를 제공하지 않습니다" 안내가 뜬다 | 이미 수집한 원본은 보관 기간(develop 10일, main hotfix 45일) 동안 집계에 계속 들어가고(§3.5), 집계 행은 조회 범위를 벗어날 때까지 남는다 |
| **켠다** | `resultType` 이 채워져 통계 패널이 뜬다. worker 가 수집을 시작하고(최대 5분) 다음 통계 배치(02:00~09:00 KST)가 집계하기 전까지 패널은 비어 있다 | 다음 배치부터 섞이기 시작한다 |

- 끄기를 "앞으로 수집하지 않는다" 로만 정의하고, 이미 쌓인 통계를 지우거나 숨기지 않는다. 스펙 A 결정 9(과거 통계 행은 지우지 않는다)와 같다.
- 켠 직후 빈 패널이 보이는 시간은 길면 하루다. 관리자 화면의 저장 안내(§3.7)에 "통계는 다음 날 배치 뒤부터 보인다" 를 함께 적는다.

### 3.9 배포 순서·롤백 (스펙 A 와 같은 릴리즈)

이 설계는 스펙 A 와 같은 develop → main 릴리즈로 운영에 나간다(U6). 그래서 운영에서는 두 가지가 한 번에 바뀐다. 수집 모드 제한(스펙 A)이 처음 적용되고, 그 목록의 출처가 처음부터 DB 다.

같은 릴리즈의 다른 마이그레이션(V261102a·V261102b·V261104a·V261104b)은 각 스펙의 런북을 따른다. 이 절은 이 설계의 테이블 생성과 스펙 A 의 컬럼 삭제(V261103)를 어디에 끼울지만 정한다.

두 DDL 은 앱 배포를 사이에 두고 반대쪽에 놓인다.

| DDL | 앱 배포와의 순서 | 이유 |
|---|---|---|
| 이 설계의 `stats_event_mode` 생성 + 시드 | 앱보다 **먼저** | 새 core-worker·core-api 가 이 테이블을 읽는다 |
| 스펙 A 의 V261103 (`battle_event_mode` 통계 컬럼 삭제) | 앱보다 **나중** | 구 core-api·core-batch 가 그 컬럼을 읽는다. forward-only 라, 적용하면 이미지만 되돌리는 롤백이 막힌다 |

**순서**

1. **배포 직전 확인**: §3.5 의 순위형 팀 구성 조회. 운영 DB 읽기라 사용자 허락을 받고 한다. 운영 worker 는 3 전까지 모든 모드를 쓰므로 배포 직전에 조회한다.
2. **DDL ①: 테이블 생성 + 시드.** `V{구현일}__create_stats_event_mode.sql` 을 적용하고 12행을 확인한다.
   ```sql
   select event_mode_brawlstars_id, type from stats_event_mode order by 1;
   ```
   - 구 앱은 이 테이블을 모르므로 영향이 없다.
   - 같은 릴리즈의 "앱보다 먼저" DDL(V261102a·V261102b·V261104a)과 같은 창에서 적용해도 된다. 시간대는 스펙 B 와 같이 통계 배치 시간대(02:00~09:00 KST) 밖으로 잡는다.
3. **앱 배포**: core-api·core-worker·core-batch(Deploy 워크플로)를 배포하고, 프론트(Amplify)는 스펙 A·B 의 순서대로 core-api 뒤에 올린다.
   - 이때부터 worker 가 시드 12개 모드만 수집한다. 운영 기준으로는 처음이다.
   - 모든 core-api 파드와 core-batch CronJob 의 이미지 태그가 새 이미지로 동기화됐는지 확인한다. Deploy 워크플로 성공은 태그 커밋일 뿐이다(V261103 머리말).
4. **배포 후 확인**(§4)과 스펙 A 의 운영 작업(스펙 A §3.10 하키·아레나 맵 등록)을 한다.
5. **DDL ②: V261103 은 4 의 확인이 끝난 뒤 적용한다.** **(Claude 판단)**
   - 스펙 A 결정 10 은 앱 배포 직후 적용이었다. 이를 바꿔, V261103 을 미룬다. V261103 을 적용한 뒤에 롤백하면 지운 세 컬럼과 그 값을 백업에서 되살려야 하기 때문이다.
   - 미뤄도 비용이 없다. 새 앱은 세 컬럼을 읽지도 쓰지도 않는다. 컬럼마다 DB 기본값이 있어 새 core-admin 의 모드 INSERT 도 실패하지 않는다.
   - 적용 조건과 방법(lock_timeout 3s, 08:00 KST `battleEventModeSyncJob`·`battleEventSyncJob` 시각 피하기, 선택 백업)은 V261103 머리말을 따른다.
6. **core-admin 을 새 코드로 다시 띄운다.** 3 뒤라면 언제든 되고, 늦어도 5 직후에는 띄운다. V261103 뒤에는 예전 체크아웃의 core-admin 을 운영 DB 에 붙이지 않는다.

**순서가 어긋나면**

- 2 전에 3 을 하면, 새 core-worker 는 기동에 실패하고(§3.3) 새 core-api 는 이벤트·로테이션·전적 응답에서 실패한다. 그래서 2 가 관문이다. 스펙 A 결정 10(몇 분의 오류는 감수)과 달리, worker 가 기동하지 못하는 것은 받아들이지 않는다.
- 3 전에 5 를 하면, 구 core-api 의 이벤트 조회와 구 core-batch 의 08:00 잡이 실패한다(V261103 머리말).

**롤백**

- **롤백은 릴리즈 전체 단위로 한다.**
  - 이미지만 되돌리는 롤백은 어느 시점에도 없다. 같은 릴리즈의 스펙 B(V261104a)가 적용되면, 구 core-api·core-batch 는 조합 테이블에서 실패하기 때문이다.
  - 그래서 스펙 B 의 롤백 절차(스펙 B §3.8)를 함께 따른다. 스펙 B 는 V261104b 전까지만 되돌릴 수 있다.
- **이 설계와 스펙 A 쪽에서 더할 일**
  - `stats_event_mode` 는 남겨도 무해하다. 구 이미지는 이 테이블을 모른다.
  - 5(V261103) 전에 되돌리면, `battle_event_mode` 의 세 컬럼과 값이 그대로 있어 스펙 A 쪽은 따로 복구할 것이 없다. V261103 을 미루는 이득이 이것이다.
  - 5 뒤에 되돌리면, V261103 머리말의 절차(컬럼 재생성 → 백업에서 값 복원)를 구 이미지 배포 전에 더한다.
  - 구 worker 는 다시 모든 모드를 수집한다.
- 이 설계만 되돌리고 스펙 A 는 남기는 롤백은 따로 두지 않는다. 같은 릴리즈의 이미지에 둘이 함께 들어 있다.

마이그레이션은 운영 DB 에 수동으로 적용한다. 멱등이어야 한다(`create ... if not exists`, `on conflict do nothing`).

## 4. 테스트·검증

**백엔드**

- core-enum
  - 결과형 유형의 `teamCount × teamSize` 가 `RESULT_SLOTS_PER_GAME` 과 같다.
  - 팀 구성 → 유형 조회가 네 유형을 돌려준다. 20×1·10×2·2×5·2×1 같은 구성은 없음이다.
  - 유형 집합을 고정한다.
  - `StatsEventModeTest` 는 지운다. 12개 목록 고정은 이제 시드와 배포 후 확인(§3.9)이 맡는다.
- db-core: 엔티티 단위 테스트는 두지 않는다. 라이브러리·DB 기능(저장·유니크 제약)은 테스트하지 않는다는 컨벤션을 따른다. 매핑은 core-worker·core-api·core-admin 의 컨텍스트 테스트가 H2 로 거친다. 테스트 DB 는 H2 라 엔티티로 테이블이 생기고, 마이그레이션은 실행되지 않는다.
- core-worker
  - `StatsBattleAggregateApplierTest` 의 기존 사례(허용 모드 저장, 목록 밖 모드 거름, modeId 기준 판정, 구성 불일치, 유형 불일치, 쇼다운 3종)를 스냅샷 입력으로 그대로 통과시킨다.
  - 스냅샷에 없는 모드는 저장되지 않는다.
  - 스냅샷이 갱신되면 새 규칙이 적용된다.
  - 갱신이 실패하면 마지막 스냅샷이 유지된다.
  - 기동할 때 읽지 못하면 실패한다.
  - 빈 스냅샷이면 경고하고 아무것도 저장하지 않는다.
- core-batch
  - `StatsBotBattlePolicyTest`·`StatsBotLobbyMonitorTest`·`BrawlerBattleRankStatsAggregatorTest`·`BrawlerBattleRankStatsTaskTest` 를 모드 상수 대신 유형·구성 기준으로 고친다.
  - 순위형 유형이 모두 봇 판정과 감시에 드는지 고정한다. 지금의 "순위형 허용 모드" 테스트를 유형 기준으로 바꾼 것이다.
  - 75(20×1)·86(10×2) 구성이 순위 집계에서 빠진다.
  - 픽스처(`StatsBattleFixtures`)의 `StatsEventMode.*.modeBrawlStarsId` 기본값은 모드 ID 숫자로 바꾼다.
- core-api
  - 응답을 합치는 계층의 매핑을 확인한다. 테이블에 있는 모드는 그 유형의 RESULT/RANK 이고, 없는 모드는 null 이다. 이벤트·로테이션·전적 응답 모두 확인한다.
  - brawlstars 의 `BattleEventMode` 와 `BattleEventRepository` 가 통계 코드를 참조하지 않는다.
  - 통계 수집 모드 캐시를 거치는 조회를 컨텍스트 테스트에서 한 번 부른다. 캐시 이름을 `CaffeineCacheManager` 목록에 빠뜨리면 여기서 드러난다(§3.4).
- core-admin
  - 통계 수집 모드 서비스 테스트: 유형 지정·변경·해제(null), 등록되지 않은 모드 404.
  - 모드 목록 컨트롤러 테스트: 목록에 수집 유형이 붙는다.
  - `BattleEventServiceTest` 의 기존 resultType 사례는 지운다. 모드 서비스는 이제 유형을 모르고, 목록에 유형을 붙이는 일은 컨트롤러가 한다(§3.6).
- `./gradlew ktlintCheck`, `./gradlew test`.
- 마이그레이션 SQL 은 일회용 `postgres:17-alpine` 컨테이너에 두 번 적용해 멱등과 12행을 확인한다. `schema.sql` 만으로 만든 빈 DB 에도 12행이 들어가는지 같은 컨테이너에서 확인한다(§3.2).

**프론트**

- imstargg-admin-frontend: CI 테스트가 없다. 로컬에서 build·lint 를 돌리고 모드 목록과 편집 화면을 직접 확인한다.
- imstargg-frontend: 주석만 바뀐다.

**배포 후 확인**

- **목록 밖 모드 차단**: 새 worker 배포 시각 뒤에 적재된 행(`created_at` 기준)의 `event_mode_brawlstars_id` 가 시드 12개 값만 갖는지 확인한다. `battle_date` 로 자르지 않는다. 최대 7일 늦게 수집된 전투가 섞이기 때문이다.
- **적재량**: 운영에 허용 목록이 처음 적용되므로 전체 적재량은 줄어든다.
  - 순위형은 2026-11-09 기준 약 35%(75·86)가 빠진다고 예상한다.
  - 결과형은 스펙 A 측정으로 약 2% 준다.
  - 그래서 전체량 대신 **시드 모드별 시간당 적재량**을 배포 전 같은 시간대와 비교한다(`created_at` 기준). 줄어드는 것은 시드 밖 모드뿐이어야 한다. 시드 모드 쪽이 급감하면 worker 스냅샷 로그를 본다.
- **시드 모드 등록**: 시드 12개 모드가 모두 `battle_event_mode` 에 등록돼 있는지 확인한다. 없는 모드는 관리자 화면 목록에 안 보이고, 유형 지정 API 가 404 라 끌 수 없다.
- **worker 스냅샷**: 기동 로그에 12개 모드를 읽었다는 기록이 있고, 경고 로그가 없다.
- **스펙 A 배포 후 확인**(스펙 A §5)도 이 릴리즈에서 한다. 하키·아레나 맵 등록 뒤 로테이션·상세 노출, 미지원 이벤트 상세의 안내, 카드 색이 대상이다.
- 관리자 화면에서 한 모드를 껐다가 다시 켜 보고, 주기 안에 수집이 멈췄다가 돌아오는지 확인한다. 운영에서 해 볼지는 배포 담당자가 정한다.

## 5. 문서·버전

- `docs/superdomain/DOMAIN.md`(statistics)
  - 통계 수집 모드는 관리자가 정한다는 점을 규칙에 더한다. "결과형 모드는 모두 3대3" 규칙은 유형으로 지켜진다고 고친다.
  - 코드 목록에 core-admin 의 `statistics` 패키지를 더한다(§3.6).
  - 클래스 이름은 넣지 않는다.
- `imstargg-backend/docs/stats-battle-data-strategy.md`: "허용 모드와 모드별 팀 구성은 `StatsEventMode`(core-enum)가 단일 출처" 를 "모드 → 수집 유형은 통계 수집 모드 테이블, 유형별 팀 구성은 수집 유형 enum" 으로 고친다. 봇 판정 절의 "모드가 `StatsEventMode` 의 결과형 모드다" 도 고친다.
- KDoc: 수집 유형 enum(결정 이력과 추가 절차), worker 스냅샷(기동 실패·갱신 실패·빈 목록 동작), core-batch 의 구성 기반 판정 이유(§3.5).
- 스펙 A 문서 머리에 "결정 2 는 2026-11-13 통계 수집 모드 DB 관리 설계로 대체" 를 한 줄 적는다.
- PR 단계에서 `/release` 로 core-api·core-worker·core-batch 버전을 올린다. 프론트는 주석만 바뀌어 산출물이 같다. core-admin·admin-frontend 는 버전 관리 대상이 아니다.

## 6. 구현자에게 맡긴 것

- 수집 유형 enum·엔티티·API 필드의 최종 이름(용어 사전 확인)
- worker 스냅샷 구현 방식과 갱신 주기 설정 이름
- core-api 통계 수집 모드 조회의 캐시 구성, 그리고 응답을 합치는 위치(QueryComposer 신설 또는 컨트롤러)
- 관리자 화면 문구와 배치
- 마이그레이션 파일 날짜
