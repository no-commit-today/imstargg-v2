package com.imstargg.core.domain.statistics

object StatsRate {
    fun winRate(
        victoryCount: Long,
        defeatCount: Long,
    ): Double {
        val total = victoryCount + defeatCount
        return if (total > 0) victoryCount.toDouble() / total else 0.0
    }

    /**
     * 픽률 = 슬롯(브롤러/조합 등장) 수 / 전투(게임) 수.
     *
     * **분모·분자 의미의 단일 정본** (Result/Pair/Enemy 통계 공통):
     * - 분모 `totalBattleCount` 는 **게임 수** 여야 한다. 이전에는 슬롯 합으로 잘못 사용됨 — 2026-05-19 정정.
     *   슬롯 합 ↔ 게임 수 환산 규칙은 [GameSlots.totalGameCount] 참조.
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
