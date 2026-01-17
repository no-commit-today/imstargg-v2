package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.storage.db.core.brawlstars.battle.BattleEntity
import com.imstargg.storage.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.storage.db.core.brawlstars.battle.BattleEntityPlayer
import com.imstargg.storage.db.core.brawlstars.battle.BattleEntityTeamPlayer
import com.imstargg.storage.db.core.brawlstars.battle.BattleEntityTeamPlayerBrawler
import com.imstargg.storage.db.core.brawlstars.battle.BattleJpaRepository
import com.imstargg.storage.db.core.brawlstars.player.PlayerEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerEntityClub
import com.imstargg.storage.db.core.test.TestDataManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class BattleUpdateApplierTest(
    private val sut: BattleUpdateApplier,
    private val testDataManager: TestDataManager,
    private val battleRepository: BattleJpaRepository,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        testDataManager.cleanUp()
    }

    @Test
    fun `기존_배틀이_없으면_모든_배틀을_저장한다`() {
        // given
        val player = createPlayer("#PLAYER1")
        testDataManager.add(player)

        val battles =
            listOf(
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)),
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)),
            )

        // when
        val result = sut.apply(player, battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(battleRepository.count()).isEqualTo(2)
    }

    @Test
    fun `기존_배틀이_있으면_최신_배틀_이후만_저장한다`() {
        // given
        val player = createPlayer("#PLAYER2")
        testDataManager.add(player)

        val existingBattleTime = OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        val existingBattle = createBattleEntity(player.id!!, existingBattleTime)
        testDataManager.add(existingBattle)

        val battles =
            listOf(
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)),
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)),
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 13, 0, 0, 0, ZoneOffset.UTC)),
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC)),
            )

        // when
        val result = sut.apply(player, battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.battleTime.toEpochSecond() }).containsExactlyInAnyOrder(
            OffsetDateTime.of(2024, 1, 1, 13, 0, 0, 0, ZoneOffset.UTC).toEpochSecond(),
            OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC).toEpochSecond(),
        )
        assertThat(battleRepository.count()).isEqualTo(3)
    }

    @Test
    fun `저장할_새_배틀이_없으면_빈_리스트를_반환한다`() {
        // given
        val player = createPlayer("#PLAYER3")
        testDataManager.add(player)

        val existingBattleTime = OffsetDateTime.of(2024, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC)
        val existingBattle = createBattleEntity(player.id!!, existingBattleTime)
        testDataManager.add(existingBattle)

        val battles =
            listOf(
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC)),
                createBattleEntity(player.id!!, OffsetDateTime.of(2024, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC)),
            )

        // when
        val result = sut.apply(player, battles)

        // then
        assertThat(result).isEmpty()
        assertThat(battleRepository.count()).isEqualTo(1)
    }

    private fun createPlayer(tag: String) =
        PlayerEntity(
            brawlStarsTag = tag,
            name = "TestPlayer",
            nameColor = "#FFFFFF",
            iconBrawlStarsId = 1L,
            trophies = 1000,
            highestTrophies = 1500,
            expLevel = 100,
            expPoints = 5000,
            qualifiedFromChampionshipChallenge = false,
            victories3vs3 = 100,
            victoriesSolo = 50,
            victoriesDuo = 30,
            bestRoboRumbleTime = 0,
            bestTimeAsBigBrawler = 0,
            club = PlayerEntityClub(brawlStarsTag = "#TESTCLUB", name = "TestClub"),
        )

    private fun createBattleEntity(
        playerId: Long,
        battleTime: OffsetDateTime,
    ) = BattleEntity(
        battleKey = "battle_key_${battleTime.toEpochSecond()}",
        battleTime = battleTime,
        event =
            BattleEntityEvent(
                brawlStarsId = 1L,
                mode = "gemGrab",
                map = "TestMap",
            ),
        mode = "gemGrab",
        type = "ranked",
        result = "victory",
        duration = 120,
        starPlayerBrawlStarsTag = null,
        player =
            BattleEntityPlayer(
                playerId = playerId,
                rank = null,
                trophyChange = 8,
            ),
        teams =
            listOf(
                listOf(
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER1",
                        name = "Player1",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 1L,
                                name = "Shelly",
                                power = 11,
                                trophies = 500,
                                trophyChange = 8,
                            ),
                    ),
                ),
            ),
    )
}
