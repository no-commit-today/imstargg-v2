package com.imstargg.core.api.controller.v1.response

import com.fasterxml.jackson.databind.ObjectMapper
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatisticsItem
import com.imstargg.core.domain.statistics.BrawlerTierScore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerResultStatisticsResponseTest {
    @Test
    fun `구버전 호환 totalBattleCount 는 battleCount 와 같은 값으로 직렬화된다`() {
        // given
        val sut =
            BrawlerResultStatisticsItemResponse(
                brawler = null,
                battleCount = 100,
                pickRate = 0.1,
                winRate = 0.55,
                starPlayerRate = 0.2,
                tierScore = 80,
                tier = "S",
            )

        // when
        val json = ObjectMapper().readTree(ObjectMapper().writeValueAsString(sut))

        // then
        assertThat(json.get("battleCount").asLong()).isEqualTo(100)
        assertThat(json.get("totalBattleCount").asLong()).isEqualTo(100)
    }

    @Test
    fun `등급이 없으면 tier 는 null 이고 tierScore 는 값이 있다`() {
        // given
        val statistics =
            BrawlerResultStatistics(
                totalBattleCount = 10,
                items =
                    listOf(
                        BrawlerResultStatisticsItem(
                            brawlerId = BrawlStarsId(1L),
                            battleCount = 100,
                            pickRate = 0.1,
                            winRate = 0.9,
                            starPlayerRate = 0.0,
                            tierScore = BrawlerTierScore(value = 66, grade = null),
                        ),
                    ),
            )

        // when
        val sut = BrawlerResultStatisticsResponse.of(statistics, Brawlers(emptyList()))
        val json = ObjectMapper().readTree(ObjectMapper().writeValueAsString(sut))
        val item = json.get("items").get(0)

        // then
        assertThat(item.has("tier")).isTrue()
        assertThat(item.get("tier").isNull).isTrue()
        assertThat(item.get("tierScore").asInt()).isEqualTo(66)
    }
}
