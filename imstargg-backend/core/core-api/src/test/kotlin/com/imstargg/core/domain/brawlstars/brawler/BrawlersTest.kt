package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlersTest {
    private fun brawler(id: Long) =
        Brawler(
            id = BrawlStarsId(id),
            name = Messages(emptyMap()),
            rarity = BrawlerRarity.RARE,
            role = BrawlerRole.TANK,
            portraitImageUrl = null,
        )

    @Test
    fun `id 로 브롤러를 찾는다`() {
        // given
        val sut = Brawlers(listOf(brawler(1L), brawler(2L)))

        // when
        val result = sut.find(BrawlStarsId(2L))

        // then
        assertThat(result).isEqualTo(brawler(2L))
    }

    @Test
    fun `없는 id 는 null 을 반환한다`() {
        // given
        val sut = Brawlers(listOf(brawler(1L)))

        // when
        val result = sut.find(BrawlStarsId(99L))

        // then
        assertThat(result).isNull()
    }
}
