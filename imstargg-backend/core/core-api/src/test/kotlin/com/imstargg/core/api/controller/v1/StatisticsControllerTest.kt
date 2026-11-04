package com.imstargg.core.api.controller.v1

import com.imstargg.CoreApiContextTest
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerPairResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTrioResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.ComboResultRankingItemResponse
import com.imstargg.core.api.controller.v1.response.MessagesResponse
import com.imstargg.core.api.query.StatisticsQueryComposer
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Clock

@AutoConfigureMockMvc
class StatisticsControllerTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var statisticsQueryComposer: StatisticsQueryComposer

    @MockkBean(relaxed = true)
    private lateinit var clock: Clock

    @Test
    fun `pair result 에 brawlerId 가 없으면 400`() {
        mockMvc
            .get("/v1/statistics/brawler/pair/result") {
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `pair result 의 legacy sort 파라미터와 eventId 는 무시한다`() {
        mockMvc
            .get("/v1/statistics/brawler/pair/result") {
                param("eventId", "1")
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
                param("sortBy", "IGNORED")
                param("sortOrder", "SIDEWAYS")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `enemy result 의 legacy sort 파라미터와 eventId 는 무시한다`() {
        mockMvc
            .get("/v1/statistics/brawler/enemy/result") {
                param("eventId", "1")
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
                param("sortBy", "IGNORED")
                param("sortOrder", "SIDEWAYS")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `brawler trio result 에 tierRange 가 없으면 400`() {
        mockMvc
            .get("/v1/statistics/brawler/trio/result") {
                param("brawlerId", "16000000")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `brawler trio result 는 필수 파라미터가 있으면 200`() {
        mockMvc
            .get("/v1/statistics/brawler/trio/result") {
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `pair ranking 에 tierRange 가 없으면 400`() {
        mockMvc
            .get("/v1/statistics/pair/result") {
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `pair ranking 은 필수 파라미터가 있으면 200`() {
        mockMvc
            .get("/v1/statistics/pair/result") {
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `trio ranking 에 tierRange 가 없으면 400`() {
        mockMvc
            .get("/v1/statistics/trio/result") {
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `trio ranking 은 필수 파라미터가 있으면 200`() {
        mockMvc
            .get("/v1/statistics/trio/result") {
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `pair result 응답 형태`() {
        every { statisticsQueryComposer.getBrawlerPairResultStatistics(any(), any(), any()) } returns
            listOf(
                BrawlerPairResultStatisticsItemResponse(
                    brawler = brawler(16000000L),
                    pairBrawler = brawler(16000001L),
                    battleCount = 120L,
                    pickRate = 0.0034,
                    winRate = 0.55,
                    tierScore = 53,
                    tier = "A",
                ),
            )

        mockMvc
            .get("/v1/statistics/brawler/pair/result") {
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
                jsonPath("$.content[0].brawler.id") { value(16000000) }
                jsonPath("$.content[0].pairBrawler.id") { value(16000001) }
                jsonPath("$.content[0].battleCount") { value(120) }
                jsonPath("$.content[0].pickRate") { value(0.0034) }
                jsonPath("$.content[0].winRate") { value(0.55) }
                jsonPath("$.content[0].tierScore") { isNumber() }
                jsonPath("$.content[0].tier") { isString() }
            }
    }

    @Test
    fun `enemy result 응답 형태`() {
        every { statisticsQueryComposer.getEnemyResultStatistics(any(), any(), any()) } returns
            listOf(
                BrawlerEnemyResultStatisticsItemResponse(
                    brawler = brawler(16000000L),
                    enemyBrawler = brawler(16000002L),
                    battleCount = 90L,
                    pickRate = 0.2,
                    winRate = 0.45,
                ),
            )

        mockMvc
            .get("/v1/statistics/brawler/enemy/result") {
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
                jsonPath("$.content[0].brawler.id") { value(16000000) }
                jsonPath("$.content[0].enemyBrawler.id") { value(16000002) }
                jsonPath("$.content[0].battleCount") { value(90) }
                jsonPath("$.content[0].pickRate") { value(0.2) }
                jsonPath("$.content[0].winRate") { value(0.45) }
                jsonPath("$.content[0].tierScore") { doesNotExist() }
                jsonPath("$.content[0].tier") { doesNotExist() }
            }
    }

    @Test
    fun `brawler trio result 응답 형태`() {
        every { statisticsQueryComposer.getBrawlerTrioResultStatistics(any(), any(), any()) } returns
            listOf(
                BrawlerTrioResultStatisticsItemResponse(
                    brawler = brawler(16000000L),
                    pairBrawlers = listOf(brawler(16000001L), brawler(16000002L)),
                    battleCount = 40L,
                    pickRate = 0.00012,
                    winRate = 0.6,
                    tierScore = 55,
                    tier = "S",
                ),
            )

        mockMvc
            .get("/v1/statistics/brawler/trio/result") {
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
                jsonPath("$.content[0].brawler.id") { value(16000000) }
                jsonPath("$.content[0].pairBrawlers.length()") { value(2) }
                jsonPath("$.content[0].pairBrawlers[1].id") { value(16000002) }
                jsonPath("$.content[0].battleCount") { value(40) }
                jsonPath("$.content[0].tierScore") { isNumber() }
                jsonPath("$.content[0].tier") { isString() }
            }
    }

    @Test
    fun `pair ranking 응답 형태`() {
        every { statisticsQueryComposer.getPairResultRanking(any(), any()) } returns
            listOf(comboRankingItem(16000000L, 16000001L))

        mockMvc
            .get("/v1/statistics/pair/result") {
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
                jsonPath("$.content[0].brawlers.length()") { value(2) }
                jsonPath("$.content[0].battleCount") { value(150) }
                jsonPath("$.content[0].pickRate") { isNumber() }
                jsonPath("$.content[0].winRate") { isNumber() }
                jsonPath("$.content[0].tierScore") { isNumber() }
                jsonPath("$.content[0].tier") { isString() }
            }
    }

    @Test
    fun `trio ranking 응답 형태`() {
        every { statisticsQueryComposer.getTrioResultRanking(any(), any()) } returns
            listOf(comboRankingItem(16000000L, 16000001L, 16000002L))

        mockMvc
            .get("/v1/statistics/trio/result") {
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isOk() }
                jsonPath("$.content[0].brawlers.length()") { value(3) }
                jsonPath("$.content[0].brawlers[2].id") { value(16000002) }
                jsonPath("$.content[0].tierScore") { isNumber() }
                jsonPath("$.content[0].tier") { isString() }
            }
    }

    @Test
    fun `pair rank endpoint 는 삭제되어 404`() {
        mockMvc
            .get("/v1/statistics/brawler/pair/rank") {
                param("eventId", "60000001")
                param("brawlerId", "16000000")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `team result endpoint 는 등록되어 있지 않다`() {
        mockMvc
            .get("/v1/statistics/brawler/team/result") {
                param("brawlerIds", "1", "2")
                param("teamSize", "SIZE_2")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `team rank endpoint 는 등록되어 있지 않다`() {
        mockMvc
            .get("/v1/statistics/brawler/team/rank") {
                param("eventId", "60000001")
                param("brawlerIds", "1", "2")
                param("teamSize", "SIZE_2")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isNotFound() }
            }
    }

    private fun brawler(id: Long) =
        BrawlerResponse(
            id = id,
            name = MessagesResponse(mapOf("ko" to "브롤러$id", "en" to "Brawler$id")),
            rarity = BrawlerRarity.EPIC,
            role = BrawlerRole.TANK,
            portraitImageUrl = null,
        )

    private fun comboRankingItem(vararg brawlerIds: Long) =
        ComboResultRankingItemResponse(
            brawlers = brawlerIds.map { brawler(it) },
            battleCount = 150L,
            pickRate = 0.01,
            winRate = 0.56,
            tierScore = 54,
            tier = "A",
        )
}
