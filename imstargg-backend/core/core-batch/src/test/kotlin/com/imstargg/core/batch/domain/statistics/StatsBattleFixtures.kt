package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsEventMode
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import java.time.OffsetDateTime

private const val BATTLE_KEY = "battle-key"
private const val EVENT_BRAWL_STARS_ID = 1L
private const val EVENT_MAP = "test-map"
private val BATTLE_AT = OffsetDateTime.parse("2026-11-01T12:00:00+09:00").toInstant()

/** 태그 "#TAG$index", 브롤러 id = index, 브롤러 이름 "Brawler$index", 파워 11. */
internal fun statsPlayer(
    index: Int,
    trophies: Int = 1000,
    name: String = "Player$index",
    tag: String = "#TAG$index",
): StatsBattleEntityTeamPlayer =
    StatsBattleEntityTeamPlayer(
        brawlStarsTag = tag,
        name = name,
        brawler =
            StatsBattleEntityTeamPlayerBrawler(
                brawlStarsId = index.toLong(),
                name = "Brawler$index",
                power = 11,
                trophies = trophies,
            ),
    )

/** battleKey "battle-key", battleAt 2026-11-01T12:00+09:00, eventBrawlStarsId 1, eventMap "test-map", mode "gemGrab". */
internal fun statsResultBattle(
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    result: String = BattleResult.VICTORY.code,
    type: String = BattleType.RANKED.code,
    eventModeBrawlStarsId: Long = StatsEventMode.GEM_GRAB.modeBrawlStarsId,
    playerBrawlStarsTag: String = teams.first().first().brawlStarsTag,
    starPlayerBrawlStarsTag: String? = null,
): StatsResultBattleEntity =
    StatsResultBattleEntity(
        battleKey = BATTLE_KEY,
        battleAt = BATTLE_AT,
        eventBrawlStarsId = EVENT_BRAWL_STARS_ID,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
        eventMap = EVENT_MAP,
        mode = "gemGrab",
        type = type,
        playerBrawlStarsTag = playerBrawlStarsTag,
        teams = teams,
        starPlayerBrawlStarsTag = starPlayerBrawlStarsTag,
        result = result,
    )

/** 위와 같은 고정값, mode "soloShowdown". */
internal fun statsRankBattle(
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    battleRank: Int = 1,
    type: String = BattleType.RANKED.code,
    eventModeBrawlStarsId: Long = StatsEventMode.SOLO_SHOWDOWN.modeBrawlStarsId,
    playerBrawlStarsTag: String = teams.first().first().brawlStarsTag,
): StatsRankBattleEntity =
    StatsRankBattleEntity(
        battleKey = BATTLE_KEY,
        battleAt = BATTLE_AT,
        eventBrawlStarsId = EVENT_BRAWL_STARS_ID,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
        eventMap = EVENT_MAP,
        mode = "soloShowdown",
        type = type,
        playerBrawlStarsTag = playerBrawlStarsTag,
        teams = teams,
        battleRank = battleRank,
    )

/** 2×3 결과형. 수집자 팀은 인덱스 1 ~ 3(수집자는 #TAG1), 상대는 인덱스 4 ~ 6. 리스트는 각 팀원의 트로피다. */
internal fun threeVsThree(
    collectorTeam: List<Int>,
    opponents: List<Int>,
    result: String = BattleResult.VICTORY.code,
    type: String = BattleType.RANKED.code,
    eventModeBrawlStarsId: Long = StatsEventMode.GEM_GRAB.modeBrawlStarsId,
    opponentNames: List<String> = listOf("Player4", "Player5", "Player6"),
): StatsResultBattleEntity =
    statsResultBattle(
        teams =
            listOf(
                collectorTeam.mapIndexed { i, trophies -> statsPlayer(1 + i, trophies) },
                opponents.mapIndexed { i, trophies -> statsPlayer(4 + i, trophies, name = opponentNames[i]) },
            ),
        result = result,
        type = type,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
    )

/** 쇼다운. teamTrophies 의 첫 팀이 수집자 팀이고 그 첫 팀원이 수집자(#TAG1)다. 슬롯 인덱스는 1 부터 팀 순서대로. 모드 id 는 mode 의 것. */
internal fun showdown(
    mode: StatsEventMode,
    teamTrophies: List<List<Int>>,
    battleRank: Int = 1,
    type: String = BattleType.RANKED.code,
): StatsRankBattleEntity {
    var index = 0
    val teams = teamTrophies.map { team -> team.map { trophies -> statsPlayer(++index, trophies) } }
    return statsRankBattle(
        teams = teams,
        battleRank = battleRank,
        type = type,
        eventModeBrawlStarsId = mode.modeBrawlStarsId,
    )
}
