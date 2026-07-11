package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant

class PlayerUpdaterCacheEvictionTest(
    private val sut: PlayerUpdater,
    private val cleanUp: CleanUp,
    private val playerRepository: PlayerJpaRepository,
) : CoreWorkerContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var cacheStore: PlayerBrawlerStatsCacheStore

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `신규 배틀이 있으면 커밋 후 해당 플레이어 캐시를 무효화한다`() {
        // given
        val tag = "#EVICT1"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        // when
        sut.update(
            createPlayerResponse(tag),
            listOf(createBattle(tag, Instant.parse("2026-04-01T10:00:00Z"))),
        )

        // then
        verify(exactly = 1) { cacheStore.evict(tag) }
    }

    @Test
    fun `신규 배틀이 없으면 캐시를 무효화하지 않는다`() {
        // given
        val tag = "#EVICT2"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        // when
        sut.update(createPlayerResponse(tag), emptyList())

        // then
        verify(exactly = 0) { cacheStore.evict(any()) }
    }

    private fun createPlayerResponse(tag: String) =
        PlayerResponse(
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
            brawlers = emptyList<BrawlerStatResponse>(),
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
