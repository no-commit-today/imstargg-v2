package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntityBrawler

class BattleTeamPlayerEntityFixture {
    private var battle: BattleEntity = BattleEntityFixture.aBattleEntity().build()
    private var teamIdx: Int = 0
    private var playerIdx: Int = 0
    private var brawlStarsTag: String = "#PLAYER1"
    private var name: String = "Player1"
    private var brawler: BattleTeamPlayerEntityBrawler = defaultBrawler()

    companion object {
        fun aBattleTeamPlayerEntity() = BattleTeamPlayerEntityFixture()

        fun defaultBrawler(
            brawlStarsId: Long = 16000000L,
            name: String = "SHELLY",
            power: Int = 11,
            trophies: Int = 750,
            trophyChange: Int? = 8,
        ) = BattleTeamPlayerEntityBrawler(
            brawlStarsId = brawlStarsId,
            name = name,
            power = power,
            trophies = trophies,
            trophyChange = trophyChange,
        )

        fun defaultTeamPlayersFor(battle: BattleEntity): List<BattleTeamPlayerEntity> =
            listOf(
                BattleTeamPlayerEntity(
                    battle = battle,
                    teamIdx = 0,
                    playerIdx = 0,
                    brawlStarsTag = "#PLAYER1",
                    name = "Player1",
                    brawler = defaultBrawler(brawlStarsId = 16000000L, name = "SHELLY"),
                ),
                BattleTeamPlayerEntity(
                    battle = battle,
                    teamIdx = 0,
                    playerIdx = 1,
                    brawlStarsTag = "#PLAYER2",
                    name = "Player2",
                    brawler = defaultBrawler(brawlStarsId = 16000001L, name = "COLT", power = 10, trophies = 700),
                ),
                BattleTeamPlayerEntity(
                    battle = battle,
                    teamIdx = 0,
                    playerIdx = 2,
                    brawlStarsTag = "#PLAYER3",
                    name = "Player3",
                    brawler = defaultBrawler(brawlStarsId = 16000002L, name = "BULL", power = 9, trophies = 680),
                ),
                BattleTeamPlayerEntity(
                    battle = battle,
                    teamIdx = 1,
                    playerIdx = 0,
                    brawlStarsTag = "#PLAYER4",
                    name = "Player4",
                    brawler =
                        defaultBrawler(
                            brawlStarsId = 16000003L,
                            name = "JESSIE",
                            trophies = 720,
                            trophyChange = -8,
                        ),
                ),
                BattleTeamPlayerEntity(
                    battle = battle,
                    teamIdx = 1,
                    playerIdx = 1,
                    brawlStarsTag = "#PLAYER5",
                    name = "Player5",
                    brawler =
                        defaultBrawler(
                            brawlStarsId = 16000004L,
                            name = "BROCK",
                            power = 10,
                            trophies = 710,
                            trophyChange = -8,
                        ),
                ),
                BattleTeamPlayerEntity(
                    battle = battle,
                    teamIdx = 1,
                    playerIdx = 2,
                    brawlStarsTag = "#PLAYER6",
                    name = "Player6",
                    brawler =
                        defaultBrawler(
                            brawlStarsId = 16000005L,
                            name = "DYNAMIKE",
                            power = 9,
                            trophies = 690,
                            trophyChange = -8,
                        ),
                ),
            )
    }

    fun withBattle(value: BattleEntity) = apply { battle = value }

    fun withTeamIdx(value: Int) = apply { teamIdx = value }

    fun withPlayerIdx(value: Int) = apply { playerIdx = value }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withName(value: String) = apply { name = value }

    fun withBrawler(value: BattleTeamPlayerEntityBrawler) = apply { brawler = value }

    fun build() =
        BattleTeamPlayerEntity(
            battle = battle,
            teamIdx = teamIdx,
            playerIdx = playerIdx,
            brawlStarsTag = brawlStarsTag,
            name = name,
            brawler = brawler,
        )
}
