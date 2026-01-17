package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BattleResultBrawlerResponse
import com.imstargg.client.brawlstars.response.BattleResultPlayerResponse
import com.imstargg.client.brawlstars.response.BattleResultResponse
import com.imstargg.client.brawlstars.response.EventResponse
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BattleEntityBuilderTest {
    private data class BattleEvent(
        val brawlStarsId: Long,
        val mode: String,
        val modeId: Int,
        val map: String,
    ) {
        companion object {
            val KNOCKOUT = BattleEvent(10000001L, "knockout", 1, "Flaring Phoenix")
            val SOLO_SHOWDOWN = BattleEvent(10000002L, "soloShowdown", 2, "Flying Fantasies")
            val DUO_SHOWDOWN = BattleEvent(10000003L, "duoShowdown", 3, "Flying Fantasies")
            val KNOCKOUT_5V5 = BattleEvent(10000004L, "knockout5V5", 4, "Sizzling Chambers")
            val DUEL = BattleEvent(10000005L, "duel", 5, "Duel Arena")
        }
    }

    private data class Brawler(
        val brawlStarsId: Long,
        val name: String,
    ) {
        companion object {
            val JUJU = Brawler(10000001L, "Juju")
            val TICK = Brawler(10000002L, "Tick")
            val BUSTER = Brawler(10000003L, "Buster")
            val SPROUT = Brawler(10000004L, "Sprout")
            val MOE = Brawler(10000005L, "Moe")
            val SQUEAK = Brawler(10000006L, "Squeak")
            val GALE = Brawler(10000007L, "Gale")
            val GENE = Brawler(10000008L, "Gene")
            val BULL = Brawler(10000009L, "Bull")
            val BYRON = Brawler(10000010L, "Byron")
            val SHADE = Brawler(10000011L, "Shade")
            val COLT = Brawler(10000012L, "Colt")
            val ANGELO = Brawler(10000013L, "Angelo")
            val PAM = Brawler(10000014L, "Pam")
            val LILY = Brawler(10000015L, "Lily")
            val EDGAR = Brawler(10000016L, "Edgar")
            val GUS = Brawler(10000017L, "Gus")
            val LOLA = Brawler(10000018L, "Lola")
            val PENNY = Brawler(10000019L, "Penny")
            val DOUG = Brawler(10000020L, "Doug")
            val POCO = Brawler(10000021L, "Poco")
            val WILLOW = Brawler(10000022L, "Willow")
            val GROM = Brawler(10000023L, "Grom")
            val EIGHT_BIT = Brawler(10000024L, "8-BIT")
            val ROSA = Brawler(10000025L, "Rosa")
        }
    }

    private val battleTime: OffsetDateTime =
        ZonedDateTime
            .of(
                LocalDateTime.of(2024, 11, 29, 11, 0, 0),
                ZoneId.systemDefault(),
            ).toOffsetDateTime()

    private fun createMockPlayerEntity(id: Long = 123L): PlayerEntity {
        val playerEntity = mockk<PlayerEntity>()
        every { playerEntity.id } returns id
        return playerEntity
    }

    private fun createBrawlerResponse(
        brawler: Brawler,
        power: Int = 11,
        trophies: Int = 500,
        trophyChange: Int? = null,
    ) = BattleResultBrawlerResponse(
        id = brawler.brawlStarsId,
        name = brawler.name,
        power = power,
        trophies = trophies,
        trophyChange = trophyChange,
    )

    private fun createPlayerResponse(
        tag: String,
        name: String,
        brawler: BattleResultBrawlerResponse?,
        brawlers: List<BattleResultBrawlerResponse>? = null,
    ) = BattleResultPlayerResponse(
        tag = tag,
        name = name,
        brawler = brawler,
        brawlers = brawlers,
    )

    private fun createEventResponse(event: BattleEvent) =
        EventResponse(
            id = event.brawlStarsId,
            mode = event.mode,
            modeId = event.modeId,
            map = event.map,
        )

    @Test
    fun `팀_전투에서_BattleEntity를_생성한다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.KNOCKOUT),
                battle =
                    BattleResultResponse(
                        mode = "knockout",
                        type = "soloRanked",
                        result = "victory",
                        duration = 117,
                        rank = null,
                        trophyChange = null,
                        starPlayer = null,
                        teams =
                            listOf(
                                listOf(
                                    createPlayerResponse(
                                        "#player1",
                                        "player1",
                                        createBrawlerResponse(Brawler.JUJU),
                                    ),
                                    createPlayerResponse(
                                        "#player2",
                                        "player2",
                                        createBrawlerResponse(Brawler.TICK),
                                    ),
                                    createPlayerResponse(
                                        "#player3",
                                        "player3",
                                        createBrawlerResponse(Brawler.BUSTER),
                                    ),
                                ),
                                listOf(
                                    createPlayerResponse(
                                        "#player4",
                                        "player4",
                                        createBrawlerResponse(Brawler.SPROUT),
                                    ),
                                    createPlayerResponse(
                                        "#player5",
                                        "player5",
                                        createBrawlerResponse(Brawler.MOE),
                                    ),
                                    createPlayerResponse(
                                        "#player6",
                                        "player6",
                                        createBrawlerResponse(Brawler.SQUEAK),
                                    ),
                                ),
                            ),
                        players = null,
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val result = sut.build()

        // then
        assertBattle(result, battleResponse, playerEntity)
        assertBattleTeams(result.teams, battleResponse.battle.teams!!)
    }

    @Test
    fun `듀오_쇼다운에서_BattleEntity를_생성한다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.DUO_SHOWDOWN),
                battle =
                    BattleResultResponse(
                        mode = "duoShowdown",
                        type = "ranked",
                        result = "victory",
                        duration = 117,
                        rank = 2,
                        trophyChange = 8,
                        starPlayer = null,
                        teams =
                            listOf(
                                listOf(
                                    createPlayerResponse(
                                        "#player1",
                                        "player1",
                                        createBrawlerResponse(Brawler.GALE, trophies = 476),
                                    ),
                                    createPlayerResponse(
                                        "#player2",
                                        "player2",
                                        createBrawlerResponse(Brawler.GENE, trophies = 619),
                                    ),
                                ),
                                listOf(
                                    createPlayerResponse(
                                        "#player3",
                                        "player3",
                                        createBrawlerResponse(Brawler.BULL, trophies = 606),
                                    ),
                                    createPlayerResponse(
                                        "#player4",
                                        "player4",
                                        createBrawlerResponse(Brawler.BYRON, trophies = 601),
                                    ),
                                ),
                                listOf(
                                    createPlayerResponse(
                                        "#player5",
                                        "player5",
                                        createBrawlerResponse(Brawler.SHADE, trophies = 640),
                                    ),
                                    createPlayerResponse(
                                        "#player6",
                                        "player6",
                                        createBrawlerResponse(Brawler.COLT, trophies = 602),
                                    ),
                                ),
                            ),
                        players = null,
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val result = sut.build()

        // then
        assertBattle(result, battleResponse, playerEntity)
        assertBattleTeams(result.teams, battleResponse.battle.teams!!)
    }

    @Test
    fun `녹아웃_5대5에서_BattleEntity를_생성한다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val starPlayerResponse =
            createPlayerResponse(
                "#player1",
                "player1",
                createBrawlerResponse(Brawler.JUJU, power = 11, trophies = 93),
            )
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.KNOCKOUT_5V5),
                battle =
                    BattleResultResponse(
                        mode = "knockout",
                        type = "ranked",
                        result = "victory",
                        duration = 144,
                        rank = null,
                        trophyChange = 12,
                        starPlayer = starPlayerResponse,
                        teams =
                            listOf(
                                listOf(
                                    createPlayerResponse(
                                        "#player1",
                                        "player1",
                                        createBrawlerResponse(Brawler.JUJU, trophies = 93),
                                    ),
                                    createPlayerResponse(
                                        "#player2",
                                        "player2",
                                        createBrawlerResponse(Brawler.LOLA, power = 1, trophies = 17),
                                    ),
                                    createPlayerResponse(
                                        "#player3",
                                        "player3",
                                        createBrawlerResponse(Brawler.PENNY, power = 4, trophies = 96),
                                    ),
                                    createPlayerResponse(
                                        "#player4",
                                        "player4",
                                        createBrawlerResponse(Brawler.DOUG, power = 4, trophies = 0),
                                    ),
                                    createPlayerResponse(
                                        "#player5",
                                        "player5",
                                        createBrawlerResponse(Brawler.POCO, power = 1, trophies = 17),
                                    ),
                                ),
                                listOf(
                                    createPlayerResponse(
                                        "#player6",
                                        "player6",
                                        createBrawlerResponse(Brawler.BULL, power = 4, trophies = 56),
                                    ),
                                    createPlayerResponse(
                                        "#player7",
                                        "player7",
                                        createBrawlerResponse(Brawler.WILLOW, power = 3, trophies = 56),
                                    ),
                                    createPlayerResponse(
                                        "#player8",
                                        "player8",
                                        createBrawlerResponse(Brawler.GROM, power = 5, trophies = 56),
                                    ),
                                    createPlayerResponse(
                                        "#player9",
                                        "player9",
                                        createBrawlerResponse(Brawler.EIGHT_BIT, power = 4, trophies = 48),
                                    ),
                                    createPlayerResponse(
                                        "#player10",
                                        "player10",
                                        createBrawlerResponse(Brawler.ROSA, power = 3, trophies = 48),
                                    ),
                                ),
                            ),
                        players = null,
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val result = sut.build()

        // then
        assertBattle(result, battleResponse, playerEntity)
        assertThat(result.starPlayerBrawlStarsTag).isEqualTo("#player1")
        assertBattleTeams(result.teams, battleResponse.battle.teams!!)
    }

    @Test
    fun `솔로_쇼다운에서_BattleEntity를_생성한다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.SOLO_SHOWDOWN),
                battle =
                    BattleResultResponse(
                        mode = "soloShowdown",
                        type = "ranked",
                        result = "victory",
                        duration = 117,
                        rank = 1,
                        trophyChange = 11,
                        starPlayer = null,
                        teams = null,
                        players =
                            listOf(
                                createPlayerResponse(
                                    "#player1",
                                    "player1",
                                    createBrawlerResponse(Brawler.GALE, trophies = 476),
                                ),
                                createPlayerResponse(
                                    "#player2",
                                    "player2",
                                    createBrawlerResponse(Brawler.GENE, trophies = 619),
                                ),
                                createPlayerResponse(
                                    "#player3",
                                    "player3",
                                    createBrawlerResponse(Brawler.BULL, trophies = 606),
                                ),
                                createPlayerResponse(
                                    "#player4",
                                    "player4",
                                    createBrawlerResponse(Brawler.BYRON, trophies = 601),
                                ),
                                createPlayerResponse(
                                    "#player5",
                                    "player5",
                                    createBrawlerResponse(Brawler.SHADE, trophies = 640),
                                ),
                                createPlayerResponse(
                                    "#player6",
                                    "player6",
                                    createBrawlerResponse(Brawler.COLT, trophies = 602),
                                ),
                                createPlayerResponse(
                                    "#player7",
                                    "player7",
                                    createBrawlerResponse(Brawler.ANGELO, trophies = 609),
                                ),
                                createPlayerResponse(
                                    "#player8",
                                    "player8",
                                    createBrawlerResponse(Brawler.PAM, trophies = 621),
                                ),
                                createPlayerResponse(
                                    "#player9",
                                    "player9",
                                    createBrawlerResponse(Brawler.LILY, trophies = 606),
                                ),
                                createPlayerResponse(
                                    "#player10",
                                    "player10",
                                    createBrawlerResponse(Brawler.EDGAR, trophies = 623),
                                ),
                            ),
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val result = sut.build()

        // then
        assertBattle(result, battleResponse, playerEntity)
        assertBattlePlayers(result.teams, battleResponse.battle.players!!)
    }

    @Test
    fun `듀얼에서_복수_브롤러로_BattleEntity를_생성한다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.DUEL),
                battle =
                    BattleResultResponse(
                        mode = "duel",
                        type = "ranked",
                        result = "victory",
                        duration = 117,
                        rank = 1,
                        trophyChange = 11,
                        starPlayer = null,
                        teams = null,
                        players =
                            listOf(
                                createPlayerResponse(
                                    "#player1",
                                    "player1",
                                    brawler = null,
                                    brawlers =
                                        listOf(
                                            createBrawlerResponse(Brawler.JUJU, trophies = 89, trophyChange = 4),
                                            createBrawlerResponse(Brawler.LILY, trophies = 581, trophyChange = 4),
                                            createBrawlerResponse(Brawler.GUS, trophies = 458, trophyChange = 4),
                                        ),
                                ),
                                createPlayerResponse(
                                    "#player2",
                                    "player2",
                                    brawler = null,
                                    brawlers =
                                        listOf(
                                            createBrawlerResponse(Brawler.COLT, trophies = 586, trophyChange = 0),
                                            createBrawlerResponse(Brawler.EIGHT_BIT, trophies = 590, trophyChange = 0),
                                            createBrawlerResponse(Brawler.BULL, trophies = 587, trophyChange = 0),
                                        ),
                                ),
                            ),
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val result = sut.build()

        // then
        assertBattle(result, battleResponse, playerEntity)
        assertBattlePlayers(result.teams, battleResponse.battle.players!!)
    }

    @Test
    fun `mode가_null인_경우_BattleEntity를_생성한다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.KNOCKOUT),
                battle =
                    BattleResultResponse(
                        mode = null,
                        type = "soloRanked",
                        result = "victory",
                        duration = 117,
                        rank = null,
                        trophyChange = null,
                        starPlayer = null,
                        teams =
                            listOf(
                                listOf(
                                    createPlayerResponse(
                                        "#player1",
                                        "player1",
                                        createBrawlerResponse(Brawler.JUJU),
                                    ),
                                    createPlayerResponse(
                                        "#player2",
                                        "player2",
                                        createBrawlerResponse(Brawler.TICK),
                                    ),
                                    createPlayerResponse(
                                        "#player3",
                                        "player3",
                                        createBrawlerResponse(Brawler.BUSTER),
                                    ),
                                ),
                                listOf(
                                    createPlayerResponse(
                                        "#player4",
                                        "player4",
                                        createBrawlerResponse(Brawler.SPROUT),
                                    ),
                                    createPlayerResponse(
                                        "#player5",
                                        "player5",
                                        createBrawlerResponse(Brawler.MOE),
                                    ),
                                    createPlayerResponse(
                                        "#player6",
                                        "player6",
                                        createBrawlerResponse(Brawler.SQUEAK),
                                    ),
                                ),
                            ),
                        players = null,
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val result = sut.build()

        // then
        assertThat(result.mode).isNull()
        assertBattle(result, battleResponse, playerEntity)
    }

    @Test
    fun `플레이어_정보가_없으면_예외를_발생시킨다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.KNOCKOUT),
                battle =
                    BattleResultResponse(
                        mode = "knockout",
                        type = "soloRanked",
                        result = "victory",
                        duration = 117,
                        rank = null,
                        trophyChange = null,
                        starPlayer = null,
                        teams = null,
                        players = null,
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val exception = catchThrowableOfType(IllegalArgumentException::class.java) { sut.build() }

        // then
        assertThat(exception).isNotNull()
        assertThat(exception.message).contains("플레이어 정보가 없습니다")
    }

    @Test
    fun `브롤러_정보가_없으면_예외를_발생시킨다`() {
        // given
        val playerEntity = createMockPlayerEntity()
        val battleResponse =
            BattleResponse(
                battleTime = battleTime,
                event = createEventResponse(BattleEvent.KNOCKOUT),
                battle =
                    BattleResultResponse(
                        mode = "knockout",
                        type = "soloRanked",
                        result = "victory",
                        duration = 117,
                        rank = null,
                        trophyChange = null,
                        starPlayer = null,
                        teams =
                            listOf(
                                listOf(
                                    createPlayerResponse(
                                        "#player1",
                                        "player1",
                                        brawler = null,
                                        brawlers = null,
                                    ),
                                ),
                            ),
                        players = null,
                    ),
            )

        // when
        val sut = BattleEntityBuilder(playerEntity, battleResponse)
        val exception = catchThrowableOfType(IllegalArgumentException::class.java) { sut.build() }

        // then
        assertThat(exception).isNotNull()
        assertThat(exception.message).contains("브롤러 정보가 없습니다")
    }

    private fun assertBattle(
        actual: com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity,
        expected: BattleResponse,
        playerEntity: PlayerEntity,
    ) {
        assertThat(actual.id).isNull()
        assertThat(actual.battleAt).isEqualTo(expected.battleTime)
        assertThat(actual.event.brawlStarsId).isEqualTo(expected.event.id)
        assertThat(actual.event.mode).isEqualTo(expected.event.mode)
        assertThat(actual.event.map).isEqualTo(expected.event.map)
        assertThat(actual.mode).isEqualTo(expected.battle.mode)
        assertThat(actual.type).isEqualTo(expected.battle.type)
        assertThat(actual.result).isEqualTo(expected.battle.result)
        assertThat(actual.duration).isEqualTo(expected.battle.duration)
        assertThat(actual.starPlayerBrawlStarsTag).isEqualTo(expected.battle.starPlayer?.tag)
        assertThat(actual.player.playerId).isEqualTo(playerEntity.id)
        assertThat(actual.player.rank).isEqualTo(expected.battle.rank)
        assertThat(actual.player.trophyChange).isEqualTo(expected.battle.trophyChange)
    }

    private fun assertBattleTeams(
        actuals: List<List<BattleEntityTeamPlayer>>,
        expecteds: List<List<BattleResultPlayerResponse>>,
    ) {
        assertThat(actuals).hasSize(expecteds.size)
        for (teamIdx in expecteds.indices) {
            var expectedPlayerSize = 0
            for (playerIdx in expecteds[teamIdx].indices) {
                val playerResponse = expecteds[teamIdx][playerIdx]
                if (playerResponse.brawler != null) {
                    assertBattlePlayerWithABrawler(
                        actuals[teamIdx][expectedPlayerSize],
                        playerResponse,
                    )
                    expectedPlayerSize += 1
                } else {
                    val brawlers = playerResponse.brawlers!!
                    for (brawlerIdx in brawlers.indices) {
                        assertBattlePlayerWithBrawlers(
                            actuals[teamIdx][expectedPlayerSize + brawlerIdx],
                            playerResponse,
                            brawlerIdx,
                        )
                    }
                    expectedPlayerSize += brawlers.size
                }
            }
            assertThat(actuals[teamIdx]).hasSize(expectedPlayerSize)
        }
    }

    private fun assertBattlePlayers(
        actuals: List<List<BattleEntityTeamPlayer>>,
        expecteds: List<BattleResultPlayerResponse>,
    ) {
        assertThat(actuals).hasSize(expecteds.size)
        for (idx in expecteds.indices) {
            val playerResponse = expecteds[idx]
            var actualBrawlerSize = 0
            if (playerResponse.brawler != null) {
                assertBattlePlayerWithABrawler(actuals[idx][0], playerResponse)
                actualBrawlerSize = 1
            } else {
                val brawlers = playerResponse.brawlers!!
                for (brawlerIdx in brawlers.indices) {
                    assertBattlePlayerWithBrawlers(actuals[idx][brawlerIdx], playerResponse, brawlerIdx)
                    actualBrawlerSize += 1
                }
            }
            assertThat(actuals[idx]).hasSize(actualBrawlerSize)
        }
    }

    private fun assertBattlePlayerWithABrawler(
        actual: BattleEntityTeamPlayer,
        expected: BattleResultPlayerResponse,
    ) {
        val expectedBrawler = expected.brawler!!
        assertThat(actual.brawlStarsTag).isEqualTo(expected.tag)
        assertThat(actual.name).isEqualTo(expected.name)
        assertThat(actual.brawler.brawlStarsId).isEqualTo(expectedBrawler.id)
        assertThat(actual.brawler.name).isEqualTo(expectedBrawler.name)
        assertThat(actual.brawler.power).isEqualTo(expectedBrawler.power)
        assertThat(actual.brawler.trophies).isEqualTo(expectedBrawler.trophies)
        assertThat(actual.brawler.trophyChange).isEqualTo(expectedBrawler.trophyChange)
    }

    private fun assertBattlePlayerWithBrawlers(
        actual: BattleEntityTeamPlayer,
        expected: BattleResultPlayerResponse,
        brawlerIdx: Int,
    ) {
        val expectedBrawlers = expected.brawlers!!
        assertThat(actual.brawlStarsTag).isEqualTo(expected.tag)
        assertThat(actual.name).isEqualTo(expected.name)
        assertThat(actual.brawler.brawlStarsId).isEqualTo(expectedBrawlers[brawlerIdx].id)
        assertThat(actual.brawler.name).isEqualTo(expectedBrawlers[brawlerIdx].name)
        assertThat(actual.brawler.power).isEqualTo(expectedBrawlers[brawlerIdx].power)
        assertThat(actual.brawler.trophies).isEqualTo(expectedBrawlers[brawlerIdx].trophies)
        assertThat(actual.brawler.trophyChange).isEqualTo(expectedBrawlers[brawlerIdx].trophyChange)
    }
}
