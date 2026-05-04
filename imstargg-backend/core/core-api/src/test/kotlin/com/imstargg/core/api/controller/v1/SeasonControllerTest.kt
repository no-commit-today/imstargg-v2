package com.imstargg.core.api.controller.v1

import com.imstargg.core.config.CorsProperties
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tag("context")
@WebMvcTest(SeasonController::class)
class SeasonControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockkBean(relaxed = true)
    private lateinit var corsProperties: CorsProperties

    @MockkBean(relaxed = true)
    private lateinit var clock: Clock

    @Test
    fun `브롤패스 시즌 목록을 반환한다`() {
        every { clock.instant() } returns Instant.parse("2026-05-05T00:00:00Z")
        every { clock.zone } returns ZoneOffset.UTC

        mockMvc
            .get("/v1/seasons/brawl-pass")
            .andExpect {
                status { isOk() }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content[0].number") { value(1) }
                jsonPath("$.content[0].startAt") { value("2022-04-07T18:00:00+09:00") }
            }
    }

    @Test
    fun `솔로랭크 시즌 목록을 반환한다`() {
        every { clock.instant() } returns Instant.parse("2025-04-01T00:00:00Z")
        every { clock.zone } returns ZoneOffset.UTC

        mockMvc
            .get("/v1/seasons/solo-rank")
            .andExpect {
                status { isOk() }
                jsonPath("$.content[0].key") { value("1-1") }
                jsonPath("$.content[0].number") { value(1) }
                jsonPath("$.content[0].month") { value(1) }
            }
    }
}
