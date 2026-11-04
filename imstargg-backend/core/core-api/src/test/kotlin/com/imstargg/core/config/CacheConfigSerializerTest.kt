package com.imstargg.core.config

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatisticsItem
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerPairResultStatisticsItem
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatisticsItem
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.TierScore
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
            .allowIfSubType("kotlin.collections.")
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

    @Test
    fun `빈 items 리스트를 가진 통계 객체 직렬화 라운드트립`() {
        // given
        val original = BrawlerResultStatistics(totalBattleCount = 0L, items = emptyList())

        // when
        val bytes = sut.serialize(original)
        val deserialized = sut.deserialize(bytes)

        // then
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `BrawlerResultStatistics 직렬화 라운드트립 - tierScore 포함`() {
        // given
        val original =
            BrawlerResultStatistics(
                totalBattleCount = 1_200L,
                items =
                    listOf(
                        BrawlerResultStatisticsItem(
                            brawlerId = BrawlStarsId(16_000_000L),
                            battleCount = 480L,
                            pickRate = 0.4,
                            winRate = 0.55,
                            starPlayerRate = 0.18,
                            tierScore = TierScore(52),
                        ),
                    ),
            )

        // when
        val bytes = sut.serialize(original)
        val deserialized = sut.deserialize(bytes)

        // then
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `BrawlerPairResultStatistics 직렬화 라운드트립`() {
        // given
        val original =
            BrawlerPairResultStatistics(
                items =
                    listOf(
                        BrawlerPairResultStatisticsItem(
                            brawlerId = BrawlStarsId(16_000_000L),
                            pairBrawlerId = BrawlStarsId(16_000_001L),
                            battleCount = 120L,
                            pickRate = 0.03,
                            winRate = 0.56,
                            tierScore = TierScore(52),
                        ),
                    ),
            )

        // when
        val bytes = sut.serialize(original)
        val deserialized = sut.deserialize(bytes)

        // then
        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun `BrawlerEnemyResultStatistics 직렬화 라운드트립`() {
        // given
        val original =
            BrawlerEnemyResultStatistics(
                items =
                    listOf(
                        BrawlerEnemyResultStatisticsItem(
                            brawlerId = BrawlStarsId(16_000_000L),
                            enemyBrawlerId = BrawlStarsId(16_000_001L),
                            battleCount = 90L,
                            pickRate = 0.12,
                            winRate = 0.44,
                        ),
                    ),
            )

        // when
        val bytes = sut.serialize(original)
        val deserialized = sut.deserialize(bytes)

        // then
        assertThat(deserialized).isEqualTo(original)
    }
}
