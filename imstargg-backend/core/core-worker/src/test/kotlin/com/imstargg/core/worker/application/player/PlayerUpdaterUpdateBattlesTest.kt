package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant

class PlayerUpdaterUpdateBattlesTest(
    private val sut: PlayerUpdater,
    private val cleanUp: CleanUp,
    private val playerRepository: PlayerJpaRepository,
    private val battleRepository: BattleJpaRepository,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `기존_배틀이_없으면_모든_배틀을_저장한다`() {
        // given
        val tag = "#PLAYER1"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        val battles =
            listOf(
                createBattle(tag, Instant.parse("2024-01-01T10:00:00Z")),
                createBattle(tag, Instant.parse("2024-01-01T11:00:00Z")),
            )

        // when
        val result = sut.update(createPlayerResponse(tag), battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(battleRepository.count()).isEqualTo(2)
    }

    @Test
    fun `기존_배틀이_있으면_최신_배틀_이후만_저장한다`() {
        // given
        val tag = "#PLAYER2"
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        val existingBattleTime = Instant.parse("2024-01-01T12:00:00Z")
        battleRepository.save(createBattleEntity(player, existingBattleTime))

        val battles =
            listOf(
                createBattle(tag, Instant.parse("2024-01-01T11:00:00Z")),
                createBattle(tag, Instant.parse("2024-01-01T12:00:00Z")),
                createBattle(tag, Instant.parse("2024-01-01T13:00:00Z")),
                createBattle(tag, Instant.parse("2024-01-01T14:00:00Z")),
            )

        // when
        val result = sut.update(createPlayerResponse(tag), battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.battleAt.epochSecond }).containsExactlyInAnyOrder(
            Instant.parse("2024-01-01T13:00:00Z").epochSecond,
            Instant.parse("2024-01-01T14:00:00Z").epochSecond,
        )
        assertThat(battleRepository.count()).isEqualTo(3)
    }

    @Test
    fun `저장할_새_배틀이_없으면_빈_리스트를_반환한다`() {
        // given
        val tag = "#PLAYER3"
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        val existingBattleTime = Instant.parse("2024-01-01T15:00:00Z")
        battleRepository.save(createBattleEntity(player, existingBattleTime))

        val battles =
            listOf(
                createBattle(tag, Instant.parse("2024-01-01T14:00:00Z")),
                createBattle(tag, Instant.parse("2024-01-01T15:00:00Z")),
            )

        // when
        val result = sut.update(createPlayerResponse(tag), battles)

        // then
        assertThat(result).isEmpty()
        assertThat(battleRepository.count()).isEqualTo(1)
    }

    @Test
    fun `최신_배틀과_같은_시각의_미저장_배틀도_저장한다`() {
        // given
        val tag = "#PLAYER4"
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        val boundaryTime = Instant.parse("2024-01-01T12:00:00Z")
        battleRepository.save(createBattleEntity(player, boundaryTime))

        val battles =
            listOf(
                // 같은 초에 종료됐지만 아직 저장되지 않은 다른 배틀
                createBattle(tag, boundaryTime, battleKey = "battle_key_boundary_other"),
                // 이미 저장된 배틀과 같은 키 — 중복 저장 금지
                createBattle(tag, boundaryTime),
                createBattle(tag, Instant.parse("2024-01-01T13:00:00Z")),
            )

        // when
        val result = sut.update(createPlayerResponse(tag), battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.battleKey }).containsExactlyInAnyOrder(
            "battle_key_boundary_other",
            "battle_key_${Instant.parse("2024-01-01T13:00:00Z").epochSecond}",
        )
        assertThat(battleRepository.count()).isEqualTo(3)
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

    private fun createBattleEntity(
        player: PlayerEntity,
        battleTime: Instant,
    ) = aBattleEntity()
        .withBattleKey("battle_key_${battleTime.epochSecond}")
        .withBattleAt(battleTime)
        .withPlayer(player)
        .withBattleRank(null)
        .withTrophyChange(8)
        .build()

    private fun createBattle(
        playerBrawlStarsTag: String,
        battleTime: Instant,
        battleKey: String = "battle_key_${battleTime.epochSecond}",
    ) = Battle(
        battleKey = battleKey,
        battleAt = battleTime,
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
