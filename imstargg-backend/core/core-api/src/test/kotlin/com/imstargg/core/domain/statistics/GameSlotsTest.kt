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
}
