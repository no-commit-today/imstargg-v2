package com.imstargg.core.batch.job.statistics

import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

open class StatsPlayerSyncJobItemWriter(
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
) : ItemWriter<List<StatsBattleEntityTeamPlayer>> {
    override fun write(chunk: Chunk<out List<StatsBattleEntityTeamPlayer>>) {
        val allTags =
            chunk.items
                .flatten()
                .map { it.brawlStarsTag }
                .distinct()

        if (allTags.isEmpty()) return

        val existingTags =
            statsPlayerJpaRepository
                .findAllByBrawlStarsTagIn(allTags)
                .map { it.brawlStarsTag }
                .toSet()

        val newPlayers =
            allTags
                .filter { it !in existingTags }
                .map { StatsPlayerEntity(brawlStarsTag = it) }

        if (newPlayers.isNotEmpty()) {
            statsPlayerJpaRepository.saveAll(newPlayers)
        }
    }
}
