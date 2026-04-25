package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleTeamPlayer
import com.imstargg.core.worker.domain.BattleTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsItemJpaRepository
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class PlayerBrawlerStatsAccumulatorTest(
    private val sut: PlayerBrawlerStatsAccumulator,
    private val cleanUp: CleanUp,
    private val playerRepository: PlayerJpaRepository,
    private val statsRepository: PlayerBrawlerStatsJpaRepository,
    private val itemRepository: PlayerBrawlerStatsItemJpaRepository,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `RANKED 배틀 1건이 들어오면 시즌 헤더와 브롤러 아이템이 생성된다`() {
        // given
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag("#P1").build())
        val battleAt = OffsetDateTime.parse("2026-04-15T12:00:00+09:00")
        val battle = battle(player.brawlStarsTag, battleAt, type = "ranked", result = "victory", brawlerId = 16000000L)

        // when
        sut.accumulate(player, listOf(battle))

        // then
        val headers = statsRepository.findAll()
        assertThat(headers).hasSize(1)
        val header = headers.first()
        assertThat(header.playerBrawlStarsTag).isEqualTo("#P1")
        assertThat(header.startBattleDate).isBefore(LocalDate.parse("2026-04-15").plusDays(1))

        val items = itemRepository.findAllByPlayerBrawlerStatsId(header.id!!)
        assertThat(items).hasSize(1)
        val item = items.first()
        assertThat(item.brawlerBrawlStarsId).isEqualTo(16000000L)
        assertThat(item.resultCount.victoryCount).isEqualTo(1)
        assertThat(item.resultCount.defeatCount).isEqualTo(0)
        assertThat(item.resultCount.totalCount).isEqualTo(1)
    }

    @Test
    fun `같은 시즌 같은 브롤러 배틀이 누적되면 단일 row 의 카운트가 증가한다`() {
        // given
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag("#P2").build())
        val battles =
            listOf(
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T12:00:00+09:00"), "ranked", "victory", 16000000L),
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T13:00:00+09:00"), "ranked", "defeat", 16000000L),
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T14:00:00+09:00"), "ranked", "draw", 16000000L),
            )

        // when
        sut.accumulate(player, battles)

        // then
        assertThat(statsRepository.count()).isEqualTo(1)
        val items = itemRepository.findAll()
        assertThat(items).hasSize(1)
        with(items.first().resultCount) {
            assertThat(victoryCount).isEqualTo(1)
            assertThat(defeatCount).isEqualTo(1)
            assertThat(drawCount).isEqualTo(1)
            assertThat(totalCount).isEqualTo(3)
        }
    }

    @Test
    fun `RANKED 와 SOLO_RANKED 는 별도 헤더로 분리된다`() {
        // given
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag("#P3").build())
        val battles =
            listOf(
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T12:00:00+09:00"), "ranked", "victory", 16000000L),
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T13:00:00+09:00"), "soloRanked", "defeat", 16000001L),
            )

        // when
        sut.accumulate(player, battles)

        // then
        val headers = statsRepository.findAll()
        assertThat(headers).hasSize(2)
        assertThat(headers.map { it.battleType.name }).containsExactlyInAnyOrder("RANKED", "SOLO_RANKED")
    }

    @Test
    fun `FRIENDLY 등 비대상 타입은 무시된다`() {
        // given
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag("#P4").build())
        val battles =
            listOf(
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T12:00:00+09:00"), "friendly", "victory", 16000000L),
                battle(player.brawlStarsTag, OffsetDateTime.parse("2026-04-15T13:00:00+09:00"), "challenge", "victory", 16000000L),
            )

        // when
        sut.accumulate(player, battles)

        // then
        assertThat(statsRepository.count()).isZero()
        assertThat(itemRepository.count()).isZero()
    }

    @Test
    fun `다른 플레이어가 사용한 브롤러는 집계되지 않는다`() {
        // given
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            Battle(
                battleKey = "k1",
                battleAt = OffsetDateTime.parse("2026-04-15T12:00:00+09:00"),
                event = null,
                mode = null,
                type = "ranked",
                result = "victory",
                duration = null,
                starPlayerBrawlStarsTag = null,
                playerBrawlStarsTag = player.brawlStarsTag,
                battleRank = null,
                trophyChange = null,
                teams =
                    listOf(
                        listOf(teamPlayer("#OTHER", brawlerId = 16000999L)),
                        listOf(teamPlayer(player.brawlStarsTag, brawlerId = 16000111L)),
                    ),
            )

        // when
        sut.accumulate(player, listOf(battle))

        // then
        val items = itemRepository.findAll()
        assertThat(items).hasSize(1)
        assertThat(items.first().brawlerBrawlStarsId).isEqualTo(16000111L)
    }

    @Test
    fun `SOLO_RANKED createFirst 이전 배틀은 무시된다`() {
        // given
        val player = playerRepository.save(aPlayerEntity().withBrawlStarsTag("#P5").build())
        val battle =
            battle(
                player.brawlStarsTag,
                OffsetDateTime.parse("2024-01-01T12:00:00+09:00"),
                type = "soloRanked",
                result = "victory",
                brawlerId = 16000000L,
            )

        // when
        sut.accumulate(player, listOf(battle))

        // then
        assertThat(statsRepository.count()).isZero()
        assertThat(itemRepository.count()).isZero()
    }

    private fun battle(
        playerTag: String,
        battleAt: OffsetDateTime,
        type: String,
        result: String,
        brawlerId: Long,
    ): Battle =
        Battle(
            battleKey = "key-$battleAt-$type-$brawlerId",
            battleAt = battleAt,
            event = null,
            mode = null,
            type = type,
            result = result,
            duration = null,
            starPlayerBrawlStarsTag = null,
            playerBrawlStarsTag = playerTag,
            battleRank = null,
            trophyChange = null,
            teams = listOf(listOf(teamPlayer(playerTag, brawlerId))),
        )

    private fun teamPlayer(
        tag: String,
        brawlerId: Long,
    ): BattleTeamPlayer =
        BattleTeamPlayer(
            brawlStarsTag = tag,
            name = "name",
            brawler =
                BattleTeamPlayerBrawler(
                    brawlStarsId = brawlerId,
                    name = "BRAWLER",
                    power = 11,
                    trophies = 1000,
                    trophyChange = null,
                ),
        )
}
