package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerBuffyEntityFixture.Companion.aPlayerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerGadgetEntityFixture.Companion.aPlayerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerGearEntityFixture.Companion.aPlayerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerHyperChargeEntityFixture.Companion.aPlayerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerStarPowerEntityFixture.Companion.aPlayerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class BrawlerItemOwnershipStatsReaderTest(
    private val sut: BrawlerItemOwnershipStatsReader,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val playerJpaRepository: PlayerJpaRepository,
    private val playerBrawlerJpaRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGearJpaRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerGadgetJpaRepository: PlayerBrawlerGadgetJpaRepository,
    private val playerBrawlerStarPowerJpaRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerHyperChargeJpaRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val playerBrawlerBuffyJpaRepository: PlayerBrawlerBuffyJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `stats_player 에 등록된 플레이어의 브롤러별 아이템 소유 스냅샷을 읽는다`() {
        // given - stats_player 에 등록된 P1 (브롤러 A: 아이템 5종, 브롤러 B: 아이템 없음)
        statsPlayerJpaRepository.save(StatsPlayerEntity(brawlStarsTag = "#P1"))
        val player1 = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#P1").build())
        val brawlerA =
            playerBrawlerJpaRepository.save(
                aPlayerBrawlerEntity()
                    .withPlayer(player1)
                    .withBrawlerBrawlStarsId(16000001L)
                    .withTrophies(600)
                    .build(),
            )
        playerBrawlerJpaRepository.save(
            aPlayerBrawlerEntity()
                .withPlayer(player1)
                .withBrawlerBrawlStarsId(16000002L)
                .withTrophies(100)
                .build(),
        )
        playerBrawlerGearJpaRepository.save(
            aPlayerBrawlerGearEntity().withPlayerBrawler(brawlerA).withGearBrawlStarsId(62000001L).build(),
        )
        playerBrawlerGadgetJpaRepository.save(
            aPlayerBrawlerGadgetEntity().withPlayerBrawler(brawlerA).withGadgetBrawlStarsId(23000001L).build(),
        )
        playerBrawlerStarPowerJpaRepository.save(
            aPlayerBrawlerStarPowerEntity().withPlayerBrawler(brawlerA).withStarPowerBrawlStarsId(23000101L).build(),
        )
        playerBrawlerHyperChargeJpaRepository.save(
            aPlayerBrawlerHyperChargeEntity().withPlayerBrawler(brawlerA).withHyperChargeBrawlStarsId(23000201L).build(),
        )
        playerBrawlerBuffyJpaRepository.save(
            aPlayerBrawlerBuffyEntity().withPlayerBrawler(brawlerA).withBuffyType(BuffyType.entries.first()).build(),
        )

        // stats_player 미등록 P2 는 결과에서 제외되어야 한다
        val player2 = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#P2").build())
        playerBrawlerJpaRepository.save(
            aPlayerBrawlerEntity().withPlayer(player2).withBrawlerBrawlStarsId(16000003L).build(),
        )

        // when
        val snapshots = mutableListOf<PlayerBrawlerOwnershipSnapshot>()
        val totalRead = sut.read { snapshots += it }

        // then - totalRead 는 stats_player 수
        assertThat(totalRead).isEqualTo(1L)
        assertThat(snapshots).hasSize(2)
        assertThat(snapshots.map { it.brawlerBrawlStarsId })
            .containsExactlyInAnyOrder(16000001L, 16000002L)

        val snapshotA = snapshots.first { it.brawlerBrawlStarsId == 16000001L }
        assertThat(snapshotA.playerBrawlerId).isEqualTo(brawlerA.id)
        assertThat(snapshotA.trophies).isEqualTo(600)
        assertThat(snapshotA.gearBrawlStarsIds).containsExactly(62000001L)
        assertThat(snapshotA.gadgetBrawlStarsIds).containsExactly(23000001L)
        assertThat(snapshotA.starPowerBrawlStarsIds).containsExactly(23000101L)
        assertThat(snapshotA.hyperChargeBrawlStarsIds).containsExactly(23000201L)
        assertThat(snapshotA.buffyTypes).containsExactly(BuffyType.entries.first())

        val snapshotB = snapshots.first { it.brawlerBrawlStarsId == 16000002L }
        assertThat(snapshotB.gearBrawlStarsIds).isEmpty()
        assertThat(snapshotB.buffyTypes).isEmpty()
    }
}
