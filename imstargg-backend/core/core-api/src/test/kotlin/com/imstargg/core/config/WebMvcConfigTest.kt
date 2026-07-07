package com.imstargg.core.config

import com.imstargg.CoreApiContextTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "app.cors.allowed-origins=https://imstargg.com,http://localhost:3000",
    ],
)
class WebMvcConfigTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @Test
    fun `허용된 origin 의 OPTIONS 요청에 CORS 헤더가 응답된다`() {
        mockMvc
            .perform(
                options("/v1/players/%2388J8Q9RPV")
                    .header(HttpHeaders.ORIGIN, "https://imstargg.com")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "https://imstargg.com"))
    }

    @Test
    fun `브롤러 목록 GET 응답에 Cache-Control 헤더가 붙는다`() {
        mockMvc
            .perform(get("/v1/brawlers"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, stale-while-revalidate=60"))
    }

    @Test
    fun `브롤러 통계 GET 응답에 Cache-Control 헤더가 붙는다`() {
        mockMvc
            .perform(
                get("/v1/statistics/brawler/result")
                    .param("tierRange", "TROPHY_1000_PLUS")
                    .param("dateRange", "THIS_WEEK"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=1800, stale-while-revalidate=300"))
    }

    @Test
    fun `플레이어 조회 응답에는 Cache-Control 헤더가 없다`() {
        mockMvc
            .perform(get("/v1/players/%23NOPE"))
            .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
    }

    @Test
    fun `매칭 경로의 404 응답에는 Cache-Control 헤더가 없다`() {
        mockMvc
            .perform(get("/v1/events/99999999"))
            .andExpect(status().isNotFound)
            .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
    }

    @Test
    fun `매칭 경로의 400 응답에는 Cache-Control 헤더가 없다`() {
        mockMvc
            .perform(
                get("/v1/statistics/brawler/result")
                    .param("tierRange", "INVALID")
                    .param("dateRange", "THIS_WEEK"),
            ).andExpect(status().isBadRequest)
            .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
    }
}
