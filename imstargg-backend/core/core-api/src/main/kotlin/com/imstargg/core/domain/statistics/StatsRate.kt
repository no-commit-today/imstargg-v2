package com.imstargg.core.domain.statistics

import com.imstargg.core.enums.StatsEventMode

object StatsRate {
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
     * - 이전 `GameSlots.totalGameCount` 의 결정을 이어받는다. Pair/Enemy 통계의 분모도 Result 슬롯 합 기준으로 계산한다.
     *   같은 게임 수가 보장되므로 Result 테이블이 단일 출처다.
     * - 2026-11 스펙 A 로 "이벤트별 내림 후 합산" 에서 "합산 후 한 번 나누기" 로 바꿨다.
     *   지원하는 결과형 모드가 모두 2×3 이라 이벤트마다 슬롯 수가 달라지지 않기 때문이다.
     *   이전 GameSlots 의 이벤트 단위 내림 테스트(2026-11 스펙 A 에서 삭제)는 이 변경으로 의도적으로 뒤집었다.
     *   합산 후 한 번만 나누는 동작은 `BattleResultStatisticsRepositoryTest` 가 저장소 수준에서 고정한다.
     *
     * @param resultSlotSum 조회 범위 안 결과형 통계의 슬롯 합 (victory + defeat + draw)
     */
    fun resultGameCount(resultSlotSum: Long): Long = resultSlotSum / StatsEventMode.RESULT_SLOTS_PER_GAME

    /**
     * 픽률 = 슬롯(브롤러/조합 등장) 수 / 전투(게임) 수.
     *
     * **분모·분자 의미의 단일 정본** (Result/Pair/Enemy 통계 공통):
     * - 분모 `totalBattleCount` 는 **게임 수** 여야 한다. 이전에는 슬롯 합으로 잘못 사용됨 — 2026-05-19 정정.
     *   슬롯 합 ↔ 게임 수 환산 규칙은 [resultGameCount] 참조.
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
