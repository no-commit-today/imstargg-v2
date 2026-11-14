package com.imstargg.core.batch.job.brawlstars

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventEntityFixture.Companion.aBattleEventEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventModeEntityFixture.Companion.aBattleEventModeEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import java.time.OffsetDateTime

/**
 * 운영 CronJob 은 `--job.name=battleEventSyncJob date=yyyy-MM-dd` 로 띄우고,
 * Spring Boot 의 `JobLauncherApplicationRunner` 는 그 인자를 [JobOperator.start] 에 그대로 넘긴다.
 * 이 테스트는 같은 경로로 잡을 실행해, 실행 인자 `date` 가 잡까지 전달되는지 고정한다.
 */
class BattleEventSyncJobTest(
    private val jobOperator: JobOperator,
    @param:Qualifier(BattleEventSyncJobConfig.JOB_NAME) private val battleEventSyncJob: Job,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
    private val statsResultBattleJpaRepository: StatsResultBattleJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `실행 인자로 받은 날짜의 경쟁전 배틀 이벤트를 시즌 맵으로 저장한다`() {
        // given
        val mode = battleEventModeJpaRepository.save(aBattleEventModeEntity().withBrawlStarsId(MODE_ID).build())
        battleEventJpaRepository.save(
            aBattleEventEntity()
                .withBrawlStarsId(EVENT_ID)
                .withMode(mode)
                .withMapBrawlStarsName(MAP_NAME)
                .build(),
        )
        statsResultBattleJpaRepository.save(soloRankResultBattle())
        val jobParameters =
            JobParametersBuilder()
                .addString("date", BATTLE_DATE)
                .toJobParameters()

        // when
        val execution = jobOperator.start(battleEventSyncJob, jobParameters)

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        assertThat(battleEventSoloRankEventJpaRepository.findAllBySoloRankSeasonKey(SEASON_2_1))
            .extracting<Long> { it.eventBrawlStarsId }
            .containsExactly(EVENT_ID)
    }

    private fun soloRankResultBattle(): StatsResultBattleEntity {
        val playerTag = "#TAG1"
        return StatsResultBattleEntity(
            battleKey = "battle-1",
            battleAt = OffsetDateTime.parse("${BATTLE_DATE}T12:00:00+09:00").toInstant(),
            eventBrawlStarsId = EVENT_ID,
            eventModeBrawlStarsId = MODE_ID,
            eventMap = MAP_NAME,
            mode = "gemGrab",
            type = BattleType.SOLO_RANKED.code,
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
            starPlayerBrawlStarsTag = null,
            result = "victory",
        )
    }

    companion object {
        private const val EVENT_ID = 15_000_001L
        private const val MODE_ID = 1L
        private const val MAP_NAME = "Hard Rock Mine"

        /** 경쟁전 시즌 `2-1`(2025-07-17 ~ 2025-08-20) 구간의 날짜(KST). */
        private const val BATTLE_DATE = "2025-08-01"
        private const val SEASON_2_1 = "2-1"
    }
}
