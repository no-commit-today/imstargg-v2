package com.imstargg.core.application.statistics.player

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.aBattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.defaultBrawler
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class PlayerBrawlerStatsRepositoryTest(
    private val sut: PlayerBrawlerStatsRepository,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
) : CoreApiContextTest() {
    private val startAt = OffsetDateTime.parse("2026-04-01T00:00:00+09:00").toInstant()
    private val endAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00").toInstant()

    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `플레이어가 없으면 입력값으로 채운 빈 stats 를 반환한다`() {
        // when
        val result = sut.compute(BrawlStarsTag("#NONE"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.playerTag).isEqualTo(BrawlStarsTag("#NONE"))
        assertThat(result.battleType).isEqualTo(BattleType.RANKED)
        assertThat(result.startBattleDate).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(result.endBattleDate).isEqualTo(LocalDate.of(2026, 5, 1))
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `본인 브롤러의 승무패를 ResultCount 로 집계한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            battleJpaRepository.save(
                aBattleEntity()
                    .withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse("2026-04-10T12:00:00+09:00").toInstant())
                    .withType("ranked")
                    .withResult("victory")
                    .withBattleKey("k1")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity()
                .withBattle(battle)
                .withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000000L))
                .build(),
        )

        // when
        val result = sut.compute(BrawlStarsTag("#ME"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.brawlerId).isEqualTo(BrawlStarsId(16000000L))
        assertThat(item.resultCount.victoryCount).isEqualTo(1L)
        assertThat(item.resultCount.defeatCount).isEqualTo(0L)
        assertThat(item.resultCount.drawCount).isEqualTo(0L)
    }
}
