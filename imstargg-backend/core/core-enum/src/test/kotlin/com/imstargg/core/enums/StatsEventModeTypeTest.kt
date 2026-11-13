package com.imstargg.core.enums

import com.imstargg.core.enums.BattleEventModeResultType.RANK
import com.imstargg.core.enums.BattleEventModeResultType.RESULT
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test

class StatsEventModeTypeTest {
    @Test
    fun `유형과 구성은 넷으로 고정한다`() {
        assertThat(StatsEventModeType.entries)
            .extracting("resultType", "teamCount", "teamSize")
            .containsExactly(
                tuple(RESULT, 2, 3),
                tuple(RANK, 10, 1),
                tuple(RANK, 5, 2),
                tuple(RANK, 4, 3),
            )
    }

    @Test
    fun `결과형 유형의 1게임당 슬롯 수는 RESULT_SLOTS_PER_GAME 과 같다`() {
        assertThat(StatsEventModeType.entries.filter { it.resultType == RESULT })
            .allSatisfy { assertThat(it.teamCount * it.teamSize).isEqualTo(StatsEventModeType.RESULT_SLOTS_PER_GAME) }
    }

    @Test
    fun `팀 구성으로 유형을 찾는다`() {
        assertThat(StatsEventModeType.find(RESULT, listOf(3, 3))).isEqualTo(StatsEventModeType.RESULT_3VS3)
        assertThat(StatsEventModeType.find(RANK, List(10) { 1 })).isEqualTo(StatsEventModeType.SOLO_SHOWDOWN)
        assertThat(StatsEventModeType.find(RANK, List(5) { 2 })).isEqualTo(StatsEventModeType.DUO_SHOWDOWN)
        assertThat(StatsEventModeType.find(RANK, List(4) { 3 })).isEqualTo(StatsEventModeType.TRIO_SHOWDOWN)
    }

    @Test
    fun `어느 유형에도 맞지 않는 구성이면 null 이다`() {
        // 20인 솔로(75), 10팀 듀오(86), 5대5, 듀얼 1대1, 3팀, 3대2, 결과형 구성의 순위 전투, 솔로 9팀
        listOf(
            RANK to List(20) { 1 },
            RANK to List(10) { 2 },
            RESULT to listOf(5, 5),
            RESULT to listOf(1, 1),
            RESULT to listOf(3, 3, 3),
            RESULT to listOf(3, 2),
            RANK to listOf(3, 3),
            RANK to List(9) { 1 },
        ).forEach { (resultType, teamSizes) ->
            assertThat(StatsEventModeType.find(resultType, teamSizes)).describedAs("$resultType $teamSizes").isNull()
        }
    }
}
