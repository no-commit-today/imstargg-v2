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
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheValue
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class PlayerBrawlerStatsServiceTest(
    private val sut: PlayerBrawlerStatsService,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
) : CoreApiContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var cacheStore: PlayerBrawlerStatsCacheStore

    private val startAt = OffsetDateTime.parse("2026-04-01T00:00:00+09:00")
    private val endAt = OffsetDateTime.parse("2026-05-01T00:00:00+09:00")

    @BeforeEach
    fun setUp() {
        cleanUp.all()
        // 기본은 캐시 미스. (relaxed 모킹의 nullable 반환이 환경에 따라 null 이 아닐 수 있어 명시 stub)
        every { cacheStore.find(any(), any(), any(), any()) } returns null
    }

    @Test
    fun `캐시 미스면 battle 에서 계산하고 캐시에 저장한다`() {
        // given
        val player = playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ME").build())
        val battle =
            battleJpaRepository.save(
                aBattleEntity()
                    .withPlayer(player)
                    .withBattleAt(OffsetDateTime.parse("2026-04-10T12:00:00+09:00"))
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
        val result = sut.get(BrawlStarsTag("#ME"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(16000000L))
        assertThat(
            result.items
                .first()
                .resultCount.victoryCount,
        ).isEqualTo(1L)
        verify { cacheStore.put("#ME", BattleType.RANKED, startAt.toLocalDate(), endAt.toLocalDate(), any()) }
    }

    @Test
    fun `캐시 히트면 계산하지 않고 캐시 값을 반환한다`() {
        // given (DB 에는 배틀이 없지만 캐시가 값을 돌려줌 = 계산을 거치지 않음)
        every {
            cacheStore.find("#ME", BattleType.RANKED, startAt.toLocalDate(), endAt.toLocalDate())
        } returns
            PlayerBrawlerStatsCacheValue(
                items = listOf(PlayerBrawlerStatsCacheValue.Item(16000000L, 5L, 1L, 0L)),
            )

        // when
        val result = sut.get(BrawlStarsTag("#ME"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(16000000L))
        assertThat(
            result.items
                .first()
                .resultCount.victoryCount,
        ).isEqualTo(5L)
        assertThat(
            result.items
                .first()
                .resultCount.defeatCount,
        ).isEqualTo(1L)
        verify(exactly = 0) { cacheStore.put(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `배틀이 없으면 빈 items 를 반환한다`() {
        // when
        val result = sut.get(BrawlStarsTag("#NONE"), BattleType.RANKED, startAt, endAt)

        // then
        assertThat(result.items).isEmpty()
        assertThat(result.startBattleDate).isEqualTo(startAt.toLocalDate())
        assertThat(result.endBattleDate).isEqualTo(endAt.toLocalDate())
    }
}
