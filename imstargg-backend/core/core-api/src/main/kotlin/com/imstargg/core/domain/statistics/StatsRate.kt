package com.imstargg.core.domain.statistics

import com.imstargg.core.enums.StatsEventMode

object StatsRate {
    /**
     * 3명 팀의 쌍 수(3C2). 기본 티어에서 2인 조합 테이블의 팀 하나는 쌍 3개, 곧 `total_count` 3 으로 쌓인다.
     * 상위 티어는 쌍 단위로 티어를 판정(두 멤버 모두 그 티어 이상)하므로 팀당 0·1·3쌍이 쌓여 `÷ 3` 은 근사다.
     * 결과형 모드가 모두 2×3 이라는 전제([StatsEventMode.RESULT_SLOTS_PER_GAME])에서 나온다.
     */
    const val PAIRS_PER_TEAM = 3

    /**
     * 한 게임에서 X 가 만나는 적 수(상대 팀 3명). 카운터 테이블에서 X 의 게임 하나는 X 기준 행 3개로 쌓인다.
     * 결과형 모드가 모두 2×3 이라는 전제([StatsEventMode.RESULT_SLOTS_PER_GAME])에서 나온다.
     */
    const val ENEMY_SLOTS_PER_GAME = 3

    fun winRate(
        victoryCount: Long,
        defeatCount: Long,
    ): Double {
        val total = victoryCount + defeatCount
        return if (total > 0) victoryCount.toDouble() / total else 0.0
    }

    /**
     * 결과형 슬롯 합을 게임 수로 환산한다. `슬롯 합 ÷ 6` 이고 정수 나눗셈이라 나머지는 버린다.
     * 6 은 [StatsEventMode.RESULT_SLOTS_PER_GAME] 이다.
     *
     * ## 결정 이력
     * - 이전 `GameSlots.totalGameCount` 의 결정을 이어받는다.
     *   2026-11 스펙 B 부터 조합·카운터 분모는 각 조합 테이블의 합(결정 12)이라 이 함수는 1인 통계만 쓴다.
     *   조합·카운터 환산은 [pairTeamCount]·[enemyGameCount] 참조.
     * - 2026-11 스펙 A 로 "이벤트별 내림 후 합산" 에서 "합산 후 한 번 나누기" 로 바꿨다.
     *   지원하는 결과형 모드가 모두 2×3 이라 이벤트마다 슬롯 수가 달라지지 않기 때문이다.
     *   이전 GameSlots 의 이벤트 단위 내림 테스트(2026-11 스펙 A 에서 삭제)는 이 변경으로 의도적으로 뒤집었다.
     *   합산 후 한 번만 나누는 동작은 `BrawlerResultStatisticsTest` 가 고정한다.
     *
     * @param resultSlotSum 조회 범위 안 결과형 통계의 슬롯 합 (victory + defeat + draw)
     */
    fun resultGameCount(resultSlotSum: Long): Long = resultSlotSum / StatsEventMode.RESULT_SLOTS_PER_GAME

    /**
     * 2인 조합 슬롯 합을 팀 수로 환산한다. `슬롯 합 ÷ 3`([PAIRS_PER_TEAM]) 이고 정수 나눗셈이라 나머지는 버린다.
     * 2인 조합 픽률의 분모다(2026-11 스펙 B 결정 12).
     *
     * @param pairSlotSum 조회 조건(티어, 기간) 안 모든 2인 조합 행의 `total_count` 합
     */
    fun pairTeamCount(pairSlotSum: Long): Long = pairSlotSum / PAIRS_PER_TEAM

    /**
     * X 기준 카운터 슬롯 합을 X 의 게임 수로 환산한다. `슬롯 합 ÷ 3`([ENEMY_SLOTS_PER_GAME]) 이고 정수 나눗셈이라
     * 나머지는 버린다. 카운터 픽률의 분모다(2026-11 스펙 B 결정 12).
     *
     * @param enemySlotSum 조회 조건(티어, 기간) 안 X 기준 카운터 행의 `total_count` 합
     */
    fun enemyGameCount(enemySlotSum: Long): Long = enemySlotSum / ENEMY_SLOTS_PER_GAME

    /**
     * 픽률 = 항목(브롤러/조합/상대) 등장 수 / 분모.
     *
     * **분모·분자 의미의 단일 정본** (Result/Pair/Enemy 통계 공통):
     * - 1인 통계 분모 `totalBattleCount` 는 **게임 수** 여야 한다. 이전에는 슬롯 합으로 잘못 사용됨 — 2026-05-19 정정.
     *   슬롯 합 ↔ 게임 수 환산 규칙은 [resultGameCount] 참조.
     * - 조합 픽률 분모 = 조합 테이블 자체의 합(2026-11 스펙 B 결정 12) — 1인 테이블 의존을 끊었다.
     *   2인은 같은 (티어, 기간)의 조합 행 합을 [pairTeamCount] 로 팀 수로 바꾼다.
     * - 카운터 픽률 = X 의 게임 중 Y 를 만난 비율. 분모는 X 기준 카운터 행의 합을 [enemyGameCount] 로 바꾼 X 의 게임 수다.
     * - item 단위 `battleCount` 는 **해당 항목의 슬롯 수** 다. 정정 당시 item 에 전역 게임 수를 복사해
     *   전 항목 전투수가 동일 표시됐던 버그가 있었음 — 2026-06-06 정정.
     *
     * Pair/Enemy 통계 클래스는 구조가 닮았지만 의미가 다른 별개 모델로 **중복 유지가 의도된 설계**다.
     * 이 규칙을 고칠 때는 각 사본의 단위 테스트(BrawlerResultStatisticsTest,
     * BrawlerPairResultStatisticsTest, BrawlerEnemyResultStatisticsTest)를 함께 갱신할 것.
     */
    fun pickRate(
        battleCount: Long,
        totalBattleCount: Long,
    ): Double = if (totalBattleCount > 0) battleCount.toDouble() / totalBattleCount else 0.0

    fun starPlayerRate(
        starPlayerCount: Long,
        totalBattleCount: Long,
    ): Double = if (totalBattleCount > 0) starPlayerCount.toDouble() / totalBattleCount else 0.0
}
