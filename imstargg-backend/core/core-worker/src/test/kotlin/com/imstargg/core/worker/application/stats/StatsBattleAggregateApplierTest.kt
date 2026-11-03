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

    @Nested
    inner class `apply 브롤러 이름` {
        @Test
        fun `브롤러 이름이 null 이면 통계 JSON 에 빈 문자열로 저장한다`() {
            // given
            val battleAt = clock.instant().minus(Duration.ofHours(1))
            val teams =
                createTeam3v3().map { team ->
                    team.map { player ->
                        if (player.brawlStarsTag == "#A1") {
                            BattleTeamPlayer(
                                brawlStarsTag = player.brawlStarsTag,
                                name = player.name,
                                brawler =
                                    BattleTeamPlayerBrawler(
                                        brawlStarsId = player.brawler.brawlStarsId,
                                        name = null,
                                        power = player.brawler.power,
                                        trophies = player.brawler.trophies,
                                        trophyChange = player.brawler.trophyChange,
                                    ),
                            )
                        } else {
                            player
                        }
                    }
                }
            val battle =
                createResultBattle(
                    battleKey = "hidden-brawler-battle-key",
                    battleAt = battleAt,
                    playerBrawlStarsTag = "#A1",
                    teams = teams,
                )

            // when
            sut.apply(listOf(battle))

            // then
            val brawlerNames =
                statsResultBattleRepository
                    .findAll()
                    .single()
                    .teams
                    .flatten()
                    .associate { it.brawlStarsTag to it.brawler.name }
            assertThat(brawlerNames["#A1"]).isEqualTo("")
            assertThat(brawlerNames["#A2"]).isEqualTo("brawler2")
        }
    }

    @Nested
    inner class `apply 수집 모드` {
        @Test
        fun `허용 목록 밖 모드의 결과형 전투는 저장하지 않는다`() {
            // given
            val battle =
                createResultBattle(
                    battleKey = "wipeout-battle-key",
                    battleAt = recentBattleAt(),
                    playerBrawlStarsTag = "#A1",
                    modeBrawlStarsId = 25L,
                    mode = "wipeout",
                )

            // when
            sut.apply(listOf(battle))

            // then
            assertThat(statsResultBattleRepository.count()).isZero()
        }

        @Test
        fun `듀얼은 2x3 으로 펼쳐져도 저장하지 않는다`() {
            // given
            val battle =
                createResultBattle(
                    battleKey = "duel-battle-key",
                    battleAt = recentBattleAt(),
                    playerBrawlStarsTag = "#DUEL0",
                    teams = createDuelTeams(),
                    modeBrawlStarsId = 24L,
                    mode = "duels",
                )

            // when
            sut.apply(listOf(battle))

            // then
            assertThat(statsResultBattleRepository.count()).isZero()
        }

        @Test
        fun `mode 문자열이 아니라 modeId 로 판정한다`() {
            // given
            val battleAt = recentBattleAt()
            val battles =
                listOf(
                    createResultBattle(
                        battleKey = "hockey-battle-key",
                        battleAt = battleAt,
                        playerBrawlStarsTag = "#A1",
                        modeBrawlStarsId = 45L,
                        mode = "brawlBall",
                    ),
                    createResultBattle(
                        battleKey = "wipeout-battle-key",
                        battleAt = battleAt,
                        playerBrawlStarsTag = "#A1",
                        modeBrawlStarsId = 25L,
                        mode = "gemGrab",
                    ),
                )

            // when
            sut.apply(battles)

            // then
            assertThat(statsResultBattleRepository.findAll().map { it.eventModeBrawlStarsId })
                .containsExactly(45L)
        }

        @Test
        fun `핵심 모드라도 팀 구성이 다르면 저장하지 않는다`() {
            // given
            val battle =
                createResultBattle(
                    battleKey = "2x2-battle-key",
                    battleAt = recentBattleAt(),
                    playerBrawlStarsTag = "#T0P0",
                    teams = createTeams(teamCount = 2, teamSize = 2),
                    modeBrawlStarsId = 20L,
                )

            // when
            sut.apply(listOf(battle))

            // then
            assertThat(statsResultBattleRepository.count()).isZero()
        }

        @Test
        fun `결과형 모드의 순위 전투는 순위형으로 저장하지 않는다`() {
            // given
            val battle =
                createRankBattle(
                    battleKey = "knockout-rank-battle-key",
                    battleAt = recentBattleAt(),
                    playerBrawlStarsTag = "#T0P0",
                    modeBrawlStarsId = 20L,
                    teams = createTeams(teamCount = 2, teamSize = 3),
                    battleRank = 1,
                )

            // when
            sut.apply(listOf(battle))

            // then
            assertThat(statsRankBattleRepository.count()).isZero()
        }

        @Test
        fun `쇼다운의 결과 전투는 결과형으로 저장하지 않는다`() {
            // given
            val battle =
                createRankBattle(
                    battleKey = "showdown-result-battle-key",
                    battleAt = recentBattleAt(),
                    playerBrawlStarsTag = "#SOLO1",
                    modeBrawlStarsId = 6L,
                    battleRank = null,
                    result = "victory",
                )

            // when
            sut.apply(listOf(battle))

            // then
            assertThat(statsResultBattleRepository.count()).isZero()
        }

        @Test
        fun `솔로·듀오·트리오 쇼다운은 순위형으로 저장된다`() {
            // given
            val battleAt = recentBattleAt()
            val battles =
                listOf(
                    createRankBattle(
                        battleKey = "solo-battle-key",
                        battleAt = battleAt,
                        playerBrawlStarsTag = "#T0P0",
                        modeBrawlStarsId = 6L,
                        teams = createTeams(teamCount = 10, teamSize = 1),
                    ),
                    createRankBattle(
                        battleKey = "duo-battle-key",
                        battleAt = battleAt,
                        playerBrawlStarsTag = "#T0P0",
                        modeBrawlStarsId = 9L,
                        teams = createTeams(teamCount = 5, teamSize = 2),
                    ),
                    createRankBattle(
                        battleKey = "trio-battle-key",
                        battleAt = battleAt,
                        playerBrawlStarsTag = "#T0P0",
                        modeBrawlStarsId = 38L,
                        teams = createTeams(teamCount = 4, teamSize = 3),
                    ),
                )

            // when
            sut.apply(battles)

            // then
            assertThat(statsRankBattleRepository.findAll().map { it.eventModeBrawlStarsId })
                .containsExactlyInAnyOrder(6L, 9L, 38L)
        }
    }

    private fun recentBattleAt(): Instant = clock.instant().minus(Duration.ofHours(1))

    private fun createRankBattle(
        battleKey: String,
        battleAt: Instant,
        playerBrawlStarsTag: String,
        modeBrawlStarsId: Long = 6L,
        teams: List<List<BattleTeamPlayer>> = createSoloTeams(),
        battleRank: Int? = 1,
        result: String? = null,
    ) = Battle(
        battleKey = battleKey,
        battleAt = battleAt,
        event =
            BattleEvent(
                brawlStarsId = 10000001L,
                modeBrawlStarsName = "soloShowdown",
                modeBrawlStarsId = modeBrawlStarsId,
                map = "Flying Fantasies",
            ),
        mode = "soloShowdown",
        type = "soloRanked",
        result = result,
        duration = 117,
        starPlayerBrawlStarsTag = null,
        playerBrawlStarsTag = playerBrawlStarsTag,
        battleRank = battleRank,
        trophyChange = null,
        teams = teams,
    )

    private fun createResultBattle(
        battleKey: String,
        battleAt: Instant,
        playerBrawlStarsTag: String,
        teams: List<List<BattleTeamPlayer>> = createTeam3v3(),
        modeBrawlStarsId: Long = 20L,
        mode: String = "knockout",
        battleRank: Int? = null,
        result: String? = "victory",
    ) = Battle(
        battleKey = battleKey,
        battleAt = battleAt,
        event =
            BattleEvent(
                brawlStarsId = 10000001L,
                modeBrawlStarsName = mode,
                modeBrawlStarsId = modeBrawlStarsId,
                map = "Flaring Phoenix",
            ),
        mode = mode,
        type = "ranked",
        result = result,
        duration = 117,
        starPlayerBrawlStarsTag = null,
        playerBrawlStarsTag = playerBrawlStarsTag,
        battleRank = battleRank,
        trophyChange = 8,
        teams = teams,
    )

    private fun createTeams(
        teamCount: Int,
        teamSize: Int,
    ): List<List<BattleTeamPlayer>> =
        (0 until teamCount).map { teamIdx ->
            (0 until teamSize).map { playerIdx ->
                // 브롤러 ID 가 전투 안에서 고유해야 브롤러 중복 검사가 아니라 모드·팀 구성 검사로 걸러진다
                val idx = teamIdx * teamSize + playerIdx + 1
                BattleTeamPlayer(
                    brawlStarsTag = "#T${teamIdx}P$playerIdx",
                    name = "t${teamIdx}p$playerIdx",
                    brawler =
                        BattleTeamPlayerBrawler(
                            brawlStarsId = 10000000L + idx,
                            name = "brawler$idx",
                            power = 11,
                            trophies = 2000,
                            trophyChange = null,
                        ),
                )
            }
        }

    // BattleBuilder 가 듀얼을 펼친 모양이다 — 플레이어 1명이 같은 태그로 브롤러 3개를 낸 팀 2개
    private fun createDuelTeams(): List<List<BattleTeamPlayer>> =
        (0 until 2).map { teamIdx ->
            (0 until 3).map { brawlerIdx ->
                val idx = teamIdx * 3 + brawlerIdx + 1
                BattleTeamPlayer(
                    brawlStarsTag = "#DUEL$teamIdx",
                    name = "duel$teamIdx",
                    brawler =
                        BattleTeamPlayerBrawler(
                            brawlStarsId = 10000000L + idx,
                            name = "brawler$idx",
                            power = 11,
                            trophies = 2000,
                            trophyChange = null,
                        ),
                )
            }
        }

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
