package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import java.time.OffsetDateTime

class BattleEntityFixture {
    private var battleKey: String = "battle-key-12345"
    private var battleAt: OffsetDateTime = OffsetDateTime.now()
    private var event: BattleEntityEvent = defaultBattleEntityEvent()
    private var mode: String? = "gemGrab"
    private var type: String? = "ranked"
    private var result: String? = "victory"
    private var duration: Int? = 120
    private var starPlayerBrawlStarsTag: String? = "#TEST12345"
    private var player: PlayerEntity = aPlayerEntity().build()
    private var battleRank: Int? = 1
    private var trophyChange: Int? = 8
    private var teams: List<List<BattleEntityTeamPlayer>> = defaultTeams()

    companion object {
        fun aBattleEntity() = BattleEntityFixture()

        fun defaultBattleEntityEvent() =
            BattleEntityEvent(
                brawlStarsId = 15000001L,
                mode = "gemGrab",
                map = "Hard Rock Mine",
            )

        fun defaultBattleEntityTeamPlayerBrawler(
            brawlStarsId: Long = 16000000L,
            name: String = "SHELLY",
            power: Int = 11,
            trophies: Int = 750,
            trophyChange: Int? = 8,
        ) = BattleEntityTeamPlayerBrawler(
            brawlStarsId = brawlStarsId,
            name = name,
            power = power,
            trophies = trophies,
            trophyChange = trophyChange,
        )

        fun defaultBattleEntityTeamPlayer(
            brawlStarsTag: String = "#PLAYER1",
            name: String = "Player1",
            brawler: BattleEntityTeamPlayerBrawler = defaultBattleEntityTeamPlayerBrawler(),
        ) = BattleEntityTeamPlayer(
            brawlStarsTag = brawlStarsTag,
            name = name,
            brawler = brawler,
        )

        fun defaultTeams() =
            listOf(
                listOf(
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER1",
                        name = "Player1",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 16000000L,
                                name = "SHELLY",
                                power = 11,
                                trophies = 750,
                                trophyChange = 8,
                            ),
                    ),
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER2",
                        name = "Player2",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 16000001L,
                                name = "COLT",
                                power = 10,
                                trophies = 700,
                                trophyChange = 8,
                            ),
                    ),
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER3",
                        name = "Player3",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 16000002L,
                                name = "BULL",
                                power = 9,
                                trophies = 680,
                                trophyChange = 8,
                            ),
                    ),
                ),
                listOf(
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER4",
                        name = "Player4",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 16000003L,
                                name = "JESSIE",
                                power = 11,
                                trophies = 720,
                                trophyChange = -8,
                            ),
                    ),
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER5",
                        name = "Player5",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 16000004L,
                                name = "BROCK",
                                power = 10,
                                trophies = 710,
                                trophyChange = -8,
                            ),
                    ),
                    BattleEntityTeamPlayer(
                        brawlStarsTag = "#PLAYER6",
                        name = "Player6",
                        brawler =
                            BattleEntityTeamPlayerBrawler(
                                brawlStarsId = 16000005L,
                                name = "DYNAMIKE",
                                power = 9,
                                trophies = 690,
                                trophyChange = -8,
                            ),
                    ),
                ),
            )
    }

    fun withBattleKey(value: String) = apply { battleKey = value }

    fun withBattleAt(value: OffsetDateTime) = apply { battleAt = value }

    fun withEvent(value: BattleEntityEvent) = apply { event = value }

    fun withMode(value: String?) = apply { mode = value }

    fun withType(value: String?) = apply { type = value }

    fun withResult(value: String?) = apply { result = value }

    fun withDuration(value: Int?) = apply { duration = value }

    fun withStarPlayerBrawlStarsTag(value: String?) = apply { starPlayerBrawlStarsTag = value }

    fun withPlayer(value: PlayerEntity) = apply { player = value }

    fun withBattleRank(value: Int?) = apply { battleRank = value }

    fun withTrophyChange(value: Int?) = apply { trophyChange = value }

    fun withTeams(value: List<List<BattleEntityTeamPlayer>>) = apply { teams = value }

    fun build() =
        BattleEntity(
            battleKey = battleKey,
            battleAt = battleAt,
            event = event,
            mode = mode,
            type = type,
            result = result,
            duration = duration,
            starPlayerBrawlStarsTag = starPlayerBrawlStarsTag,
            player = player,
            battleRank = battleRank,
            trophyChange = trophyChange,
            teams = teams,
        )
}
