package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.query.StatisticsQueryComposer
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Clock

@Tag("context")
@WebMvcTest(StatisticsController::class)
class StatisticsControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean(relaxed = true)
    private lateinit var statisticsQueryComposer: StatisticsQueryComposer

    @MockkBean(relaxed = true)
    private lateinit var clock: Clock

    @Test
    fun `team result 에 brawlerIds 4 개 이상이면 400`() {
        mockMvc
            .get("/v1/statistics/brawler/team/result") {
                param("brawlerIds", "1", "2", "3", "4")
                param("teamSize", "SIZE_3")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `team rank 에 brawlerIds 4 개 이상이면 400`() {
        mockMvc
            .get("/v1/statistics/brawler/team/rank") {
                param("eventId", "60000001")
                param("brawlerIds", "1", "2", "3", "4")
                param("teamSize", "SIZE_3")
                param("tierRange", "TROPHY_1000_PLUS")
                param("dateRange", "THIS_WEEK")
            }.andExpect {
                status { isBadRequest() }
            }
    }
}
