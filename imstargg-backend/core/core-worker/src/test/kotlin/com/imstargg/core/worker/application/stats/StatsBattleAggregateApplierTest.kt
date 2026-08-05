package com.imstargg.core.worker.application.stats

import com.imstargg.CoreWorkerContextTest
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleEvent
import com.imstargg.core.worker.domain.BattleTeamPlayer
import com.imstargg.core.worker.domain.BattleTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class StatsBattleAggregateApplierTest(
    private val sut: StatsBattleAggregateApplier,
    private val cleanUp: CleanUp,
    private val statsRankBattleRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleRepository: StatsResultBattleJpaRepository,
    private val clock: Clock,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Nested
    inner class `apply 중복 수집` {
        @Test
        fun `rank 배틀을 같은 팀 A B C 가 동시에 수집해 넘겨도 1 row 만 저장된다`() {
            // given
            val battleAt = clock.instant().minus(Duration.ofHours(1))
            val battleKey = "rank-battle-key"
            val battles =
                listOf(
                    createRankBattle(battleKey = battleKey, battleAt = battleAt, playerBrawlStarsTag = "#A"),
                    createRankBattle(battleKey = battleKey, battleAt = battleAt, playerBrawlStarsTag = "#B"),
                    createRankBattle(battleKey = battleKey, battleAt = battleAt, playerBrawlStarsTag = "#C"),
                )

            // when
            sut.apply(battles)

            // then
            val saved = statsRankBattleRepository.findAll()
            assertThat(saved)
                .singleElement()
                .extracting { it.playerBrawlStarsTag }
                .isEqualTo("#A")
        }

        @Test
        fun `result 배틀도 같은 battle_key 인 중복 수집은 1 row 만 저장된다`() {
            // given
            val battleAt = clock.instant().minus(Duration.ofHours(1))
            val battleKey = "result-battle-key"
            val battles =
                listOf(
                    createResultBattle(battleKey = battleKey, battleAt = battleAt, playerBrawlStarsTag = "#A"),
                    createResultBattle(battleKey = battleKey, battleAt = battleAt, playerBrawlStarsTag = "#B"),
                )

            // when
            sut.apply(battles)

            // then
            assertThat(statsResultBattleRepository.count()).isEqualTo(1)
        }
    }

    private fun createRankBattle(
        battleKey: String,
        battleAt: Instant,
        playerBrawlStarsTag: String,
    ) = Battle(
        battleKey = battleKey,
        battleAt = battleAt,
        event =
            BattleEvent(
                brawlStarsId = 10000001L,
                modeBrawlStarsName = "soloShowdown",
                modeBrawlStarsId = 2L,
                map = "Flying Fantasies",
            ),
        mode = "soloShowdown",
        type = "soloRanked",
        result = null,
        duration = 117,
        starPlayerBrawlStarsTag = null,
        playerBrawlStarsTag = playerBrawlStarsTag,
        battleRank = 1,
        trophyChange = null,
        teams = createSoloTeams(),
    )

    private fun createResultBattle(
        battleKey: String,
        battleAt: Instant,
        playerBrawlStarsTag: String,
    ) = Battle(
        battleKey = battleKey,
        battleAt = battleAt,
        event =
            BattleEvent(
                brawlStarsId = 10000001L,
                modeBrawlStarsName = "knockout",
                modeBrawlStarsId = 1L,
                map = "Flaring Phoenix",
            ),
        mode = "knockout",
        type = "ranked",
        result = "victory",
        duration = 117,
        starPlayerBrawlStarsTag = null,
        playerBrawlStarsTag = playerBrawlStarsTag,
        battleRank = null,
        trophyChange = 8,
        teams = createTeam3v3(),
    )

    private fun createSoloTeams(): List<List<BattleTeamPlayer>> =
        (1..10).map { idx ->
            listOf(
                BattleTeamPlayer(
                    brawlStarsTag = "#SOLO$idx",
                    name = "solo$idx",
                    brawler =
                        BattleTeamPlayerBrawler(
                            brawlStarsId = 10000000L + idx,
                            name = "brawler$idx",
                            power = 11,
                            trophies = 2000,
                            trophyChange = null,
                        ),
                ),
            )
        }

    private fun createTeam3v3(): List<List<BattleTeamPlayer>> =
        listOf(
            (1..3).map { idx ->
                BattleTeamPlayer(
                    brawlStarsTag = "#A$idx",
                    name = "a$idx",
                    brawler =
                        BattleTeamPlayerBrawler(
                            brawlStarsId = 10000000L + idx,
                            name = "brawler$idx",
                            power = 11,
                            trophies = 1500,
                            trophyChange = null,
                        ),
                )
            },
            (4..6).map { idx ->
                BattleTeamPlayer(
                    brawlStarsTag = "#B$idx",
                    name = "b$idx",
                    brawler =
                        BattleTeamPlayerBrawler(
                            brawlStarsId = 10000000L + idx,
                            name = "brawler$idx",
                            power = 11,
                            trophies = 1500,
                            trophyChange = null,
                        ),
                )
            },
        )
}
