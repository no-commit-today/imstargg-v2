package com.imstargg.infrastructure.redis.core.statistics.player

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlayerBrawlerStatsCacheCodecTest {
    private val sut = PlayerBrawlerStatsCacheCodec()

    @Test
    fun `인코딩 후 디코딩하면 원본과 같다`() {
        // given
        val value =
            PlayerBrawlerStatsCacheValue(
                items =
                    listOf(
                        PlayerBrawlerStatsCacheValue.Item(16000000L, 5L, 1L, 0L),
                        PlayerBrawlerStatsCacheValue.Item(16000001L, 2L, 2L, 2L),
                    ),
            )

        // when
        val decoded = sut.decode(sut.encode(value))

        // then
        assertThat(decoded).isEqualTo(value)
    }

    @Test
    fun `빈 items 도 라운드트립된다`() {
        // given
        val value = PlayerBrawlerStatsCacheValue(items = emptyList())

        // when
        val decoded = sut.decode(sut.encode(value))

        // then
        assertThat(decoded).isEqualTo(value)
    }
}
