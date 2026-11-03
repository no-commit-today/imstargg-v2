package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventEntityFixture.Companion.aBattleEventEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventRotationEntityFixture.Companion.aBattleEventRotationEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventRotationItemEntityFixture.Companion.aBattleEventRotationItemEntity
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class UnregisteredRotationEventAlertTaskTest(
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventRotationJpaRepository: BattleEventRotationJpaRepository,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
    private val statsRankBattleJpaRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleJpaRepository: StatsResultBattleJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    private val alertManager = mockk<AlertManager>(relaxUnitFun = true)

    private val sut =
        UnregisteredRotationEventAlertTask(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventRotationJpaRepository = battleEventRotationJpaRepository,
            battleEventRotationItemJpaRepository = battleEventRotationItemJpaRepository,
            statsRankBattleJpaRepository = statsRankBattleJpaRepository,
            statsResultBattleJpaRepository = statsResultBattleJpaRepository,
        )

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `그날 로테이션에 나온 미등록 이벤트를 알린다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T12:00+09:00"))

        // when
        sut.alert(DATE)

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("미등록 배틀 이벤트")
        assertThat(alert.captured.content).contains(EVENT_ID.toString())
    }

    @Test
    fun `알림 내용에 모드와 맵 이름을 담는다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T12:00+09:00"))

        // when
        sut.alert(DATE)

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.content).contains("modeBrawlStarsId=$MODE_ID", "map=$MAP_NAME")
    }

    @Test
    fun `등록된 이벤트는 알리지 않는다`() {
        // given
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(EVENT_ID)
                .withMode(null)
                .withMapBrawlStarsName(MAP_NAME)
                .build(),
        )
        saveRotationWithEvent(collectedAt = at("2026-11-02T12:00+09:00"))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `같은 날 통계 전투에 나온 이벤트는 전투 단계가 알리므로 알리지 않는다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T12:00+09:00"))
        statsRankBattleJpaRepository.save(rankBattle(battleAt = at("2026-11-02T13:00+09:00")))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `같은 날 결과형 통계 전투에 나온 이벤트도 알리지 않는다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T12:00+09:00"))
        statsResultBattleJpaRepository.save(resultBattle(battleAt = at("2026-11-02T13:00+09:00")))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `다른 날 통계 전투에만 나온 이벤트는 로테이션 기준으로 알린다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T12:00+09:00"))
        statsRankBattleJpaRepository.save(rankBattle(battleAt = at("2026-11-01T13:00+09:00")))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 1) { alertManager.alert(any()) }
    }

    @Test
    fun `하루에 여러 번 수집된 같은 이벤트는 한 번만 알린다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T01:00+09:00"))
        saveRotationWithEvent(collectedAt = at("2026-11-02T02:00+09:00"))
        saveRotationWithEvent(collectedAt = at("2026-11-02T03:00+09:00"))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 1) { alertManager.alert(any()) }
    }

    @Test
    fun `같은 이벤트가 여러 번 나오면 마지막에 저장된 항목의 모드·맵으로 알린다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T01:00+09:00"), mapName = "Old Map")
        saveRotationWithEvent(collectedAt = at("2026-11-02T02:00+09:00"), mapName = "Latest Map")

        // when
        sut.alert(DATE)

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.content).contains("map=Latest Map")
    }

    @Test
    fun `KST 전날 23시 59분에 수집된 로테이션은 보지 않는다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-01T23:59+09:00"))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `KST 당일 0시에 수집된 로테이션은 본다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-02T00:00+09:00"))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 1) { alertManager.alert(any()) }
    }

    @Test
    fun `KST 다음 날 0시에 수집된 로테이션은 보지 않는다`() {
        // given
        saveRotationWithEvent(collectedAt = at("2026-11-03T00:00+09:00"))

        // when
        sut.alert(DATE)

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `그날 수집된 로테이션이 없으면 알리지 않는다`() {
        // when
        sut.alert(DATE)

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    private fun saveRotationWithEvent(
        collectedAt: Instant,
        eventBrawlStarsId: Long = EVENT_ID,
        mapName: String = MAP_NAME,
    ) {
        val rotation =
            battleEventRotationJpaRepository.save(
                aBattleEventRotationEntity().withCollectedAt(collectedAt).build(),
            )
        battleEventRotationItemJpaRepository.save(
            aBattleEventRotationItemEntity()
                .withBattleEventRotation(rotation)
                .withEventBrawlStarsId(eventBrawlStarsId)
                .withEventModeBrawlStarsId(MODE_ID)
                .withEventMapName(mapName)
                .build(),
        )
    }

    private fun rankBattle(battleAt: Instant): StatsRankBattleEntity =
        StatsRankBattleEntity(
            battleKey = "battle-1",
            battleAt = battleAt,
            eventBrawlStarsId = EVENT_ID,
            eventModeBrawlStarsId = MODE_ID,
            eventMap = MAP_NAME,
            mode = "soloShowdown",
            type = BattleType.RANKED.code,
            playerBrawlStarsTag = PLAYER_TAG,
            teams = teams(),
            battleRank = 1,
        )

    private fun resultBattle(battleAt: Instant): StatsResultBattleEntity =
        StatsResultBattleEntity(
            battleKey = "battle-1",
            battleAt = battleAt,
            eventBrawlStarsId = EVENT_ID,
            eventModeBrawlStarsId = MODE_ID,
            eventMap = MAP_NAME,
            mode = "hockey",
            type = BattleType.RANKED.code,
            playerBrawlStarsTag = PLAYER_TAG,
            teams = teams(),
            starPlayerBrawlStarsTag = null,
            result = BattleResult.VICTORY.code,
        )

    private fun teams(): List<List<StatsBattleEntityTeamPlayer>> =
        listOf(
            listOf(
                StatsBattleEntityTeamPlayer(
                    brawlStarsTag = PLAYER_TAG,
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
        )

    private fun at(offsetDateTime: String): Instant = OffsetDateTime.parse(offsetDateTime).toInstant()

    companion object {
        private val DATE = LocalDate.of(2026, 11, 2)
        private const val EVENT_ID = 15_009_999L
        private const val MODE_ID = 25L
        private const val MAP_NAME = "Hockey Rink"
        private const val PLAYER_TAG = "#TAG1"
    }
}
