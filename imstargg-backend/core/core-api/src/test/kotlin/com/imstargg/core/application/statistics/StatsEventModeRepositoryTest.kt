package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.StatsEventModeEntity
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager

class StatsEventModeRepositoryTest(
    private val sut: StatsEventModeRepository,
    private val cleanUp: CleanUp,
    private val statsEventModeJpaRepository: StatsEventModeJpaRepository,
    private val cacheManager: CacheManager,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
        cacheManager.getCache(CacheNames.STATS_EVENT_MODE)?.clear()
    }

    @AfterEach
    fun tearDown() {
        cacheManager.getCache(CacheNames.STATS_EVENT_MODE)?.clear()
    }

    @Test
    fun `수집 모드 목록을 캐시를 거쳐 읽는다`() {
        // given
        statsEventModeJpaRepository.save(StatsEventModeEntity(20L, StatsEventModeType.RESULT_3VS3))
        statsEventModeJpaRepository.save(StatsEventModeEntity(6L, StatsEventModeType.SOLO_SHOWDOWN))

        // when
        val result = sut.findAll()

        // then
        assertThat(result.resultTypeOf(BrawlStarsId(20))).isEqualTo(BattleEventModeResultType.RESULT)
        assertThat(result.resultTypeOf(BrawlStarsId(6))).isEqualTo(BattleEventModeResultType.RANK)
        assertThat(result.resultTypeOf(BrawlStarsId(25))).isNull()
    }
}
