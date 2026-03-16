package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.battle.Battle
import com.imstargg.core.domain.brawlstars.battle.BattlePlayer
import com.imstargg.core.domain.brawlstars.battle.BattlePlayerBrawler
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.enums.BattleMode
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.SoloRankTier
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntityBrawler
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.toTeams
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class BattleRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerRepository: BattleTeamPlayerJpaRepository,
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun find(
        player: Player,
        offsetLimit: OffsetLimit,
    ): Slice<Battle> {
        val playerEntity = playerJpaRepository.findByBrawlStarsTag(player.tag.value)!!

        val battleSlice =
            battleJpaRepository.findSliceByPlayerIdOrderByBattleAtDesc(
                playerEntity.id!!,
                offsetLimit.toPageable(),
            )

        val battleIds = battleSlice.content.mapNotNull { it.id }
        val teamPlayersByBattleId =
            battleTeamPlayerRepository
                .findAllByBattleIdIn(battleIds)
                .groupBy { it.battle.id!! }

        val brawlers = brawlerRepositoryWithCache.findBrawlers()

        return Slice(
            content =
                battleSlice.content.map { battle ->
                    toBattle(battle, teamPlayersByBattleId[battle.id] ?: emptyList(), brawlers)
                },
            hasNext = battleSlice.hasNext(),
        )
    }

    private fun toBattle(
        entity: BattleEntity,
        teamPlayers: List<BattleTeamPlayerEntity>,
        brawlers: Brawlers,
    ): Battle {
        val battleType = entity.type?.let { EnumCodeType.findByCode<BattleType>(it) }
        return Battle(
            battleTime = entity.battleAt,
            event = findEvent(entity.event?.brawlStarsId),
            mode = entity.mode?.let { EnumCodeType.findByCode<BattleMode>(it) },
            type = battleType,
            result = entity.result?.let { EnumCodeType.findByCode<BattleResult>(it) },
            duration = entity.duration,
            starPlayerTag = entity.starPlayerBrawlStarsTag?.let { BrawlStarsTag(it) },
            rank = entity.battleRank,
            trophyChange = entity.trophyChange,
            teams = toTeams(battleType, teamPlayers, brawlers),
        )
    }

    private fun findEvent(brawlStarsId: Long?): BattleEvent? {
        if (brawlStarsId == null || brawlStarsId <= 0) return null
        return battleEventRepositoryWithCache.find(BrawlStarsId(brawlStarsId))
    }

    private fun toTeams(
        battleType: BattleType?,
        teamPlayers: List<BattleTeamPlayerEntity>,
        brawlers: Brawlers,
    ): List<List<BattlePlayer>> =
        teamPlayers.toTeams().map { team ->
            team.map { player -> toBattlePlayer(battleType, player, brawlers) }
        }

    private fun toBattlePlayer(
        battleType: BattleType?,
        player: BattleTeamPlayerEntity,
        brawlers: Brawlers,
    ): BattlePlayer =
        BattlePlayer(
            tag = BrawlStarsTag(player.brawlStarsTag),
            name = player.name,
            soloRankTier =
                if (battleType == BattleType.SOLO_RANKED) {
                    SoloRankTier.of(player.brawler.trophies)
                } else {
                    null
                },
            brawler = toBattlePlayerBrawler(battleType, player.brawler, brawlers),
        )

    private fun toBattlePlayerBrawler(
        battleType: BattleType?,
        brawler: BattleTeamPlayerEntityBrawler,
        brawlers: Brawlers,
    ): BattlePlayerBrawler {
        val isSoloRanked = battleType == BattleType.SOLO_RANKED
        return BattlePlayerBrawler(
            brawler = brawlers.find(BrawlStarsId(brawler.brawlStarsId)),
            power = brawler.power,
            trophies = if (isSoloRanked) null else brawler.trophies,
            trophyChange = if (isSoloRanked) null else brawler.trophyChange,
        )
    }
}
