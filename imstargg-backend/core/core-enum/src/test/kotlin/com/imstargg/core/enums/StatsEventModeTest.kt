package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
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
    fun `허용 목록은 지원 모드 12개와 그 유형·팀 구성 전체와 같다`() {
        // given
        val result = BattleEventModeResultType.RESULT
        val rank = BattleEventModeResultType.RANK

        // when
        val sut = StatsEventMode.entries

        // then
        assertThat(sut)
            .extracting("modeBrawlStarsId", "resultType", "teamCount", "teamSize")
            .containsExactlyInAnyOrder(
                tuple(0L, result, 2, 3),
                tuple(2L, result, 2, 3),
                tuple(3L, result, 2, 3),
                tuple(5L, result, 2, 3),
                tuple(17L, result, 2, 3),
                tuple(20L, result, 2, 3),
                tuple(22L, result, 2, 3),
                tuple(45L, result, 2, 3),
                tuple(48L, result, 2, 3),
                tuple(6L, rank, 10, 1),
                tuple(9L, rank, 5, 2),
                tuple(38L, rank, 4, 3),
            )
    }

    @Test
    fun `modeBrawlStarsId 는 중복되지 않는다`() {
        val ids = StatsEventMode.entries.map { it.modeBrawlStarsId }
        assertThat(ids).doesNotHaveDuplicates()
    }

    @Test
    fun `결과형 항목의 1게임당 슬롯 수는 RESULT_SLOTS_PER_GAME 과 같다`() {
        val result = StatsEventMode.entries.filter { it.resultType == BattleEventModeResultType.RESULT }
        assertThat(result).allSatisfy {
            assertThat(it.teamCount * it.teamSize).isEqualTo(StatsEventMode.RESULT_SLOTS_PER_GAME)
        }
    }
}
