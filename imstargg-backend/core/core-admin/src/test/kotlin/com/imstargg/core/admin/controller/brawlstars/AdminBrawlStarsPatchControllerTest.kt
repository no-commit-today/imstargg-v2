package com.imstargg.core.admin.controller.brawlstars

import com.imstargg.CoreAdminContextTest
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@AutoConfigureMockMvc
@Import(AdminBrawlStarsPatchControllerTest.FixedClockConfig::class)
class AdminBrawlStarsPatchControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val patchJpaRepository: BrawlStarsPatchJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @TestConfiguration
        class FixedClockConfig {
            @Bean
            @Primary
            fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-12-01T03:00:00Z"), KOREA_ZONE)
        }

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        private fun save(
            date: String,
            memo: String? = null,
        ): BrawlStarsPatchEntity = patchJpaRepository.save(BrawlStarsPatchEntity(LocalDate.parse(date), memo))

        private fun body(
            date: String,
            memo: String? = null,
        ): String = """{"patchDate":"$date","memo":${memo?.let { "\"$it\"" } ?: "null"}}"""

        @Test
        fun `목록은 오늘과 가장 이른 날짜와 패치일을 늦은 순서로 준다`() {
            save("2026-11-10", "옛")
            save("2026-11-25", "새")

            mockMvc
                .get("/patches")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.today") { value("2026-12-01") }
                    jsonPath("$.earliestRegistrableDate") { value("2026-11-01") }
                    jsonPath("$.patches.length()") { value(2) }
                    jsonPath("$.patches[0].patchDate") { value("2026-11-25") }
                    jsonPath("$.patches[0].memo") { value("새") }
                    jsonPath("$.patches[1].patchDate") { value("2026-11-10") }
                }
        }

        @ParameterizedTest
        @CsvSource("2026-12-01, 200", "2026-11-01, 200", "2026-10-31, 400", "2026-12-02, 400")
        fun `등록은 기한 안의 날짜만 받는다`(
            patchDate: String,
            status: Int,
        ) {
            mockMvc
                .post("/patches") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body(patchDate, "  메모  ")
                }.andExpect {
                    status { isEqualTo(status) }
                    if (status == 200) {
                        jsonPath("$.patchDate") { value(patchDate) }
                        jsonPath("$.memo") { value("메모") }
                        jsonPath("$.id") { exists() }
                    }
                }
            assertThat(patchJpaRepository.existsByPatchDate(LocalDate.parse(patchDate))).isEqualTo(status == 200)
        }

        @Test
        fun `같은 날짜를 두 번 등록하면 400 이다`() {
            save("2026-11-25")

            mockMvc
                .post("/patches") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body("2026-11-25")
                }.andExpect { status { isBadRequest() } }
            assertThat(patchJpaRepository.count()).isEqualTo(1)
        }

        @Test
        fun `날짜 형식이 틀리면 400 이다`() {
            mockMvc
                .post("/patches") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body("2026/11/25")
                }.andExpect { status { isBadRequest() } }
        }

        @Test
        fun `메모가 255자를 넘으면 400 이다`() {
            mockMvc
                .post("/patches") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body("2026-11-25", "가".repeat(256))
                }.andExpect { status { isBadRequest() } }
            assertThat(patchJpaRepository.count()).isZero()
        }

        @Test
        fun `날짜를 바꾸는 수정은 옛 날짜와 새 날짜가 모두 기한 안이어야 한다`() {
            val inRange = save("2026-11-10")
            val outOfRange = save("2026-10-01")

            // 새 날짜가 기한 밖
            put(inRange.id!!, body("2026-10-31")).andExpect { status { isBadRequest() } }
            // 옛 날짜가 기한 밖
            put(outOfRange.id!!, body("2026-11-20")).andExpect { status { isBadRequest() } }
            // 둘 다 기한 안
            put(inRange.id!!, body("2026-11-20", "수정")).andExpect { status { isNoContent() } }

            val updated = patchJpaRepository.findById(inRange.id!!).orElseThrow()
            assertThat(updated.patchDate).isEqualTo(LocalDate.parse("2026-11-20"))
            assertThat(updated.memo).isEqualTo("수정")
            assertThat(patchJpaRepository.findById(outOfRange.id!!).orElseThrow().patchDate)
                .isEqualTo(LocalDate.parse("2026-10-01"))
        }

        @Test
        fun `옛 날짜가 기한 밖이면 메모만 고칠 수 있다`() {
            val old = save("2026-10-01", "옛 메모")

            put(old.id!!, body("2026-10-01", "새 메모")).andExpect { status { isNoContent() } }
            put(old.id!!, body("2026-10-02", "새 메모")).andExpect { status { isBadRequest() } }

            val reloaded = patchJpaRepository.findById(old.id!!).orElseThrow()
            assertThat(reloaded.patchDate).isEqualTo(LocalDate.parse("2026-10-01"))
            assertThat(reloaded.memo).isEqualTo("새 메모")
        }

        @Test
        fun `이미 있는 날짜로 수정하면 400 이다`() {
            val first = save("2026-11-10")
            save("2026-11-20")

            put(first.id!!, body("2026-11-20")).andExpect { status { isBadRequest() } }

            assertThat(patchJpaRepository.findById(first.id!!).orElseThrow().patchDate)
                .isEqualTo(LocalDate.parse("2026-11-10"))
        }

        @Test
        fun `기한 밖의 패치일은 삭제할 수 없다`() {
            val old = save("2026-10-01")
            val recent = save("2026-11-10")

            mockMvc.delete("/patches/${old.id}").andExpect { status { isBadRequest() } }
            mockMvc.delete("/patches/${recent.id}").andExpect { status { isNoContent() } }

            assertThat(patchJpaRepository.findAll().map { it.id }).containsExactly(old.id)
        }

        @Test
        fun `없는 id 의 수정과 삭제는 404 다`() {
            put(9999L, body("2026-11-10")).andExpect { status { isNotFound() } }
            mockMvc.delete("/patches/9999").andExpect { status { isNotFound() } }
        }

        private fun put(
            id: Long,
            content: String,
        ) = mockMvc.put("/patches/$id") {
            contentType = MediaType.APPLICATION_JSON
            this.content = content
        }
    }
