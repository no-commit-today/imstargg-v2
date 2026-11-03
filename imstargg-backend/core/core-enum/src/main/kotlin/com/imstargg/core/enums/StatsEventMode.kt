package com.imstargg.core.enums

/**
 * 통계를 수집하는 모드의 허용 목록. 목록에 없는 모드의 전투는 core-worker 수집 단계에서 버린다.
 *
 * ## 결정 이력
 * - 2026-11 기준 핵심 6개 모드(젬그랩·하이스트·바운티·브롤볼·핫존·넉아웃)가 결과형 전투의 91% 이고,
 *   하키·아레나·농구를 더하면 약 98% 다.
 * - 하키·아레나·농구는 상시 로테이션이고, 팀 구성이 100% 2×3 이라 넣었다.
 * - 와이프아웃은 맵당 하루 판 수가 얇아서, 태그팀/듀얼(24)은 팀당 플레이어가 1명이라 조합 통계가 성립하지 않아서,
 *   5v5 는 팀 구성이 2×3 이 아니라서 뺐다.
 * - battlelog 의 `battle.mode` 문자열은 믿을 수 없다(45 는 `brawlBall`, 48 은 `siege` 로 온다).
 *   하키·아레나도 `modeBrawlStarsId` 로만 식별한다.
 * - 기존 `battle_event_mode.player_counts` 는 관리되지 않았고, 듀얼을 펼치는 과정에서 오류도 있었다.
 *   그래서 팀 수와 팀당 인원을 이 enum 이 단일 출처로 갖는다.
 *
 * ## 모드를 추가하는 절차
 * 1. 운영 `stats_*_battle` 이 아닌 battlelog 표본으로 팀 구성을 확인한다. 추가하기 전에는 수집되지 않으므로 운영 테이블에 표본이 없다.
 * 2. 이 enum 에 항목을 추가한다.
 * 3. `StatsEventModeTest` 의 기대 집합(허용 목록 전체를 고정한다)에 같은 항목을 더한다.
 * 4. 프론트 `MODE_COLORS`(`imstargg-frontend/app/events/_components/modeStyles.ts`)에 모드 색을 추가한다.
 *    색을 정하지 않은 모드(아레나 48)는 기본 회색으로 보인다.
 * 5. 결과형이면 반드시 2×3 이어야 한다. [RESULT_SLOTS_PER_GAME] 의 전제다.
 *
 * @property modeBrawlStarsId 브롤스타즈가 부여한 모드 ID
 * @property resultType 통계 수집 유형. 결과(승/패/무)를 모으는지, 순위를 모으는지 나타낸다
 * @property teamCount 한 게임의 팀 수
 * @property teamSize 팀당 플레이어 수
 */
enum class StatsEventMode(
    val modeBrawlStarsId: Long,
    val resultType: BattleEventModeResultType,
    val teamCount: Int,
    val teamSize: Int,
) {
    GEM_GRAB(
        modeBrawlStarsId = 0,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    HEIST(
        modeBrawlStarsId = 2,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    BOUNTY(
        modeBrawlStarsId = 3,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    BRAWL_BALL(
        modeBrawlStarsId = 5,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    HOT_ZONE(
        modeBrawlStarsId = 17,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    KNOCKOUT(
        modeBrawlStarsId = 20,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    BASKET_BRAWL(
        modeBrawlStarsId = 22,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    BRAWL_HOCKEY(
        modeBrawlStarsId = 45,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    BRAWL_ARENA(
        modeBrawlStarsId = 48,
        resultType = BattleEventModeResultType.RESULT,
        teamCount = 2,
        teamSize = 3,
    ),
    SOLO_SHOWDOWN(
        modeBrawlStarsId = 6,
        resultType = BattleEventModeResultType.RANK,
        teamCount = 10,
        teamSize = 1,
    ),
    DUO_SHOWDOWN(
        modeBrawlStarsId = 9,
        resultType = BattleEventModeResultType.RANK,
        teamCount = 5,
        teamSize = 2,
    ),
    TRIO_SHOWDOWN(
        modeBrawlStarsId = 38,
        resultType = BattleEventModeResultType.RANK,
        teamCount = 4,
        teamSize = 3,
    ),
    ;

    companion object {
        /**
         * 결과형 1게임당 슬롯 수.
         * 결과형 항목이 모두 2×3 이라는 전제에서 나온다(StatsEventModeTest 가 고정).
         */
        const val RESULT_SLOTS_PER_GAME = 6

        fun find(modeBrawlStarsId: Long): StatsEventMode? = entries.find { it.modeBrawlStarsId == modeBrawlStarsId }
    }
}
