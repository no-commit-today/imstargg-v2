package com.imstargg.core.batch.job.statistics.playersync

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.enums.StatsPlayerStatus
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.StatsPlayerEntityFixture.Companion.aStatsPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.Chunk

class StatsPlayerSyncJobItemWriterTest(
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    private val sut = StatsPlayerSyncJobItemWriter(statsPlayerJpaRepository)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `표시된 태그를 다시 만나도 새로 넣지 않고 상태를 유지한다`() {
        // given
        statsPlayerJpaRepository.save(
            aStatsPlayerEntity()
                .withBrawlStarsTag("#A")
                .withStatus(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
                .build(),
        )

        // when
        sut.write(Chunk(listOf(listOf(teamPlayer("#A"), teamPlayer("#B")))))

        // then
        assertThat(statsPlayerJpaRepository.count()).isEqualTo(2)
        val statusByTag =
            statsPlayerJpaRepository
                .findAllByBrawlStarsTagIn(listOf("#A", "#B"))
                .associate { it.brawlStarsTag to it.status }
        assertThat(statusByTag["#A"]).isEqualTo(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
        assertThat(statusByTag["#B"]).isEqualTo(StatsPlayerStatus.ACTIVE)
    }

    private fun teamPlayer(tag: String) =
        StatsBattleEntityTeamPlayer(
            brawlStarsTag = tag,
            name = "p",
            brawler = StatsBattleEntityTeamPlayerBrawler(16000000, "SHELLY", 11, 1000),
        )
}
