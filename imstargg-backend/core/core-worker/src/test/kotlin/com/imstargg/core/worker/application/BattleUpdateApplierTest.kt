package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.test.TestDataManager
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
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
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)),
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)),
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
        val existingBattle = createBattleEntity(player, existingBattleTime)
        testDataManager.add(existingBattle)

        val battles =
            listOf(
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC)),
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)),
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 13, 0, 0, 0, ZoneOffset.UTC)),
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC)),
            )

        // when
        val result = sut.apply(player, battles)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.battleAt.toEpochSecond() }).containsExactlyInAnyOrder(
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
        val existingBattle = createBattleEntity(player, existingBattleTime)
        testDataManager.add(existingBattle)

        val battles =
            listOf(
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneOffset.UTC)),
                createBattle(player.brawlStarsTag, OffsetDateTime.of(2024, 1, 1, 15, 0, 0, 0, ZoneOffset.UTC)),
            )

        // when
        val result = sut.apply(player, battles)

        // then
        assertThat(result).isEmpty()
        assertThat(battleRepository.count()).isEqualTo(1)
    }

    private fun createPlayer(tag: String) =
        aPlayerEntity()
            .withBrawlStarsTag(tag)
            .build()

    private fun createBattleEntity(
        player: PlayerEntity,
        battleTime: OffsetDateTime,
    ) = aBattleEntity()
        .withBattleKey("battle_key_${battleTime.toEpochSecond()}")
        .withBattleAt(battleTime)
        .withPlayer(player)
        .withBattleRank(null)
        .withTrophyChange(8)
        .build()

    private fun createBattle(
        playerBrawlStarsTag: String,
        battleTime: OffsetDateTime,
    ) = Battle(
        battleKey = "battle_key_${battleTime.toEpochSecond()}",
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
