package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameSlotsTest {
    @Test
    fun `등록된 eventId 는 해당 GameSlot 을 반환`() {
        // given
        val eventId = BrawlStarsId(1L)
        val slot = GameSlot(listOf(3, 3))
        val sut = GameSlots(mapOf(eventId to slot))

        // when
        val result = sut[eventId]

        // then
        assertThat(result).isEqualTo(slot)
    }

    @Test
    fun `등록되지 않은 eventId 는 default GameSlot 으로 폴백`() {
        // given
        val sut = GameSlots(emptyMap())

        // when
        val result = sut[BrawlStarsId(999L)]

        // then
        assertThat(result).isEqualTo(GameSlot.default())
    }

    @Test
    fun `totalGameCount 는 이벤트별 슬롯 합을 게임 수로 환산해 합산한다`() {
        // given - 3v3(slots=6) 360슬롯 → 60게임, 솔로 쇼다운(slots=10) 500슬롯 → 50게임
        val sut =
            GameSlots(
                mapOf(
                    BrawlStarsId(1L) to GameSlot(listOf(3, 3)),
                    BrawlStarsId(2L) to GameSlot(List(10) { 1 }),
                ),
            )

        // when
        val result = sut.totalGameCount(mapOf(BrawlStarsId(1L) to 360L, BrawlStarsId(2L) to 500L))

        // then
        assertThat(result).isEqualTo(110L)
    }

    @Test
    fun `totalGameCount 는 미등록 이벤트를 default 슬롯으로 환산한다`() {
        // given - default = 3v3 (slots=6)
        val sut = GameSlots(emptyMap())

        // when
        val result = sut.totalGameCount(mapOf(BrawlStarsId(9L) to 12L))

        // then
        assertThat(result).isEqualTo(2L)
    }

    @Test
    fun `totalGameCount 는 빈 입력이면 0`() {
        // when
        val result = GameSlots(emptyMap()).totalGameCount(emptyMap())

        // then
        assertThat(result).isEqualTo(0L)
    }
}
