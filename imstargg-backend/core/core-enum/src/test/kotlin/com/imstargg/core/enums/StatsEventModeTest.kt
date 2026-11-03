package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StatsEventModeTest {
    @Nested
    inner class Find {
        @Test
        fun `지원 모드 ID 면 해당 항목을 반환한다`() {
            assertThat(StatsEventMode.find(0L)).isEqualTo(StatsEventMode.GEM_GRAB)
            assertThat(StatsEventMode.find(45L)).isEqualTo(StatsEventMode.BRAWL_HOCKEY)
            assertThat(StatsEventMode.find(38L)).isEqualTo(StatsEventMode.TRIO_SHOWDOWN)
        }

        @Test
        fun `미지원 모드 ID 면 null 을 반환한다`() {
            // 24 태그팀/듀얼, 25 와이프아웃, 31 와이프아웃 5v5, 57 BRAWL HOCKEY(로테이션에 나온 적 없음)
            listOf(24L, 25L, 31L, 57L).forEach { assertThat(StatsEventMode.find(it)).isNull() }
        }
    }

    @Test
    fun `modeBrawlStarsId 는 중복되지 않는다`() {
        val ids = StatsEventMode.entries.map { it.modeBrawlStarsId }
        assertThat(ids).doesNotHaveDuplicates()
    }

    @Test
    fun `결과형 항목의 1게임당 슬롯 수는 RESULT_SLOTS_PER_GAME 과 같다`() {
        val result = StatsEventMode.entries.filter { it.resultType == BattleEventModeResultType.RESULT }
        assertThat(result).hasSize(9)
        assertThat(result).allSatisfy {
            assertThat(it.teamCount * it.teamSize).isEqualTo(StatsEventMode.RESULT_SLOTS_PER_GAME)
        }
    }

    @Test
    fun `순위형 항목은 솔로·듀오·트리오 쇼다운이다`() {
        val rank = StatsEventMode.entries.filter { it.resultType == BattleEventModeResultType.RANK }
        assertThat(rank.map { Triple(it.modeBrawlStarsId, it.teamCount, it.teamSize) })
            .containsExactlyInAnyOrder(Triple(6L, 10, 1), Triple(9L, 5, 2), Triple(38L, 4, 3))
    }
}
