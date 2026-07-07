package com.imstargg.core.application.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.annotation.Cacheable
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

class StatisticsCacheKeyTest {
    private val targets =
        listOf(
            BattleResultStatisticsRepositoryWithCache::class,
            BattleRankStatisticsRepositoryWithCache::class,
        )

    @Test
    fun `Cacheable key 표현식은 메서드의 모든 파라미터를 포함한다`() {
        targets.forEach { klass ->
            klass.declaredFunctions.forEach { fn ->
                val cacheable = fn.findAnnotation<Cacheable>() ?: return@forEach
                fn.valueParameters.forEach { param ->
                    assertThat(cacheable.key)
                        .withFailMessage(
                            "%s.%s 의 캐시 키에 #%s 가 없습니다 — 파라미터를 추가했다면 key 도 갱신하세요",
                            klass.simpleName,
                            fn.name,
                            param.name,
                        ).contains("#${param.name}")
                }
            }
        }
    }

    @Test
    fun `캐시 키 스냅샷 - 형식이 바뀌면 메서드 단위 v prefix 범프를 검토할 것`() {
        // given
        val keys =
            targets
                .flatMap { klass ->
                    klass.declaredFunctions.mapNotNull { fn ->
                        fn.findAnnotation<Cacheable>()?.let { "${klass.simpleName}.${fn.name}" to it.key }
                    }
                }.toMap()

        // then
        assertThat(keys).isEqualTo(
            mapOf(
                "BattleResultStatisticsRepositoryWithCache.findBrawlerResultStatistics" to
                    "'v2:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleResultStatisticsRepositoryWithCache.findBrawlerPairResultStatistics" to
                    "'v2:brawlerPairResultStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                    " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleResultStatisticsRepositoryWithCache.findBrawlerEnemyResultStatistics" to
                    "'v2:brawlerEnemyResultStats:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                    " + ':' + #startDate + ':' + #endDate",
                "BattleResultStatisticsRepositoryWithCache.findBrawlerDailyResultStatistics" to
                    "'v1:brawlerDailyResultStats:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleRankStatisticsRepositoryWithCache.findBrawlerRankStatistics" to
                    "'v1:brawlerRankStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleRankStatisticsRepositoryWithCache.findBrawlerPairRankStatistics" to
                    "'v1:brawlerPairRankStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                    " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
            ),
        )
    }
}
