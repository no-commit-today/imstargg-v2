package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.battle.Battle
import com.imstargg.core.domain.brawlstars.battle.BattlePlayer
import com.imstargg.core.domain.brawlstars.battle.BattlePlayerBrawler
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.brawlstars.event.Event
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.enums.BattleMode
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.SoloRankTier
import com.imstargg.core.error.CoreException
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerJpaRepository
import org.springframework.stereotype.Repository

@Repository
class BattleRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val eventRepositoryWithCache: EventRepositoryWithCache,
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun find(
        player: Player,
        offsetLimit: OffsetLimit,
    ): List<Battle> {
        val playerEntity = playerJpaRepository.findByBrawlStarsTag(player.tag.value)!!

        val battleSlice =
            battleJpaRepository.findSliceByPlayerPlayerIdAndDeletedFalseOrderByBattleAtDesc(
                playerEntity.id!!,
                offsetLimit.toPageable(),
            )

        val brawlers = brawlerRepositoryWithCache.findBrawlers()

        return battleSlice.content.map { toBattle(it, brawlers) }
    }

    private fun toBattle(
        entity: BattleEntity,
        brawlers: Brawlers,
    ): Battle {
        val battleType = entity.type?.let { EnumCodeType.findByCode<BattleType>(it) }
        return Battle(
            battleTime = entity.battleAt,
            event = findEvent(entity.event.brawlStarsId),
            mode = entity.mode?.let { EnumCodeType.findByCode<BattleMode>(it) },
            type = battleType,
            result = entity.result?.let { EnumCodeType.findByCode<BattleResult>(it) },
            duration = entity.duration,
            starPlayerTag = entity.starPlayerBrawlStarsTag?.let { BrawlStarsTag(it) },
            rank = entity.player.rank,
            trophyChange = entity.player.trophyChange,
            teams = toTeams(battleType, entity.teams, brawlers),
        )
    }

    private fun findEvent(brawlStarsId: Long?): Event? {
        if (brawlStarsId == null || brawlStarsId <= 0) return null
        return eventRepositoryWithCache.find(BrawlStarsId(brawlStarsId))
    }

    private fun toTeams(
        battleType: BattleType?,
        teams: List<List<BattleEntityTeamPlayer>>,
        brawlers: Brawlers,
    ): List<List<BattlePlayer>> =
        teams.map { team ->
            team.map { player -> toBattlePlayer(battleType, player, brawlers) }
        }

    private fun toBattlePlayer(
        battleType: BattleType?,
        player: BattleEntityTeamPlayer,
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
        brawler: BattleEntityTeamPlayerBrawler,
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
