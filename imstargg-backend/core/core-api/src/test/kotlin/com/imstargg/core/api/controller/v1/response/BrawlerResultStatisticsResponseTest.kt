package com.imstargg.core.api.controller.v1.response

import com.fasterxml.jackson.databind.ObjectMapper
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
}
