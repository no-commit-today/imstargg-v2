package com.imstargg.core.worker.domain

import java.time.OffsetDateTime

class Battle(
    val battleKey: String,
    val battleAt: OffsetDateTime,
    val event: BattleEvent?,
    val mode: String?,
    val type: String?,
    val result: String?,
    val duration: Int?,
    val starPlayerBrawlStarsTag: String?,
    val playerBrawlStarsTag: String,
    val battleRank: Int?,
    val trophyChange: Int?,
    val teams: List<List<BattleTeamPlayer>>,
)

class BattleEvent(
    val brawlStarsId: Long?,
    val modeBrawlStarsName: String?,
    val modeBrawlStarsId: Int?,
    val map: String?,
)

class BattleTeamPlayer(
    val brawlStarsTag: String,
    val name: String,
    val brawler: BattleTeamPlayerBrawler,
)

class BattleTeamPlayerBrawler(
    val brawlStarsId: Long,
    val name: String,
    val power: Int,
    val trophies: Int,
    val trophyChange: Int?,
)
