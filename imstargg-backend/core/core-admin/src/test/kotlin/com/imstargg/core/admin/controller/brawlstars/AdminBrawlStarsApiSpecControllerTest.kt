package com.imstargg.core.admin.controller.brawlstars

import com.imstargg.CoreAdminContextTest
import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiSpecJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@AutoConfigureMockMvc
class AdminBrawlStarsApiSpecControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val brawlStarsApiSpecJpaRepository: BrawlStarsApiSpecJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        private val baseCollectedAt: Instant = Instant.parse("2026-06-06T07:00:00Z")

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `목록은 collectedAt 내림차순으로 메타만 반환한다`() {
            // given
            brawlStarsApiSpecJpaRepository.save(
                BrawlStarsApiSpecEntity(
                    content = "swagger: \"2.0\"",
                    collectedAt = baseCollectedAt.minusSeconds(86400),
                ),
            )
            val newer =
                brawlStarsApiSpecJpaRepository.save(
                    BrawlStarsApiSpecEntity(
                        content = "swagger: \"2.0\"\nnewField: true",
                        collectedAt = baseCollectedAt,
                    ),
                )

            // when & then
            mockMvc
                .get("/api-specs")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(2) }
                    jsonPath("$[0].id") { value(newer.id!!.toInt()) }
                    jsonPath("$[0].collectedAt") { value("2026-06-06T07:00:00Z") }
                    jsonPath("$[0].lineCount") { value(2) }
                    jsonPath("$[0].sizeBytes") { exists() }
                    jsonPath("$[0].content") { doesNotExist() }
                }
        }

        @Test
        fun `상세는 원문과 이전 스냅샷 정보를 함께 반환한다`() {
            // given
            val previous =
                brawlStarsApiSpecJpaRepository.save(
                    BrawlStarsApiSpecEntity(
                        content = "swagger: \"2.0\"",
                        collectedAt = baseCollectedAt.minusSeconds(86400),
                    ),
                )
            val latest =
                brawlStarsApiSpecJpaRepository.save(
                    BrawlStarsApiSpecEntity(
                        content = "swagger: \"2.0\"\nnewField: true",
                        collectedAt = baseCollectedAt,
                    ),
                )

            // when & then
            mockMvc
                .get("/api-specs/${latest.id}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(latest.id!!.toInt()) }
                    jsonPath("$.collectedAt") { value("2026-06-06T07:00:00Z") }
                    jsonPath("$.content") { value("swagger: \"2.0\"\nnewField: true") }
                    jsonPath("$.previousId") { value(previous.id!!.toInt()) }
                    jsonPath("$.previousCollectedAt") { value("2026-06-05T07:00:00Z") }
                    jsonPath("$.previousContent") { value("swagger: \"2.0\"") }
                }
        }

        @Test
        fun `최초 스냅샷의 상세는 previousContent 가 null 이다`() {
            // given
            val first =
                brawlStarsApiSpecJpaRepository.save(
                    BrawlStarsApiSpecEntity(
                        content = "swagger: \"2.0\"",
                        collectedAt = baseCollectedAt,
                    ),
                )

            // when & then
            mockMvc
                .get("/api-specs/${first.id}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.content") { value("swagger: \"2.0\"") }
                    jsonPath("$.previousId") { value(null) }
                    jsonPath("$.previousContent") { value(null) }
                }
        }

        @Test
        fun `존재하지 않는 id 는 404 를 반환한다`() {
            // when & then
            mockMvc
                .get("/api-specs/999999")
                .andExpect {
                    status { isNotFound() }
                }
        }
    }
