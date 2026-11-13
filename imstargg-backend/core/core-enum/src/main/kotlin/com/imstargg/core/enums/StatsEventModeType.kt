package com.imstargg.core.enums

/**
 * 통계 수집 유형. 모드 → 유형 대응은 DB `stats_event_mode` 에 있고, 관리자 화면에서 정한다.
 * 유형별 규칙(결과형/순위형, 팀 수, 팀당 인원)만 이 enum 이 단일 출처로 갖는다.
 * 대응이 없는 모드의 전투는 core-worker 수집 단계에서 버린다.
 *
 * ## 결정 이력
 * - 2026-11 기준 핵심 6개 모드(젬그랩·하이스트·바운티·브롤볼·핫존·넉아웃)가 결과형 전투의 91% 이고,
 *   하키·아레나·농구를 더하면 약 98% 다.
 * - 하키·아레나·농구는 상시 로테이션이고, 팀 구성이 100% 2×3 이라 수집 대상에 넣었다.
 * - 와이프아웃은 맵당 하루 판 수가 얇아서, 태그팀/듀얼(24)은 팀당 플레이어가 1명이라 조합 통계가 성립하지 않아서,
 *   5v5 는 팀 구성이 2×3 이 아니라서 뺐다.
 * - battlelog 의 `battle.mode` 문자열은 믿을 수 없다(45 는 `brawlBall`, 48 은 `siege` 로 온다).
 *   하키·아레나도 `modeBrawlStarsId` 로만 식별한다.
 * - 기존 `battle_event_mode.player_counts` 는 관리되지 않았고, 듀얼을 펼치는 과정에서 오류도 있었다.
 *   그래서 팀 수와 팀당 인원을 이 enum 이 단일 출처로 갖는다.
 *
 * ## 모드를 추가하는 절차
 * 1. battlelog 표본으로 팀 구성을 확인한다.
 * 2. 관리자 화면에서 유형을 지정한다.
 * 3. 필요하면 프론트 `MODE_COLORS`(`imstargg-frontend/app/events/_components/modeStyles.ts`)에 모드 색을 더한다.
 *    색을 정하지 않은 모드는 기본 회색으로 보인다.
 * 4. 기존 유형에 맞지 않으면 새 유형이 필요하다. 이때는 코드 변경이다.
 *    결과형 유형은 반드시 2×3 이어야 한다. [RESULT_SLOTS_PER_GAME] 의 전제다.
 *    순위형 유형이면 core-batch 의 봇 로비 판정(`StatsBotBattlePolicy` 의 쇼다운 임계값)과
 *    감시(`StatsBotLobbyMonitor` 의 `SPECS`)에도 유형을 더한다.
 *
 * @property resultType 통계 수집 유형. 결과(승/패/무)를 모으는지, 순위를 모으는지 나타낸다
 * @property teamCount 한 게임의 팀 수
 * @property teamSize 팀당 플레이어 수
 */
enum class StatsEventModeType(
    val resultType: BattleEventModeResultType,
    val teamCount: Int,
    val teamSize: Int,
) {
    RESULT_3VS3(BattleEventModeResultType.RESULT, 2, 3),
    SOLO_SHOWDOWN(BattleEventModeResultType.RANK, 10, 1),
    DUO_SHOWDOWN(BattleEventModeResultType.RANK, 5, 2),
    TRIO_SHOWDOWN(BattleEventModeResultType.RANK, 4, 3),
    ;

    /** 팀 수가 [teamCount] 이고 모든 팀이 [teamSize] 명이면 일치한다. */
    fun matches(teamSizes: List<Int>): Boolean = teamSizes.size == teamCount && teamSizes.all { it == teamSize }

    companion object {
        /**
         * 결과형 1게임당 슬롯 수.
         * 결과형 유형이 모두 2×3 이라는 전제에서 나온다(StatsEventModeTypeTest 가 고정).
         */
        const val RESULT_SLOTS_PER_GAME = 6

        /** 팀 구성에 맞는 유형을 찾는다. 어느 유형에도 맞지 않으면 null. */
        fun find(
            resultType: BattleEventModeResultType,
            teamSizes: List<Int>,
        ): StatsEventModeType? = entries.find { it.resultType == resultType && it.matches(teamSizes) }
    }
}
