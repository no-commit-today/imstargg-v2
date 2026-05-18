package com.imstargg.core.config

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.domain.statistics.ResultCount
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule

class CacheConfigSerializerTest {
    private val typeValidator =
        BasicPolymorphicTypeValidator
            .builder()
            .allowIfSubType("com.imstargg.")
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .build()

    private val sut =
        GenericJacksonJsonRedisSerializer
            .builder()
            .enableDefaultTyping(typeValidator)
            .customize { mapperBuilder ->
                mapperBuilder
                    .addModule(KotlinModule.Builder().build())
                    .setDefaultTyping(StatisticsTypeResolverBuilder(typeValidator))
            }.build()

    @Test
    fun `Kotlin data class List 직렬화 라운드트립`() {
        // given
        val original =
            listOf(
                BrawlerRankCount(
                    brawlerId = BrawlStarsId(16_000_000L),
                    rankCount = RankCount(counts = mapOf(1 to 10L, 2 to 5L)),
                ),
                BrawlerRankCount(
                    brawlerId = BrawlStarsId(16_000_001L),
                    rankCount = RankCount(counts = mapOf(3 to 7L, 4 to 2L)),
                ),
            )

        // when
        val bytes = sut.serialize(original)
        val deserialized = sut.deserialize(bytes)

        // then
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `BrawlerResultCount List 직렬화 라운드트립`() {
        // given
        val original =
            listOf(
                BrawlerResultCount(
                    brawlerId = BrawlStarsId(16_000_000L),
                    resultCount = ResultCount(victoryCount = 10L, defeatCount = 5L, drawCount = 1L),
                    starPlayerCount = 3L,
                ),
            )

        // when
        val bytes = sut.serialize(original)
        val deserialized = sut.deserialize(bytes)

        // then
        assertThat(deserialized).isEqualTo(original)
    }
}
