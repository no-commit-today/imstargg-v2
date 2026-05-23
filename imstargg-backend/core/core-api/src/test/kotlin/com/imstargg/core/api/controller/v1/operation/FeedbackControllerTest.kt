package com.imstargg.core.api.controller.v1.operation

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class FeedbackControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val feedbackJpaRepository: FeedbackJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreApiContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `정상 제출시 201과 id를 반환한다`() {
            // given
            val body = """{"rating":"VERY_SATISFIED","content":"만족합니다"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.id") { exists() }
                }

            val saved = feedbackJpaRepository.findAll()
            assertThat(saved).hasSize(1)
            assertThat(saved[0].rating).isEqualTo(FeedbackRating.VERY_SATISFIED)
            assertThat(saved[0].content).isEqualTo("만족합니다")
        }

        @Test
        fun `content가 빈문자열이면 400을 반환한다`() {
            // given
            val body = """{"rating":"VERY_SATISFIED","content":"   "}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `content가 2000자를 초과하면 400을 반환한다`() {
            // given
            val longContent = "a".repeat(2001)
            val body = """{"rating":"VERY_SATISFIED","content":"$longContent"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `rating이 없으면 400을 반환한다`() {
            // given
            val body = """{"content":"내용"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `rating이 enum에 없는 값이면 400을 반환한다`() {
            // given
            val body = """{"rating":"BUG","content":"내용"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }
    }
