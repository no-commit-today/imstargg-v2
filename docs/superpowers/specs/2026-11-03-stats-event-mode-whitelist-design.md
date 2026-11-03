# 통계 대상 모드 정리 설계 (스펙 A)

- 작성일: 2026-11-03
- 대상: `core/core-enum`, `core/core-worker`(통계 전투 수집), `core/core-api`(이벤트 모드 응답·게임 수 환산), `core/core-admin`, `infrastructure/db-core`(`battle_event_mode`), `imstargg-frontend`, `imstargg-admin-frontend`, `imstargg-backend/docs`
- 상태: 설계 확정 (구현 전)
- 관련: 같은 검토에서 나온 스펙 B(조합 통계 재구성 — 보존 정책·이벤트 키·3인 조합)와 스펙 C(티어 스코어 개선)는 이 스펙 다음에 따로 작성한다. 두 스펙의 입력이 되는 데이터 근거는 [부록](#부록-통계-검토-근거-2026-11-03)에 남긴다.

## 1. 배경

통계 수집·표시 방식을 네 주제(이벤트 모드 제한, 모드 색상, 조합 통계, 티어 스코어)로 검토했다. 그중 이 스펙은 "어떤 모드의 전투를 통계로 모으는가"와 그에 딸린 모드 메타데이터를 다룬다. 운영 DB(읽기 전용)로 확인한 사실이다.

- **수집 단계에 모드 제한이 없다.** `StatsBattleAggregateApplier` 는 7일 이내·친선전 제외·브롤러 중복 없음·최저 트로피(1000) 또는 경쟁전 신화 이상만 본다. 결과형/순위형은 응답에 `result`·`battleRank` 가 있는지로 나뉜다.
- **결과형 전투의 약 91% 가 핵심 6개 모드의 3v3** 이다(2026-10-27~11-02, 2% 표본). 젬그랩(0)·하이스트(2)·바운티(3)·브롤볼(5)·핫존(17)·녹아웃(20). 나머지는 Brawl Hockey(45) 3.6%, Brawl Arena(48) 2.5%, 농구(22) 1.1%, 태그팀/듀얼(24) 1.1%, 와이프아웃·5v5 합 0.8% 다. 순위형은 솔로 69%·듀오 23%·트리오 쇼다운 8% 다.
- **battlelog 의 mode 문자열을 믿을 수 없다.** modeId 45(하키)는 `brawlBall`, 48(아레나)은 `siege` 로 온다. 거르는 기준은 `event.modeId` 여야 한다.
- **`battle_event_mode` 의 통계용 메타데이터가 관리되지 않는다.** 68개 모드 중 `player_counts` 는 20인 보스전(`[20]`) 하나만 빼고 전부 기본값 `[3, 3]` 이다(5v5·2v2·듀얼 포함). `color` 는 26개가 기본 회색이고, 모드 24 는 DB 이름이 `DUELS` 인데 로테이션 API 는 `tagTeam` 이라고 한다. 사용자는 색을 정확히 뽑기 어렵고 팀 인원 관리가 번거롭다고 했다.
- **듀얼 집계 오류.** `BattleBuilder` 는 듀얼에서 한 플레이어의 브롤러 3개를 같은 태그로 3명처럼 펼친다. 이것이 조합 통계에 팀원으로 들어간다.
- **`player_counts` 의 쓰임은 하나다.** core-api 가 결과형 픽률 분모(게임 수)를 "이벤트별 슬롯 합 ÷ 1게임당 슬롯" 으로 환산할 때(`GameSlotRepository` → `GameSlots.totalGameCount`)만 쓴다. 운영 값이 사실상 전부 `[3, 3]` 이라 지금도 6 으로 나누고 있다.
- **`result_type`·`color` 의 쓰임.** `result_type` 은 프론트 이벤트 상세가 결과형/순위형 API 중 어느 것을 부를지와 배지를 고르는 데 쓴다. `color` 는 로테이션·경쟁전 카드 머리 배경에만 쓴다.

## 2. 결정 사항 (사용자 합의)

| # | 결정 | 이유 |
|---|---|---|
| 1 | 통계 수집 모드 = **핵심 6(3v3 결과형) + 솔로·듀오·트리오 쇼다운(순위형)** 9개 | 검증되고 대부분의 전투를 차지하는 모드만. 하키·아레나는 제외(맵이 이벤트 테이블에 미등록) |
| 2 | 허용 목록은 **코드 enum** 으로 둔다 | 관리자 데이터 관리 부담 제거. 모드 추가는 드물고 코드 한 줄 |
| 3 | **수집 시점(core-worker)** 에서 거른다 | 다운스트림(배치 5종·API)이 코드 변경 없이 자동 제한. 저장 공간 절약. 집계·조회 시점 필터보다 고칠 곳이 적다 |
| 4 | 팀 구성(팀 수 × 팀당 인원)도 enum 과 정확히 같아야 수집한다 | 예외 구성 전투 방어. 최근 순위형 표본은 100% 세 구성 중 하나 |
| 5 | `battle_event_mode` 에서 `result_type`·`player_counts`·`color` 컬럼 제거 | 결과형/순위형은 enum 이 단일 출처. 게임 수는 3v3 상수. 색은 프론트로 |
| 6 | 유형 enum 이름 `BattleEventModeResultType` 과 API 필드 `mode.resultType` 은 **이름 유지, nullable 로만 변경**(null = 통계 미지원 모드) | 결과형/순위형은 모드 자체의 성질이다. 통계 지원 여부는 `StatsEventMode` 소속으로 표현된다. 변경 범위가 가장 작다 |
| 7 | 미지원 모드 이벤트는 로테이션·검색에 **그대로 노출**하고, 이벤트 상세의 통계 영역만 안내로 바꾼다 | 맵 정보·전투 기록은 유지 |
| 8 | 카드 배경색은 **지원 9개 모드만 프론트 상수**(modeId → 색), 나머지는 기본 회색 | 지원 모드가 고정이라 관리할 것이 거의 없다 |
| 9 | 과거 통계 행은 지우지 않는다 | 조회 범위가 최대 약 2개월(`SINCE_LAST_MONTH`)이라 자연히 밀려나고, 스펙 B 의 보존 정책이 정리한다. 지우려면 이벤트→모드 매핑이 필요한데 그 매핑이 불완전하다 |
| 10 | 배포 순서 관문을 두지 않는다 | 사용자 판단 — 몇 분의 오류는 감수한다. 컬럼 DROP 은 코드 배포 직후 적용 |

## 3. 설계

### 3.1 허용 목록 enum — `StatsEventMode` (core-enum)

```kotlin
enum class StatsEventMode(
    val modeBrawlStarsId: Long,
    val resultType: BattleEventModeResultType,
    val teamCount: Int,
    val teamSize: Int,
) {
    GEM_GRAB(0, RESULT, 2, 3),
    HEIST(2, RESULT, 2, 3),
    BOUNTY(3, RESULT, 2, 3),
    BRAWL_BALL(5, RESULT, 2, 3),
    HOT_ZONE(17, RESULT, 2, 3),
    KNOCKOUT(20, RESULT, 2, 3),
    SOLO_SHOWDOWN(6, RANK, 10, 1),
    DUO_SHOWDOWN(9, RANK, 5, 2),
    TRIO_SHOWDOWN(38, RANK, 4, 3),
    ;

    companion object {
        fun find(modeBrawlStarsId: Long): StatsEventMode? = ...
    }
}
```

- 값의 표기(named arguments 여부)는 프로젝트 규칙을 따른다. 인자가 3개 이상이므로 생성자 호출에 이름을 붙인다.
- KDoc 에 결정 이력을 남긴다. 2026-11 기준 핵심 6개가 결과형의 91% 라는 점, mode 문자열을 믿을 수 없다는 점(45→`brawlBall`, 48→`siege`), `player_counts` 가 관리되지 않았다는 점, 듀얼 펼침 오류다.
- 모드를 추가하는 절차도 KDoc 에 적는다. ① 운영 `stats_*_battle` 이 아닌 battlelog 표본으로 팀 구성을 확인한다(추가 전에는 수집되지 않으므로). ② 항목을 추가한다. ③ 프론트 `MODE_COLORS` 에 색을 추가한다. ④ 결과형이면 반드시 2×3 이어야 한다(§3.3 상수의 전제).
- `BattleEventModeResultType`(RESULT/RANK)은 이름 그대로 둔다. 엔티티 컬럼이 사라진 뒤에는 이 enum 과 API 응답에서만 쓰인다.

### 3.2 수집 필터 — `StatsBattleAggregateApplier` (core-worker)

`canCreateStatsBattle` 에 조건을 더한다. 기존 조건(이벤트 필드 존재, 친선전 제외, 브롤러 중복 없음, 최저 트로피)은 그대로 둔다.

1. `StatsEventMode.find(event.modeBrawlStarsId)` 가 null 이면 버린다.
2. `battle.teams` 의 팀 수가 `teamCount` 이고 모든 팀의 인원이 `teamSize` 여야 한다.
3. 경로별 유형 일치:
   - 순위형(`canCreateRankBattle`): `mode.resultType == RANK` 이고 `battleRank != null`.
   - 결과형(`canCreateResultBattle`): `mode.resultType == RESULT` 이고 `result != null`.
   - 지금은 필드 존재만 보므로 한 전투가 두 테이블에 동시에 들어갈 수 있다. 이 구멍도 함께 막힌다.

효과:

- `stats_result_battle`·`stats_rank_battle` 에 9개 모드만 들어간다. core-batch 집계 5종(`brawler_battle_result_stats`·`brawler_battle_rank_stats`·`brawler_enemy_battle_result_stats`·`brawler_pair_battle_result_stats`·`brawler_pair_battle_rank_stats`)과 core-api 는 코드 변경 없이 자동으로 제한된다.
- 듀얼(24)이 빠지므로 듀얼 펼침이 조합 통계를 오염시키던 문제가 사라진다. `BattleBuilder` 의 펼침 자체는 전투 기록 표시용이라 그대로 둔다.
- 통계 전투 적재량이 약 9% 준다(결과형 기준).

### 3.3 결과형 게임 수 환산 — 상수화 (core-api)

- `GameSlot`·`GameSlots`·`GameSlotRepository` 와 그 테스트(`GameSlotTest`·`GameSlotsTest`·`GameSlotRepositoryTest`)를 지운다.
- 결과형 총 게임 수 = **조회 범위의 결과형 슬롯 합 ÷ 6**. 6 은 "결과형 지원 모드는 모두 2×3" 이라는 §3.1 의 전제에서 나온다. 상수는 결과형 통계 도메인(`BrawlerResultStatistics` 또는 `StatsRate` 근처)에 한 곳만 두고, 이 전제를 KDoc 으로 연결한다. enum 테스트가 전제를 고정한다(§5).
- 사용처: `BattleResultStatisticsRepository.computeTotalGameCount`(결과·조합·카운터 픽률 분모 공통), `BrawlerResultStatistics.create`.
- 지금은 이벤트별로 `slot_e / 6` 을 정수 나눗셈한 뒤 더한다. 새 식은 합한 뒤 한 번 나눈다. 차이는 조회 범위의 이벤트 수 이하로 무시할 수 있으며, 테스트 기대값만 맞춘다.
- tier 구간은 슬롯마다 그 플레이어의 트로피로 정해지므로, 상위 구간에서는 한 게임이 6 슬롯보다 적게 들어갈 수 있다. "슬롯 합 ÷ 6" 은 그때도 근사값이다. 지금과 같은 근사이며 이 스펙에서 바꾸지 않는다.
- Redis `statistics` 캐시 객체 형태가 바뀌지 않으므로 캐시 키 버전을 올리지 않는다. `StatisticsCacheKeyTest` 는 그대로 통과해야 한다.
- `StatsRate.pickRate` KDoc 의 "환산 규칙은 [GameSlots.totalGameCount] 참조" 를 새 상수 위치로 고친다.

### 3.4 스키마 — `battle_event_mode` (db-core)

- `BattleEventModeEntity` 에서 `resultType`·`playerCounts`·`color` 필드, `updateResultType`·`updatePlayerCounts`·`updateColor`, 검증 함수, `DEFAULT_PLAYER_COUNTS`·`DEFAULT_COLOR` 를 지운다. 테스트 픽스처(`BattleEventModeEntityFixture`)도 맞춘다.
- `schema.sql` 의 `battle_event_mode` 정의에서 세 컬럼을 지운다.
- 증분 마이그레이션 `V261103__drop_battle_event_mode_stats_columns.sql`(구현일에 맞춰 날짜 조정):

  ```sql
  alter table battle_event_mode
      drop column if exists result_type,
      drop column if exists player_counts,
      drop column if exists color;
  ```

- 코드 배포 직후 수동으로 적용한다. 세 컬럼 모두 DB 기본값이 있어, 그 사이 새 코드의 INSERT 는 실패하지 않는다. 반대로 DDL 이 먼저 적용되면 구버전 앱의 조회가 잠시 실패한다(결정 10 으로 감수).

### 3.5 이벤트 모드 응답 — core-api

- 도메인 `BattleEventMode`(`core/domain/brawlstars/event`)에서 `playerCounts`·`color`·`DEFAULT_PLAYER_COUNTS` 를 지운다. `resultType` 은 `BattleEventModeResultType?` 로 바꾸고 `StatsEventMode.find(id)?.resultType` 으로 정한다. 매핑은 `BattleEventRepository` 의 두 `BattleEventMode(...)` 생성 지점에서 한다.
- `BattleEventModeResponse`: `color` 를 지우고 `resultType` 을 nullable 로 바꾼다. KDoc 에 "null = 통계를 모으지 않는 모드" 를 적는다.
- 같은 응답을 품는 곳(로테이션, 이벤트 상세, 경쟁전 맵, 이벤트 검색, 전투 기록의 `BattleResponse`)에 그대로 반영된다.
- `BATTLE_EVENT` 캐시는 로컬(Caffeine)이라 형태가 바뀌어도 재시작 때 비워진다. 버전 prefix 가 필요 없다.
- 통계 API 는 미지원 eventId 요청도 그대로 받는다(프론트가 보내지 않으므로 막지 않는다).

### 3.6 관리자 API — core-admin

- `BattleEventModeCreateRequest`: `resultType`·`playerCounts`·`color` 를 지운다. 남는 필드는 `brawlStarsId`·`brawlStarsName`·`names` 다.
- `BattleEventModeUpdateRequest`·`BattleEventModeColorUpdateRequest` 와 `PATCH /event-modes/{brawlStarsId}`·`PUT /event-modes/{brawlStarsId}/color`, `BattleEventService.updateMode`·`updateModeColor` 를 지운다.
- `BattleEventModeResponse`(관리자): `playerCounts`·`color` 를 지우고, `resultType` 은 `StatsEventMode.find(brawlStarsId)?.resultType` 으로 채우는 nullable 읽기 전용 값으로 바꾼다. 운영자는 "통계 지원: 결과형/순위형/미지원" 으로 본다.

### 3.7 사용자 프론트 — imstargg-frontend

- **타입·매퍼**: `src/api/types.ts`·`src/domain/event/types.ts`·`src/domain/event/mappers.ts` 에서 `color` 를 지우고 `resultType` 을 `"RESULT" | "RANK" | null` 로 바꾼다. 타입 주석에 null 의 의미를 적는다. 전투 기록 매퍼(`src/domain/battle/mappers.ts`)의 `modeResultType` 은 읽는 컴포넌트가 없으므로 지운다.
- **카드 색**: `app/events/_components/modeStyles.ts`
  - `MODE_COLORS: Record<number, string>` 상수를 둔다. 값은 현재 DB 값을 옮긴다.

    | 모드 (modeId) | 색 |
    |---|---|
    | 젬그랩 0 | `#8F43EA` |
    | 하이스트 2 | `#C763CD` |
    | 바운티 3 | `#FFC60A` |
    | 브롤볼 5 | `#909FDB` |
    | 핫존 17 | `#FF8A1F` |
    | 녹아웃 20 | `#E8893A` |
    | 솔로·듀오·트리오 쇼다운 6·9·38 | `#95D449` |

  - `getModeStyle(modeId: number | null | undefined)` 로 바꾼다. 목록에 없으면 기존 `DEFAULT_STYLE`(`#2A2E38`) 이다. 명도로 글자색을 고르는 `pickFg` 는 유지한다.
  - 상수 옆 주석에 "백엔드 `StatsEventMode` 에 모드를 추가할 때 함께 추가" 라고 적고, "색 단일 출처는 백엔드" 라는 기존 주석을 고친다.
  - `RotationCard`·`SoloRankCard` 는 `event.mode?.id` 를 넘긴다.
- **이벤트 상세** (`app/events/[id]/`)
  - `_components/EventStatsSection.tsx`·`src/hooks/queries/useEventStats.ts`·`_components/EventStatsPanel.tsx`·`_components/EventSelectedBrawlerPanel.tsx`: `resultType` 이 RESULT 면 결과형, RANK 면 순위형 통계를 보여 준다(지금과 같다). **null 이면 통계 API 를 부르지 않고** 통계 영역 자리에 "이 모드는 통계를 제공하지 않습니다" 안내를 띄운다(문구는 구현 때 다듬는다).
  - `_components/EventHero.tsx` 배지와 `page.tsx` 라벨은 `resultType` 이 있을 때만 결과형/순위형을 표시한다.
  - `mode` 자체가 null 인 이벤트(모드 미등록)도 같은 안내 경로를 탄다.

### 3.8 관리자 프론트 — imstargg-admin-frontend

- `lib/types.ts`·`lib/api.ts`: 생성 요청과 응답에서 `playerCounts`·`color` 를, 생성 요청에서 `resultType` 을 지운다. 유형·인원 수정과 색 수정 API 함수를 지운다.
- `app/event-modes/page.tsx`: 생성 폼에서 세 입력을 지운다. 목록의 유형 열은 응답의 nullable `resultType` 을 "결과형/순위형/미지원" 으로 읽기 전용 표시한다.
- `app/event-modes/[brawlStarsId]/edit/page.tsx`: 유형·인원·색 편집 영역을 지운다. 이름·이미지 편집은 남긴다.

## 4. 바꾸지 않는 것 (비목표)

- core-batch 집계 코드와 `battleEventModeSyncTask`(새 모드 알림)·`battleEventSyncJob`(미등록 이벤트 알림). 미지원 모드의 미등록 이벤트 알림이 계속 올 수 있지만 이번 범위가 아니다.
- `BattleBuilder` 의 듀얼 펼침(전투 기록 표시에 필요).
- 과거 통계 행 정리(결정 9). 미지원 모드의 과거 행은 최대 약 2개월 동안 "전체 이벤트" 맥락(`/brawlers`·브롤러 상세의 조합·카운터)에 섞여 있다가 사라진다.
- 조합 통계의 이벤트 키·보존 정책·3인 조합(스펙 B), 티어 스코어 산식(스펙 C).

## 5. 테스트·검증

**백엔드**

- core-enum `StatsEventModeTest`: `find` 결과, modeId 중복 없음, **모든 RESULT 항목이 2×3**(게임 수 상수 6 의 전제 고정).
- core-worker `StatsBattleAggregateApplierTest` 에 다음 사례를 추가한다.
  - 허용 목록 밖 모드는 두 테이블 어디에도 저장되지 않는다. 하키(modeId 45, mode `brawlBall`)와 듀얼(24, 펼친 3명 팀)을 넣는다.
  - 핵심 모드라도 팀 구성이 다르면(예: 2팀 × 2명) 버린다.
  - 유형이 어긋나면(핵심 모드인데 `battleRank` 만 있음, 쇼다운인데 `result` 만 있음) 버린다.
  - 솔로(10×1)·듀오(5×2)·트리오(4×3) 쇼다운은 순위형으로 저장된다.
- core-api
  - `BattleEventRepository` 매핑: 지원 모드는 RESULT/RANK, 미지원 모드는 null.
  - `BattleEventModeResponse` 직렬화(`resultType: null`, `color` 없음).
  - `BrawlerResultStatisticsTest` 등 `GameSlots` 를 쓰던 테스트를 상수 6 기준으로 고친다. `StatisticsCacheKeyTest` 는 바뀌지 않는다.
- core-admin `BattleEventServiceTest`: 생성 요청 축소, 목록 응답의 `resultType` 이 enum 에서 오는지 확인.
- `./gradlew ktlintCheck`, `./gradlew test`.
- 마이그레이션 SQL 은 일회용 `postgres:17-alpine` 컨테이너에서 `schema.sql` 의 이전 정의로 테이블을 만든 뒤 두 번 적용해 멱등을 확인한다. 테스트 DB 가 H2(엔티티로 DDL 생성)라 테스트로는 드러나지 않는다.

**프론트**

- imstargg-frontend: `tests/components/EventStatsPanel.test.tsx`·`EventStatsTables.test.tsx` 등 픽스처를 고친다. 새로 넣을 테스트는 두 가지다. `resultType` 이 null 이면 안내가 뜨고 통계 요청이 나가지 않는지, `getModeStyle` 의 매핑과 기본값이다. 단위·컴포넌트 테스트, 타입 체크, lint 를 돌린다.
- imstargg-admin-frontend: CI 테스트가 없으므로 로컬 build·lint 후 모드 목록·생성·편집 화면을 직접 확인한다.

**배포 후 확인**

- 최신 `stats_result_battle`·`stats_rank_battle` 파티션의 `event_mode_brawlstars_id` 가 9개 값만 갖는지 DB 로 확인한다.
- 미지원 이벤트(예: 농구 맵) 상세에 안내가 뜨고, 지원 이벤트 카드 색이 이전과 같은지 확인한다.

## 6. 문서·버전

- `imstargg-backend/docs/stats-battle-data-strategy.md` 의 수집 조건에 허용 목록·팀 구성 검사를 추가한다.
- `StatsEventMode` 결정 이력 KDoc(§3.1), 게임 수 상수 KDoc(§3.3), `BattleEventModeResponse` 의 null 의미 KDoc(§3.5).
- PR 단계에서 `/release` 스킬로 core-api·core-worker·core-batch·frontend 버전을 올린다(core-batch 는 db-core 엔티티 변경으로 산출물이 바뀐다). core-admin·admin-frontend 는 버전 관리 대상이 아니다.

## 7. 위험과 대응

| 위험 | 대응 |
|---|---|
| 브롤스타즈가 핵심 모드의 modeId 를 바꾸거나 새 변형을 같은 ID 로 낸다 | 팀 구성 검사가 다른 구성의 전투를 막는다. `battleEventModeSyncTask` 의 새 모드 알림으로 인지한다 |
| 핵심 모드가 일시적으로 다른 인원(예: 이벤트성 2v2)으로 열린다 | 별도 modeId(46·49~53)로 오므로 허용 목록 밖이다 |
| 순위형 전투의 팀 구성 예외(탈주·봇 등) | 최근 표본은 100% 정상 구성이었다. 배포 후 적재량을 이전과 비교해 급감하면 검사를 다시 본다 |
| 컬럼 DROP 과 배포 사이 몇 분의 오류 | 결정 10 으로 감수 |

## 부록: 통계 검토 근거 (2026-11-03)

스펙 B·C 의 입력이다. 수치는 운영 DB 읽기 전용 조회로 얻었고, 분석 스크립트는 세션 임시 폴더에만 있었으므로 결론과 수치를 여기에 옮긴다.

### A.1 수집량 기준선

- 2026-10-30 배포 `c08e0c63`(통계 전투 수집량 10배 확대)이 직전 7일치 전투까지 소급 수집했다. 그래서 battle_date 10-26 부터 볼륨이 올라간다. 예: 조합 TROPHY_1000+ 관측은 10-19 주 1.16M → 10-26~31 10.9M.
- 10-26 이전 주는 다른 구간이다. 그 주를 기준으로 한 시간 분할 분석은 표본 부족으로 무효다(1차 분석에서 "조합의 이벤트 분리는 무의미" 로 잘못 결론 냈다가 정정했다).

### A.2 조합·카운터의 이벤트 분리 (스펙 B)

반분 검증: 수집 플레이어 태그 해시로 전투를 둘로 나눴다(10-27~11-01, 10-31 제외, 핵심 6모드 3v3). 한쪽 추정으로 다른 쪽 승률을 예측해 global 대비 MSE 개선(pp²)을 쟀다.

| 유형 | global 대비 개선: 모드 | global 대비 개선: 이벤트→모드 수축 | 이벤트−모드 실제 SD(반분 상관) |
|---|---|---|---|
| 트로피전 pair | 8.74 | 20.17 | 4.20pp (0.74) |
| 트로피전 enemy | 8.82 | 19.61 | 3.62pp (0.71) |
| 경쟁전 pair | 5.82 | 6.96 | 2.02pp (0.32) |
| 경쟁전 enemy | 7.43 | 8.54 | 1.82pp (0.31) |

- 트로피전은 맵 효과가 실재하고, 경쟁전은 이득의 약 85% 가 모드로 설명된다.
- 고표본 셀(이벤트 n≥300, 트로피전 pair 게임의 32%)에서 RMSE 는 global 4.90 → 모드 4.17 → 이벤트 1.99pp 다.
- 희소 티어(SOLO_RANK_MASTER_PLUS 약 3.4만 게임, TROPHY_3000_PLUS 약 3천 게임/주)는 n≥100 이벤트 셀이 0% 다.
- 행 감소: 이벤트→모드 약 3.3배, 이벤트→global 약 20배.
- 다른 검증과 같은 방향이었다. 상대팀 페어만 쓴 변형, 시간 분리 2주 예측(저볼륨이라 근거가 약함)이다.

### A.3 저장 용량 (스펙 B)

- `brawler_pair_battle_result_stats` 4.7GB/13.2M 행(이력 2026-04-27~), `brawler_enemy_battle_result_stats` 5.9GB/17.0M 행(2026-04-20~). 행당 약 365~369B 이고 그중 unique 인덱스가 약 165B 다.
- **보존 정책이 없다.** 그런데 API 최대 조회 범위는 `SINCE_LAST_MONTH`(약 2개월)다.
- 주간 버킷 행 수(event 포함, 6개 tier 합, pair / enemy):

  | 버킷 | pair | enemy |
  |---|---|---|
  | 10-05 주 | 420K | 524K |
  | 10-26~31 (6일) | 1.07M | 1.22M |
  | 11-01 (1일 버킷) | 458K | 524K |

  현재 수집량으로는 주 약 300만 행, 약 1.1GB/주(연 약 57GB)로 추정된다.
- pair 는 (A,B)·(B,A) 가 같은 카운트로 이중 저장되고, enemy 는 승↔패가 서로 보완되는 형태로 이중 저장된다.
- 트로피전 주간 조합 표의 약 19.5% 가 비핵심 모드다. 스펙 A 로 줄어든다.

### A.4 3인 조합 (스펙 B)

설계 가정: 이벤트 없음, 정렬된 (b1<b2<b3) 1행, tier 는 팀 최저 트로피로 `resolveAll`, 핵심 6모드 3v3. 10-26~11-02 실측이다.

- 주간 버킷 행 수: 10-26~31 은 29.5만(방향성 3행 저장이면 103만), 11-01(1일)은 18.4만이다. 행당 약 400B(위치별 조회 인덱스 3개 가정)로 6일 버킷이 약 120MB 다.
- 8일 누적 유용성:

  | tier | n≥100 조합 수 | 관측 커버 | 브롤러별 n≥100 조합 수 |
  |---|---|---|---|
  | TROPHY_1000+ | 4,370 | 44% | 중앙 31 (0개인 브롤러 19명) |
  | SOLO_RANK_MYTHIC_PLUS | 6,482 | 52% | 중앙 73 (0개 12명) |
  | SOLO_RANK_LEGENDARY_PLUS | 1,184 | — | — |
  | TROPHY_2000+ | 63 | — | — |
  | MASTER+ / 3000+ | 0 | — | — |

- 원본 `stats_result_battle` 보존은 10일(`RETENTION_DAYS = 10`)이라 과거를 채울 수 없다.
- 배치 맵 최대 엔트리는 약 29.5만(약 36MiB)이다.
- 과거 trio 테이블(`V260426a` 생성, `V260707a` 삭제)은 용량이 아니라 API·배치가 없어 쓰이지 않아 삭제됐다.
- 참고: brawltime.ninja 는 3인 조합을 미리 집계하지 않고 원본에서 즉석 집계한다(승수 기준 상위 50).

### A.5 티어 스코어 (스펙 C)

현재 산식(`BrawlerTierScore`)은 베이지안 수축(C=300) × 픽률 로그 가중(곱)이고, 고정 임계값 S≥55/A≥52/B≥48/C≥45 를 쓴다.

홀짝 일자 분할 예측 상관(pearson_all, 10-26~11-01 고볼륨 구간):

| 맥락 | 현재 | eb_mom | eb300 | eb_mom+0.25픽 | 픽률만 |
|---|---|---|---|---|---|
| 경쟁전 신화+ 전체 이벤트 | 0.816 | 0.911 | 0.911 | 0.893 | 0.138 |
| 트로피 1000+ 전체 이벤트 | 0.772 | 0.871 | 0.865 | 0.812 | **−0.497** |
| 경쟁전 신화+ 이벤트별 | 0.242 | 0.271 | 0.270 | 0.314 | 0.245 |
| 트로피 1000+ 이벤트별 | 0.457 | 0.568 | 0.551 | 0.559 | 0.047 |

- eb_mom 은 맥락별 DerSimonian–Laird 로 τ² 를 추정해 C 를 정하는 방식이다. C 중앙값은 트로피 55~80, 경쟁전 이벤트 약 155, 경쟁전 전체 약 320 이다.
- 픽률 곱 가중은 신뢰도 보정이 아니라 인기 감점에 가깝다. pmax 6% 일 때 픽률 0.1% 브롤러의 승수는 0.144 다.
- 트로피전에서는 픽률과 승률이 반대 방향이다(미러전 압축 등). 경쟁전 이벤트 단위에서는 픽률에 약한 신호가 있다.
- 티어 분포: 현재 산식은 브롤러의 74~84% 를 B 로 둔다.
- 경쟁전 전체 맥락에서 방식별 분포와 안정성:

  | 방식 | S / A / B / C / D (%) | 홀짝 불일치 | kappa |
  |---|---|---|---|
  | 현재 | 3.7 / 13.1 / 73.8 / 8.4 / 0.9 | 0.09 | 0.84 |
  | eb_z | 6.5 / 21.5 / 29.9 / 29.0 / 13.1 | 0.22 | 0.82 |
  | 고정 pp 컷 | — | 0.39 | 0.73 |

- 저표본 맥락(수백~천여 판)에서 z·백분위 티어는 4~11판 브롤러에게도 S/D 를 준다. 최소 판수 규칙과 절대 편차 하한이 필요하다. 합성 시뮬레이션에서도 z·백분위 방식은 순수 잡음에서 S/D 를 약 9% 만들었다.
- 전체 이벤트 맥락의 비핵심 모드 비중은 트로피 19%, 경쟁전 0% 다. 스펙 A 이후 해소된다.
- Wilson 하한은 0 쪽으로 끌리고 예측력이 가장 낮다.
- Redis `statistics` 캐시(TTL 30분)가 `tierScore` 를 품은 `BrawlerResultStatistics` 를 저장한다. 산식만 바뀌면 객체 형태는 같아 역직렬화는 깨지지 않는다. 다만 배포 직후 30분 동안 옛 점수와 새 점수가 섞이지 않게 `v2:brawlerResultStats` 키 버전을 올리는 편이 낫다.

### A.6 외부 사이트 (2026-11-03 조사)

- **Brawlify**: 맵 단위 통계(winRate·useRate)를 낸다. 모드 API 에 `color`·`bgColor` 는 있지만 팀 크기 숫자 필드는 없다("3 vs 3" 같은 자유 텍스트). 산식은 공개하지 않았다.
- **brawltime.ninja**(공개 포크 기준)
  - 모드 화이트리스트가 없다.
  - 2인 조합·카운터는 맵 단위로 미리 집계하지만, 브롤러 페이지의 시너지는 전역 값이다.
  - 3인 조합은 즉석 집계한다.
  - 정렬 지표는 사전 가중치 1,760~2,880판의 강한 베이지안 평균이다.
  - 티어는 이상치 하나씩을 뺀 뒤 min–max 를 등간격으로 나눈다.
- **op.gg·lol.ps**: 승률 중심에 픽·밴률과 인게임 지표를 섞는다고만 밝혔다. 산식은 비공개다.
- **LoLalytics**: 승률 편차 × 픽률 공식을 공개했다.
