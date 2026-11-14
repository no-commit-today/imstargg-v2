package com.imstargg.core.batch.domain.battle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class SoloRankSeasonCacheTest {
    private val sut = SoloRankSeasonCache()

    @Test
    fun `시즌 구간 안의 시각은 그 시즌을 돌려준다`() {
        // when
        val season = sut.get(OffsetDateTime.parse("2025-08-01T12:00:00+09:00").toInstant())

        // then
        assertThat(season.key).isEqualTo("2-1")
    }

    @Test
    fun `시즌 사이 공백 구간의 시각은 다음 시즌을 돌려준다`() {
        // given - 시즌 1-1 종료(04-16 18:00) 와 시즌 1-2 시작(04-17 18:00) 사이
        val betweenSeasons = OffsetDateTime.parse("2025-04-17T00:00:00+09:00").toInstant()

        // when
        val season = sut.get(betweenSeasons)

        // then
        assertThat(season.key).isEqualTo("1-2")
    }

    @Test
    fun `공백 구간을 여러 번 조회해도 같은 시즌 객체를 다시 쓴다`() {
        // given
        val first = sut.get(OffsetDateTime.parse("2025-04-17T00:00:00+09:00").toInstant())

        // when
        val second = sut.get(OffsetDateTime.parse("2025-04-17T01:00:00+09:00").toInstant())
        val inSeason = sut.get(OffsetDateTime.parse("2025-04-20T12:00:00+09:00").toInstant())

        // then
        assertThat(second).isSameAs(first)
        assertThat(inSeason).isSameAs(first)
    }
}
