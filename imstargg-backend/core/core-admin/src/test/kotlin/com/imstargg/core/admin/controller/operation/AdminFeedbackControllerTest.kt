package com.imstargg.core.admin.controller.operation

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.operation.FeedbackRating
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
import java.time.Instant

@AutoConfigureMockMvc
class AdminFeedbackControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val feedbackJpaRepository: FeedbackJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        private val baseSubmittedAt: Instant = Instant.parse("2026-05-22T10:00:00Z")

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `목록을 rating으로 필터링할 수 있다`() {
            // given
            feedbackJpaRepository.save(
                FeedbackEntity(
                    rating = FeedbackRating.VERY_SATISFIED,
                    content = "매우 만족",
                    submittedAt = baseSubmittedAt,
                ),
            )
            feedbackJpaRepository.save(
                FeedbackEntity(
                    rating = FeedbackRating.DISSATISFIED,
                    content = "불만족",
                    submittedAt = baseSubmittedAt,
                ),
            )

            // when & then
            mockMvc
                .get("/feedbacks") {
                    param("rating", "VERY_SATISFIED")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(1) }
                    jsonPath("$.content[0].rating") { value("VERY_SATISFIED") }
                    jsonPath("$.content[0].submittedAt") { exists() }
                }
        }

        @Test
        fun `목록은 submittedAt 내림차순으로 정렬된다`() {
            // given
            val older =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        rating = FeedbackRating.NEUTRAL,
                        content = "오래된 의견",
                        submittedAt = baseSubmittedAt.minusSeconds(3600),
                    ),
                )
            val newer =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        rating = FeedbackRating.NEUTRAL,
                        content = "최근 의견",
                        submittedAt = baseSubmittedAt,
                    ),
                )

            // when & then
            mockMvc
                .get("/feedbacks")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.content[0].id") { value(newer.id!!.toInt()) }
                    jsonPath("$.content[1].id") { value(older.id!!.toInt()) }
                }
        }

        @Test
        fun `PUT _feedbacks_{id}_check가 checked를 true로 만든다`() {
            // given
            val saved =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        rating = FeedbackRating.SATISFIED,
                        content = "체크 테스트",
                        submittedAt = baseSubmittedAt,
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
