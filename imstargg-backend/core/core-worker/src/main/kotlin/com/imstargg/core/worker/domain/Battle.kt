package com.imstargg.core.worker.domain

import java.time.Instant

class Battle(
    val battleKey: String,
    val battleAt: Instant,
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
    /**
     * 플레이어 한 명이 브롤러 여러 개를 낸 전투인지(듀얼처럼 BattleBuilder 가 한 명의 브롤러를 같은 태그의 여러 슬롯으로 펼친 전투).
     */
    val brawlersExpanded: Boolean = false,
)

class BattleEvent(
    val brawlStarsId: Long?,
    val modeBrawlStarsName: String?,
    val modeBrawlStarsId: Long?,
    val map: String?,
)

class BattleTeamPlayer(
    val brawlStarsTag: String,
    val name: String,
    val brawler: BattleTeamPlayerBrawler,
)

class BattleTeamPlayerBrawler(
    val brawlStarsId: Long,
    val name: String?,
    val power: Int,
    val trophies: Int,
    val trophyChange: Int?,
)
