package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.AccessoryResponse
import com.imstargg.client.brawlstars.response.BrawlerBuffiesResponse
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.GearStatResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.client.brawlstars.response.SkinResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlayerUpdaterConcurrencyTest(
    private val sut: PlayerUpdater,
    private val playerRepository: PlayerJpaRepository,
    private val playerBrawlerRepository: PlayerBrawlerJpaRepository,
    private val battleRepository: BattleJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `동시에 같은 태그로 처음 update하면 재시도로 모두 성공하고 하나만 저장된다`() {
        // given
        val tag = "#PLAYER1"
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createPlayerResponse(tag), emptyList())
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(playerRepository.count()).isEqualTo(1)
    }

    @Test
    fun `동시에 같은 플레이어를 update하면 모두 성공한다`() {
        // given
        val tag = "#PLAYER2"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createPlayerResponse(tag), emptyList())
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(playerRepository.count()).isEqualTo(1)
    }

    @Test
    fun `동시에 같은 배틀을 update하면 중복 저장되지 않는다`() {
        // given
        val tag = "#PLAYER3"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())
        val battles =
            listOf(
                createBattle(tag, Instant.parse("2024-01-01T10:00:00Z")),
                createBattle(tag, Instant.parse("2024-01-01T11:00:00Z")),
            )
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(createPlayerResponse(tag), battles)
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(battleRepository.count()).isEqualTo(2)
    }

    @Test
    fun `동시에 같은 브롤러를 update하면 중복 저장되지 않는다`() {
        // given
        val tag = "#PLAYER4"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())
        val playerResponse =
            createPlayerResponse(
                tag = tag,
                brawlers = listOf(createBrawlerStatResponse(brawlerId = 1L)),
            )
        val barrier = CyclicBarrier(2)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    barrier.await()
                    try {
                        sut.update(playerResponse, emptyList())
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        assertThat(results).allMatch { it == null }
        assertThat(playerBrawlerRepository.count()).isEqualTo(1)
    }

    private fun createPlayerResponse(
        tag: String,
        brawlers: List<BrawlerStatResponse> = emptyList(),
    ) = PlayerResponse(
        tag = tag,
        name = "Test",
        nameColor = "0xff0000ff",
        icon = PlayerIconResponse(id = 1L),
        trophies = 1000,
        highestTrophies = 1200,
        expLevel = 50,
        expPoints = 5000,
        isQualifiedFromChampionshipChallenge = false,
        victories3vs3 = 10,
        victoriesSolo = 5,
        victoriesDuo = 3,
        bestRoboRumbleTime = 0,
        bestTimeAsBigBrawler = 0,
        totalPrestigeLevel = 0,
        club = PlayerClubResponse(tag = "#CLUBTEST", name = "Test Club"),
        brawlers = brawlers,
    )

    private fun createBrawlerStatResponse(brawlerId: Long) =
        BrawlerStatResponse(
            id = brawlerId,
            name = "Test Brawler",
            power = 11,
            rank = 20,
            trophies = 500,
            highestTrophies = 600,
            prestigeLevel = 0,
            maxWinStreak = 5,
            currentWinStreak = 0,
            buffies = BrawlerBuffiesResponse(hyperCharge = false, gadget = false, starPower = false),
            skin = SkinResponse(id = 1L, name = "Default"),
            gears = emptyList<GearStatResponse>(),
            starPowers = emptyList<AccessoryResponse>(),
            gadgets = emptyList<AccessoryResponse>(),
            hyperCharges = emptyList<AccessoryResponse>(),
        )

    private fun createBattle(
        playerBrawlStarsTag: String,
        battleAt: Instant,
    ) = Battle(
        battleKey = "battle_key_${battleAt.epochSecond}",
        battleAt = battleAt,
        event =
            BattleEvent(
                brawlStarsId = 1L,
                modeBrawlStarsName = "knockout",
                modeBrawlStarsId = 1,
                map = "Test Map",
            ),
        mode = "knockout",
        type = "ranked",
        result = "victory",
        duration = 117,
        starPlayerBrawlStarsTag = null,
        playerBrawlStarsTag = playerBrawlStarsTag,
        battleRank = null,
        trophyChange = 8,
        teams = emptyList(),
    )
}
