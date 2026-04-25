package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatisticsCacheKeysTest {
    @Test
    fun `joinIds 는 빈 리스트면 빈 문자열`() {
        // when
        val sut = StatisticsCacheKeys.joinIds(emptyList())

        // then
        assertThat(sut).isEqualTo("")
    }

    @Test
    fun `joinIds 는 value 정렬 후 _ 로 join`() {
        // when
        val sut =
            StatisticsCacheKeys.joinIds(
                listOf(
                    BrawlStarsId(16000003L),
                    BrawlStarsId(16000001L),
                    BrawlStarsId(16000002L),
                ),
            )

        // then
        assertThat(sut).isEqualTo("16000001_16000002_16000003")
    }

    @Test
    fun `joinIds 는 동일 입력 순서가 달라도 같은 결과`() {
        // when
        val a = StatisticsCacheKeys.joinIds(listOf(BrawlStarsId(2L), BrawlStarsId(1L)))
        val b = StatisticsCacheKeys.joinIds(listOf(BrawlStarsId(1L), BrawlStarsId(2L)))

        // then
        assertThat(a).isEqualTo(b)
    }
}
