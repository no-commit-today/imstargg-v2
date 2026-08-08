package com.imstargg.core.batch.job.brawlstars

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventEntityFixture.Companion.aBattleEventEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventModeEntityFixture.Companion.aBattleEventModeEntity
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.Chunk
import java.time.Instant
import java.time.OffsetDateTime

class BattleEventSyncItemWriterTest(
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    private val alertManager = mockk<AlertManager>(relaxUnitFun = true)

    private val sut =
        BattleEventSyncItemWriter<StatsRankBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `경쟁전 배틀의 이벤트를 배틀 시점의 시즌 키로 저장한다`() {
        // given
        saveRegisteredEvent(EVENT_ID)

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1"))))

        // then
        val saved = battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey(SEASON_2_1)
        assertThat(saved.map { it.eventBrawlStarsId }).containsExactly(EVENT_ID)
    }

    @Test
    fun `같은 청크에 같은 이벤트가 여러 번 있어도 한 번만 저장한다`() {
        // given
        saveRegisteredEvent(EVENT_ID)

        // when
        sut.write(
            Chunk(
                listOf(
                    soloRankBattle(battleKey = "battle-1"),
                    soloRankBattle(battleKey = "battle-2"),
                ),
            ),
        )

        // then
        val saved = battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey(SEASON_2_1)
        assertThat(saved).hasSize(1)
    }

    @Test
    fun `이미 등록된 시즌과 이벤트 조합은 다시 저장하지 않는다`() {
        // given
        saveRegisteredEvent(EVENT_ID)
        battleEventSoloRankEventJpaRepository.save(
            BattleEventSoloRankEventEntity(soloRankSeasonKey = SEASON_2_1, eventBrawlStarsId = EVENT_ID),
        )

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1"))))

        // then
        val saved = battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey(SEASON_2_1)
        assertThat(saved).hasSize(1)
    }

    @Test
    fun `경쟁전이 아닌 배틀의 이벤트는 저장하지 않는다`() {
        // given
        saveRegisteredEvent(EVENT_ID)

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1", type = BattleType.RANKED.code))))

        // then
        assertThat(battleEventSoloRankEventJpaRepository.findAll()).isEmpty()
    }

    @Test
    fun `같은 이벤트라도 배틀 시점이 다른 시즌이면 시즌별로 저장한다`() {
        // given - 시즌 1-1 의 마지막 시각과 시즌 1-2 의 첫 시각
        saveRegisteredEvent(EVENT_ID)
        val season1m1EndAt = OffsetDateTime.parse("2025-04-16T18:00:00+09:00").toInstant()
        val season1m2StartAt = OffsetDateTime.parse("2025-04-17T18:00:00+09:00").toInstant()

        // when
        sut.write(
            Chunk(
                listOf(
                    soloRankBattle(battleKey = "battle-1", battleAt = season1m1EndAt),
                    soloRankBattle(battleKey = "battle-2", battleAt = season1m2StartAt),
                ),
            ),
        )

        // then
        assertThat(battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey("1-1"))
            .extracting<Long> { it.eventBrawlStarsId }
            .containsExactly(EVENT_ID)
        assertThat(battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey("1-2"))
            .extracting<Long> { it.eventBrawlStarsId }
            .containsExactly(EVENT_ID)
    }

    @Test
    fun `시즌 사이 공백 구간의 배틀은 다음 시즌으로 귀속시킨다`() {
        // given - 시즌 1-1 종료(04-16 18:00) 와 시즌 1-2 시작(04-17 18:00) 사이
        saveRegisteredEvent(EVENT_ID)
        val betweenSeasons = OffsetDateTime.parse("2025-04-17T00:00:00+09:00").toInstant()

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1", battleAt = betweenSeasons))))

        // then
        assertThat(battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey("1-2"))
            .extracting<Long> { it.eventBrawlStarsId }
            .containsExactly(EVENT_ID)
    }

    @Test
    fun `등록되지 않은 이벤트를 발견하면 경고를 발송한다`() {
        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1"))))

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("미등록 배틀 이벤트")
        assertThat(alert.captured.content).contains(EVENT_ID.toString())
    }

    @Test
    fun `이미 확인한 이벤트는 다음 청크에서 다시 경고하지 않는다`() {
        // given
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1"))))

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-2"))))

        // then
        verify(exactly = 1) { alertManager.alert(any()) }
    }

    @Test
    fun `등록된 이벤트와 맵이 다르면 불일치 경고를 발송한다`() {
        // given
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(EVENT_ID)
                .withMode(null)
                .withMapBrawlStarsName(MAP_NAME)
                .build(),
        )

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1", eventMap = "Another Map"))))

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("배틀 이벤트 불일치")
    }

    @Test
    fun `등록된 이벤트와 모드가 다르면 불일치 경고를 발송한다`() {
        // given
        val mode = battleEventModeJpaRepository.save(aBattleEventModeEntity().withBrawlStarsId(MODE_ID).build())
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(EVENT_ID)
                .withMode(mode)
                .withMapBrawlStarsName(MAP_NAME)
                .build(),
        )

        // when
        sut.write(Chunk(listOf(soloRankBattle(battleKey = "battle-1", eventModeBrawlStarsId = MODE_ID + 1))))

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("배틀 이벤트 불일치")
    }

    /** 모드·맵이 배틀과 일치해 불일치 경고가 나가지 않는 이벤트를 등록한다. */
    private fun saveRegisteredEvent(eventBrawlStarsId: Long) {
        val mode = battleEventModeJpaRepository.save(aBattleEventModeEntity().withBrawlStarsId(MODE_ID).build())
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(eventBrawlStarsId)
                .withMode(mode)
                .withMapBrawlStarsName(MAP_NAME)
                .build(),
        )
    }

    private fun soloRankBattle(
        battleKey: String,
        battleAt: Instant = BATTLE_AT_SEASON_2_1,
        type: String = BattleType.SOLO_RANKED.code,
        eventMap: String = MAP_NAME,
        eventModeBrawlStarsId: Long = MODE_ID,
    ): StatsRankBattleEntity {
        val playerTag = "#TAG1"
        return StatsRankBattleEntity(
            battleKey = battleKey,
            battleAt = battleAt,
            eventBrawlStarsId = EVENT_ID,
            eventModeBrawlStarsId = eventModeBrawlStarsId,
            eventMap = eventMap,
            mode = "gemGrab",
            type = type,
            playerBrawlStarsTag = playerTag,
            teams =
                listOf(
                    listOf(
                        StatsBattleEntityTeamPlayer(
                            brawlStarsTag = playerTag,
                            name = "Player1",
                            brawler =
                                StatsBattleEntityTeamPlayerBrawler(
                                    brawlStarsId = 16_000_000L,
                                    name = "SHELLY",
                                    power = 11,
                                    trophies = 1000,
                                ),
                        ),
                    ),
                ),
            battleRank = 1,
        )
    }

    companion object {
        private const val EVENT_ID = 15_000_001L
        private const val MODE_ID = 1L
        private const val MAP_NAME = "Hard Rock Mine"
        private const val SEASON_2_1 = "2-1"

        /** 경쟁전 시즌 `2-1`(2025-07-17 ~ 2025-08-20) 구간의 시각. */
        private val BATTLE_AT_SEASON_2_1: Instant = OffsetDateTime.parse("2025-08-01T12:00:00+09:00").toInstant()
    }
}
