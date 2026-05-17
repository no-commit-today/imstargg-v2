package com.imstargg.core.api.controller.v1.operation

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackCategory
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
            val body = """{"category":"BUG","content":"버그가 발생했어요","email":"user@example.com"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                    header("X-Forwarded-For", "1.2.3.4")
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.id") { exists() }
                }

            val saved = feedbackJpaRepository.findAll()
            assertThat(saved).hasSize(1)
            assertThat(saved[0].category).isEqualTo(FeedbackCategory.BUG)
            assertThat(saved[0].content).isEqualTo("버그가 발생했어요")
            assertThat(saved[0].email).isEqualTo("user@example.com")
            assertThat(saved[0].ipHash).hasSize(64)
        }

        @Test
        fun `content가 빈문자열이면 400을 반환한다`() {
            // given
            val body = """{"category":"BUG","content":"   "}"""

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
        fun `잘못된 이메일 형식이면 400을 반환한다`() {
            // given
            val body = """{"category":"BUG","content":"내용","email":"not-an-email"}"""

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
