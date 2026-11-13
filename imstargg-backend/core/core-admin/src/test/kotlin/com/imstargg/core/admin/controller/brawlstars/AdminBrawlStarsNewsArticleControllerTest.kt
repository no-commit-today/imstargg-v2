package com.imstargg.core.admin.controller.brawlstars

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.BrawlStarsNewsArticleJpaRepository
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
class AdminBrawlStarsNewsArticleControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val articleJpaRepository: BrawlStarsNewsArticleJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        private val publishedAt: Instant = Instant.parse("2026-11-01T09:00:00Z")

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        private fun save(
            slug: String,
            content: String = "본문 $slug",
            type: BrawlStarsNewsArticleType = BrawlStarsNewsArticleType.OTHER,
        ): BrawlStarsNewsArticleEntity =
            articleJpaRepository.save(
                BrawlStarsNewsArticleEntity(
                    link = BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/release-notes/$slug"),
                    title = "제목 $slug",
                    publishedAt = publishedAt,
                    type = type,
                    rawBody = "{\"raw\":\"$slug\"}",
                    content = content,
                ),
            )

        @Test
        fun `목록은 글마다 가장 최근 판 하나와 판 수를 준다`() {
            // given
            save("update65")
            val latest = save("update65-2")
            val other = save("update66")

            // when & then
            mockMvc
                .get("/news-articles")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(2) }
                    jsonPath("$[0].id") { value(other.id) }
                    jsonPath("$[0].articleKey") { value("update66") }
                    jsonPath("$[0].versionCount") { value(1) }
                    jsonPath("$[1].id") { value(latest.id) }
                    jsonPath("$[1].articleKey") { value("update65") }
                    jsonPath("$[1].linkUrl") { value("https://supercell.com/en/games/brawlstars/blog/release-notes/update65-2") }
                    jsonPath("$[1].title") { value("제목 update65-2") }
                    jsonPath("$[1].type") { value("OTHER") }
                    jsonPath("$[1].publishedAt") { value("2026-11-01T09:00:00Z") }
                    jsonPath("$[1].seenAt") { exists() }
                    jsonPath("$[1].versionCount") { value(2) }
                }
        }

        @Test
        fun `목록에는 본문이 없다`() {
            save("update65")

            mockMvc
                .get("/news-articles")
                .andExpect {
                    status { isOk() }
                    jsonPath("$[0].content") { doesNotExist() }
                    jsonPath("$[0].rawBody") { doesNotExist() }
                }
        }

        @Test
        fun `상세는 본문과 직전 판의 본문을 함께 준다`() {
            // given
            val first = save("update65", content = "첫 판 본문")
            val second = save("update65-2", content = "둘째 판 본문")

            // when & then
            mockMvc
                .get("/news-articles/${second.id}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.id") { value(second.id) }
                    jsonPath("$.articleKey") { value("update65") }
                    jsonPath("$.content") { value("둘째 판 본문") }
                    jsonPath("$.seenAt") { exists() }
                    jsonPath("$.previousId") { value(first.id) }
                    jsonPath("$.previousSeenAt") { exists() }
                    jsonPath("$.previousContent") { value("첫 판 본문") }
                    jsonPath("$.rawBody") { doesNotExist() }
                }
        }

        @Test
        fun `상세의 직전 판은 같은 주소의 판이 먼저다`() {
            // given
            val v1 = save("update65-2")
            save("update65-3")
            val v2 = save("update65-2")

            // when & then
            mockMvc
                .get("/news-articles/${v2.id}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.previousId") { value(v1.id) }
                }
        }

        @Test
        fun `직전 판이 없으면 previous 는 null 이다`() {
            val only = save("update65")

            mockMvc
                .get("/news-articles/${only.id}")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.previousId") { value(null) }
                    jsonPath("$.previousSeenAt") { value(null) }
                    jsonPath("$.previousContent") { value(null) }
                }
        }

        @Test
        fun `판 목록은 같은 주소 키의 판을 시간순으로 준다`() {
            // given
            val first = save("update65")
            val second = save("update65-2")
            save("update66")

            // when & then
            mockMvc
                .get("/news-articles/${second.id}/versions")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(2) }
                    jsonPath("$[0].id") { value(first.id) }
                    jsonPath("$[0].linkUrl") { value("https://supercell.com/en/games/brawlstars/blog/release-notes/update65") }
                    jsonPath("$[0].title") { value("제목 update65") }
                    jsonPath("$[0].publishedAt") { value("2026-11-01T09:00:00Z") }
                    jsonPath("$[0].seenAt") { exists() }
                    jsonPath("$[0].content") { doesNotExist() }
                    jsonPath("$[1].id") { value(second.id) }
                }
        }

        @Test
        fun `유형 수정은 같은 글의 모든 판에 적용된다`() {
            // given
            val first = save("update65")
            val second = save("update65-2")
            val other = save("update66")

            // when
            mockMvc
                .put("/news-articles/${first.id}/type") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"type":"RELEASE_NOTE"}"""
                }.andExpect { status { isNoContent() } }

            // then
            val types = articleJpaRepository.findAll().associate { it.id to it.type }
            assertThat(types[first.id]).isEqualTo(BrawlStarsNewsArticleType.RELEASE_NOTE)
            assertThat(types[second.id]).isEqualTo(BrawlStarsNewsArticleType.RELEASE_NOTE)
            assertThat(types[other.id]).isEqualTo(BrawlStarsNewsArticleType.OTHER)
        }

        @Test
        fun `모르는 유형은 400 이다`() {
            val article = save("update65")

            mockMvc
                .put("/news-articles/${article.id}/type") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"type":"UNKNOWN"}"""
                }.andExpect { status { isBadRequest() } }

            assertThat(articleJpaRepository.findById(article.id!!).get().type).isEqualTo(BrawlStarsNewsArticleType.OTHER)
        }

        @Test
        fun `없는 id 는 404 다`() {
            mockMvc.get("/news-articles/999999").andExpect { status { isNotFound() } }
            mockMvc.get("/news-articles/999999/versions").andExpect { status { isNotFound() } }
            mockMvc
                .put("/news-articles/999999/type") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"type":"RELEASE_NOTE"}"""
                }.andExpect { status { isNotFound() } }
        }
    }
