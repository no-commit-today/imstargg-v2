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
    fun `pair rank 에 brawlerId 가 없으면 400`() {
        mockMvc
            .get("/v1/statistics/brawler/pair/rank") {
                param("eventId", "60000001")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
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
