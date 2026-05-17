package com.imstargg.core.admin.controller.operation

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
class AdminFeedbackControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val feedbackJpaRepository: FeedbackJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `목록을 카테고리로 필터링할 수 있다`() {
            // given
            feedbackJpaRepository.save(
                FeedbackEntity(
                    category = FeedbackCategory.BUG,
                    content = "버그 리포트",
                    email = null,
                ),
            )
            feedbackJpaRepository.save(
                FeedbackEntity(
                    category = FeedbackCategory.FEATURE_REQUEST,
                    content = "기능 요청",
                    email = null,
                ),
            )

            // when & then
            mockMvc
                .get("/feedbacks") {
                    param("category", "BUG")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(1) }
                    jsonPath("$.content[0].category") { value("BUG") }
                }
        }

        @Test
        fun `PUT _feedbacks_{id}_check가 checked를 true로 만든다`() {
            // given
            val saved =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        category = FeedbackCategory.BUG,
                        content = "체크 테스트",
                        email = null,
                        checked = false,
                    ),
                )

            // when
            mockMvc
                .put("/feedbacks/${saved.id}/check") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"checked":true}"""
                }.andExpect {
                    // then
                    status { isNoContent() }
                }

            val updated = feedbackJpaRepository.findById(requireNotNull(saved.id)).get()
            assertThat(updated.checked).isTrue()
        }

        @Test
        fun `존재하지 않는 id에 PUT하면 404를 반환한다`() {
            // when & then
            mockMvc
                .put("/feedbacks/999999/check") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"checked":true}"""
                }.andExpect {
                    status { isNotFound() }
                }
        }
    }
