package com.imstargg.core.batch.job.statistics.playersync

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.enums.Country
import com.imstargg.core.enums.StatsPlayerStatus
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.StatsPlayerEntityFixture.Companion.aStatsPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant

class StatsPlayerSyncFromRankingTaskTest(
    private val sut: StatsPlayerSyncFromRankingTask,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `랭킹에 표시된 태그가 다시 나와도 새로 넣지 않고 상태를 유지한다`() {
        // given
        statsPlayerJpaRepository.save(
            aStatsPlayerEntity()
                .withBrawlStarsTag("#MARKED")
                .withStatus(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
                .build(),
        )
        saveRanking(Country.GLOBAL, "#MARKED", "#NEW")

        // when
        sut.sync()

        // then
        assertThat(statsPlayerJpaRepository.count()).isEqualTo(2)
        val statusByTag =
            statsPlayerJpaRepository
                .findAllByBrawlStarsTagIn(listOf("#MARKED", "#NEW"))
                .associate { it.brawlStarsTag to it.status }
        assertThat(statusByTag["#MARKED"]).isEqualTo(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
        assertThat(statusByTag["#NEW"]).isEqualTo(StatsPlayerStatus.ACTIVE)
    }

    private fun saveRanking(
        country: Country,
        vararg tags: String,
    ) {
        val ranking = playerRankingJpaRepository.save(PlayerRankingEntity(country = country, collectedAt = Instant.now()))
        playerRankingItemJpaRepository.saveAll(
            tags.mapIndexed { index, tag ->
                PlayerRankingItemEntity(
                    playerRanking = ranking,
                    rank = index + 1,
                    player =
                        RankingEntityPlayer(
                            brawlStarsTag = tag,
                            name = "name$index",
                            nameColor = null,
                            iconBrawlStarsId = 28000000,
                            clubName = "",
                        ),
                    trophies = 10000,
                )
            },
        )
    }
}
