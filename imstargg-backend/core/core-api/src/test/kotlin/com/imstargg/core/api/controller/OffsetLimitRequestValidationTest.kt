package com.imstargg.core.api.controller

import com.imstargg.CoreApiContextTest
import com.imstargg.core.application.brawlstars.PlayerService
import com.imstargg.core.domain.Slice
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class OffsetLimitRequestValidationTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @MockkBean
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setUp() {
        every { playerService.search(any(), any()) } returns Slice(content = emptyList(), hasNext = false)
    }

    @Test
    fun `limit 이 100 을 초과하면 400 과 필드 에러를 반환한다`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("limit", "101")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.fields[0].field") { value("limit") }
            }
    }

    @Test
    fun `limit 이 1 미만이면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("limit", "0")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `offset 이 음수면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("offset", "-1")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `offset 이 10000 을 초과하면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("offset", "10001")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `경계값 offset 10000 limit 100 은 200`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("offset", "10000")
                param("limit", "100")
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `하한 경계값 limit 1 은 200`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("limit", "1")
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `파라미터 생략 시 기본값으로 200`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
            }.andExpect { status { isOk() } }
    }
}
