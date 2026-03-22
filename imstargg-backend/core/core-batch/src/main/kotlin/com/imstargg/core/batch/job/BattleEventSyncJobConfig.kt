package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.batch.support.querydsl.QuerydslPagingItemReader
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.SoloRankSeason
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.OffsetDateTime

@Configuration
class BattleEventSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) {
    companion object {
        const val JOB_NAME = "battleEventSyncJob"
        const val DATE_PARAMETER_NAME = "battleEventSyncJobDateJobParameter"
        const val STEP_1_NAME = "battleEventSyncFromRankBattleStep"
        const val STEP_2_NAME = "battleEventSyncFromResultBattleStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun battleEventSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(battleEventSyncFromRankBattleStep())
            .next(battleEventSyncFromResultBattleStep())
            .build()

    @Bean(STEP_1_NAME)
    @JobScope
    fun battleEventSyncFromRankBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsRankBattleEntity, StatsRankBattleEntity>(STEP_1_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(battleEventSyncRankBattleReader())
            .writer(battleEventSyncFromRankBattleWriter())
            .build()

    @Bean(STEP_2_NAME)
    @JobScope
    fun battleEventSyncFromResultBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsResultBattleEntity, StatsResultBattleEntity>(STEP_2_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(battleEventSyncResultBattleReader())
            .writer(battleEventSyncFromResultBattleWriter())
            .build()

    @Bean
    @StepScope
    fun battleEventSyncRankBattleReader(): QuerydslPagingItemReader<StatsRankBattleEntity> {
        val rankBattle = QStatsRankBattleEntity.statsRankBattleEntity
        return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
            queryFactory
                .selectFrom(rankBattle)
                .where(rankBattle.createdDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncResultBattleReader(): QuerydslPagingItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.createdDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncFromRankBattleWriter() =
        BattleEventSyncItemWriter<StatsRankBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )

    @Bean
    @StepScope
    fun battleEventSyncFromResultBattleWriter() =
        BattleEventSyncItemWriter<StatsResultBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )

    class SoloRankSeasonCache {
        private val seasons: MutableList<SoloRankSeason> = mutableListOf()

        fun get(battleAt: OffsetDateTime): SoloRankSeason =
            seasons.find { !battleAt.isBefore(it.startAt) && !battleAt.isAfter(it.endAt) }
                ?: SoloRankSeason.of(battleAt).also { seasons.add(it) }
    }

    open class BattleEventSyncItemWriter<T : BaseStatsBattleEntity>(
        private val alertManager: AlertManager,
        private val battleEventJpaRepository: BattleEventJpaRepository,
        private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
    ) : ItemWriter<T> {
        private val checkedEventIds: MutableSet<Long> = mutableSetOf()
        private val soloRankSeasonCache = SoloRankSeasonCache()
        private val existingSoloRankEventIds: MutableMap<String, MutableSet<Long>> = mutableMapOf()

        override fun write(chunk: Chunk<out T>) {
            detectEventChanges(chunk)
            createSoloRankEvents(chunk)
        }

        private fun detectEventChanges(chunk: Chunk<out BaseStatsBattleEntity>) {
            val uncheckedIds =
                chunk.items
                    .map { it.eventBrawlStarsId }
                    .distinct()
                    .filter { it !in checkedEventIds }

            if (uncheckedIds.isEmpty()) return

            val events =
                battleEventJpaRepository
                    .findAllByBrawlStarsIdIn(uncheckedIds)
                    .associateBy { it.brawlStarsId }

            for (battle in chunk.items) {
                if (battle.eventBrawlStarsId in checkedEventIds) continue

                val event = events[battle.eventBrawlStarsId]
                if (event == null) {
                    alertUnregisteredEvent(battle)
                } else if (!matches(battle, event)) {
                    alertMismatch(battle, event)
                }
            }

            checkedEventIds.addAll(uncheckedIds)
        }

        private fun matches(
            battle: BaseStatsBattleEntity,
            event: BattleEventEntity,
        ): Boolean {
            val mode = event.mode
            if (mode != null && mode.brawlStarsId != battle.eventModeBrawlStarsId) return false
            if (event.mapName != null && event.mapName != battle.eventMap) return false
            return true
        }

        private fun alertUnregisteredEvent(battle: BaseStatsBattleEntity) {
            alertManager.alert(
                AlertCommand
                    .builder()
                    .warn()
                    .title("[$JOB_NAME] 미등록 배틀 이벤트 감지")
                    .content(
                        """
                        eventBrawlStarsId=${battle.eventBrawlStarsId},
                        modeBrawlStarsId=${battle.eventModeBrawlStarsId},
                        map=${battle.eventMap}
                        """.trimIndent(),
                    ).build(),
            )
        }

        private fun alertMismatch(
            battle: BaseStatsBattleEntity,
            event: BattleEventEntity,
        ) {
            alertManager.alert(
                AlertCommand
                    .builder()
                    .warn()
                    .title("[$JOB_NAME] 배틀 이벤트 불일치")
                    .content(
                        """
                        등록된 데이터
                        - eventBrawlStarsId=${event.brawlStarsId}
                        - modeBrawlStarsId=${event.mode?.brawlStarsId}
                        - map=${event.mapName}

                        실제 데이터
                        - eventBrawlStarsId=${battle.eventBrawlStarsId}
                        - modeBrawlStarsId=${battle.eventModeBrawlStarsId}
                        - map=${battle.eventMap}
                        """.trimIndent(),
                    ).build(),
            )
        }

        private fun createSoloRankEvents(chunk: Chunk<out BaseStatsBattleEntity>) {
            val soloRankBattles = chunk.items.filter { it.type == BattleType.SOLO_RANKED.code }

            for ((seasonKey, battles) in soloRankBattles.groupBy { soloRankSeasonCache.get(it.battleAt).key }) {
                val existingIds =
                    existingSoloRankEventIds.getOrPut(seasonKey) {
                        battleEventSoloRankEventJpaRepository
                            .findBySoloRankSeasonKey(seasonKey)
                            .map { it.eventBrawlStarsId }
                            .toMutableSet()
                    }

                val newEventIds =
                    battles
                        .map { it.eventBrawlStarsId }
                        .distinct()
                        .filter { it !in existingIds }

                if (newEventIds.isEmpty()) continue

                val entities =
                    newEventIds.map { eventBrawlStarsId ->
                        BattleEventSoloRankEventEntity(
                            soloRankSeasonKey = seasonKey,
                            eventBrawlStarsId = eventBrawlStarsId,
                        )
                    }

                battleEventSoloRankEventJpaRepository.saveAll(entities)
                existingIds.addAll(newEventIds)
            }
        }
    }
}
