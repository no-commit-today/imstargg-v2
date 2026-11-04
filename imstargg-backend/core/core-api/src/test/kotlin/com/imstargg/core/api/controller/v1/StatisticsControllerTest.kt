package com.imstargg.core.api.controller.v1

import com.imstargg.CoreApiContextTest
import com.imstargg.core.api.query.StatisticsQueryComposer
import com.ninjasquad.springmockk.MockkBean
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
}
