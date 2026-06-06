package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.fixture.BattleEntityFixture.Companion.aBattleEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.aBattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleTeamPlayerEntityFixture.Companion.defaultBrawler
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class BattleJpaRepositoryCustomImplTest(
    private val sut: BattleJpaRepository,
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
) : DbCoreContextTest() {
    private val startAt = OffsetDateTime.parse("2026-04-01T00:00:00+09:00").toInstant()
    private val endAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00").toInstant()

    @Test
    fun `본인 브롤러의 결과별 카운트를 집계한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-11T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-12T12:00:00+09:00", "ranked", "defeat", 16000000L)

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactlyInAnyOrder(
            PlayerBrawlerResultProjection(16000000L, "victory", 2L),
            PlayerBrawlerResultProjection(16000000L, "defeat", 1L),
        )
    }

    @Test
    fun `본인 태그가 아닌 팀플레이어 브롤러는 제외한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            sut.save(
                aBattleEntity()
                    .withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse("2026-04-10T12:00:00+09:00").toInstant())
                    .withType("ranked")
                    .withResult("victory")
                    .withBattleKey("kb")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity()
                .withBattle(battle)
                .withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000111L))
                .build(),
        )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity()
                .withBattle(battle)
                .withBrawlStarsTag("#OTHER")
                .withBrawler(defaultBrawler(brawlStarsId = 16000999L))
                .build(),
        )

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000111L, "victory", 1L))
    }

    @Test
    fun `시즌 시각 경계(startAt 이상 endAt 미만)만 집계한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-03-31T23:59:59+09:00", "ranked", "victory", 16000000L) // startAt 이전 → 제외
        saveOwnBattle(player, "2026-04-01T00:00:00+09:00", "ranked", "victory", 16000000L) // startAt 포함
        saveOwnBattle(player, "2026-04-30T23:59:59+09:00", "ranked", "victory", 16000000L) // endAt 직전 포함
        saveOwnBattle(player, "2026-05-01T00:00:00+09:00", "ranked", "victory", 16000000L) // endAt → 제외

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 2L))
    }

    @Test
    fun `요청한 battleType 코드와 다른 배틀은 제외한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-10T13:00:00+09:00", "friendly", "victory", 16000000L)

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 1L))
    }

    @Test
    fun `result 가 null 인 배틀은 제외한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        saveOwnBattle(player, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        saveOwnBattle(player, "2026-04-10T13:00:00+09:00", "ranked", null, 16000000L)

        // when
        val result = sut.aggregatePlayerBrawlerResults(player.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 1L))
    }

    @Test
    fun `다른 플레이어가 소유한 배틀은 제외한다`() {
        // given
        val me = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val other = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#OTHER").build())
        saveOwnBattle(me, "2026-04-10T12:00:00+09:00", "ranked", "victory", 16000000L)
        val otherBattle =
            sut.save(
                aBattleEntity()
                    .withPlayer(other)
                    .withBattleAt(OffsetDateTime.parse("2026-04-11T12:00:00+09:00").toInstant())
                    .withType("ranked")
                    .withResult("victory")
                    .withBattleKey("ob")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity()
                .withBattle(otherBattle)
                .withBrawlStarsTag("#ME")
                .withBrawler(defaultBrawler(brawlStarsId = 16000000L))
                .build(),
        )

        // when
        val result = sut.aggregatePlayerBrawlerResults(me.id!!, "#ME", "ranked", startAt, endAt)

        // then
        assertThat(result).containsExactly(PlayerBrawlerResultProjection(16000000L, "victory", 1L))
    }

    private fun saveOwnBattle(
        player: PlayerEntity,
        at: String,
        type: String,
        result: String?,
        brawlerId: Long,
    ): BattleEntity {
        val battle =
            sut.save(
                aBattleEntity()
                    .withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse(at).toInstant())
                    .withType(type)
                    .withResult(result)
                    .withBattleKey("k-$at-$brawlerId")
                    .build(),
            )
        battleTeamPlayerJpaRepository.save(
            aBattleTeamPlayerEntity()
                .withBattle(battle)
                .withBrawlStarsTag(player.brawlStarsTag)
                .withBrawler(defaultBrawler(brawlStarsId = brawlerId))
                .build(),
        )
        return battle
    }
}
