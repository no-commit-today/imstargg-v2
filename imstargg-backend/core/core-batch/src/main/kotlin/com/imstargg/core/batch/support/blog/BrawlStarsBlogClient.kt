package com.imstargg.core.batch.support.blog

import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * 브롤스타즈 공식 블로그를 읽는 클라이언트.
 *
 * 블로그는 Next.js 페이지라 HTML 안의 `__NEXT_DATA__` JSON 에 목록과 본문이 들어 있다.
 * 페이지가 기대한 모양이 아니면 [BrawlStarsBlogPageException] 이다.
 * 200 이 아닌 응답은 [RestClient] 가 던지는 예외를 그대로 둔다.
 *
 * `RestClient` 는 주입받은 빌더로 이 클래스 안에서 만든다. 빈으로 등록하면
 * core-api 호출용 `RestClient` 와 주입이 겹친다.
 */
@Component
class BrawlStarsBlogClient(
    restClientBuilder: RestClient.Builder,
) {
    private val restClient: RestClient = restClientBuilder.build()
    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    /** 목록의 [page] 쪽(1부터)을 읽는다. 제목은 앞뒤 공백을 뗀다. */
    fun getArticleSummaries(page: Int): List<BrawlStarsBlogArticleSummary> {
        val url = if (page <= 1) BLOG_URL else "${BLOG_URL}page/$page/"
        val articles = readNextData(url).path("props").path("pageProps").path("articles")
        if (!articles.isArray || articles.isEmpty) {
            throw BrawlStarsBlogPageException("목록에 글이 없다 - $url")
        }
        return articles
            .iterator()
            .asSequence()
            .map { toSummary(it, url) }
            .toList()
    }

    /** 글 페이지의 `bodyCollection` 을 JSON 문자열 그대로 돌려준다. */
    fun getRawBody(link: BrawlStarsNewsArticleLink): String {
        val url = link.linkUrl + "/"
        val nextData = readNextData(url)
        val pageName = nextData.path("page").asString("")
        if (pageName != ARTICLE_PAGE) {
            throw BrawlStarsBlogPageException("글 페이지가 아니다 - $url, page: $pageName")
        }
        val bodyCollection = nextData.path("props").path("pageProps").path("bodyCollection")
        if (!bodyCollection.isArray || bodyCollection.isEmpty) {
            throw BrawlStarsBlogPageException("글 본문이 없다 - $url")
        }
        return jsonMapper.writeValueAsString(bodyCollection)
    }

    private fun toSummary(
        article: JsonNode,
        url: String,
    ): BrawlStarsBlogArticleSummary {
        val title = article.path("title").asString("").trim()
        val linkUrl = article.path("linkUrl").asString("")
        val publishDate = article.path("publishDate").asString("")
        if (title.isEmpty() || linkUrl.isEmpty()) {
            throw BrawlStarsBlogPageException("목록의 글에 제목이나 주소가 없다 - $url, linkUrl: $linkUrl")
        }
        val publishedAt =
            parseInstant(publishDate)
                ?: throw BrawlStarsBlogPageException("목록의 글에 게시일이 없거나 해석되지 않는다 - $url, linkUrl: $linkUrl, publishDate: $publishDate")
        return BrawlStarsBlogArticleSummary(
            title = title,
            link = BrawlStarsNewsArticleLink.of(linkUrl),
            publishedAt = publishedAt,
        )
    }

    private fun parseInstant(value: String): Instant? =
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }

    private fun readNextData(url: String): JsonNode {
        // 응답에 문자 인코딩 표시가 없을 수 있어 바이트로 받아 UTF-8 로 직접 해석한다.
        val bytes =
            restClient
                .get()
                .uri(url)
                .retrieve()
                .body(ByteArray::class.java)
                ?: throw BrawlStarsBlogPageException("응답 본문이 없다 - $url")
        val html = String(bytes, Charsets.UTF_8)
        val json =
            NEXT_DATA_PATTERN.find(html)?.groupValues?.get(1)
                ?: throw BrawlStarsBlogPageException("__NEXT_DATA__ 가 없다 - $url")
        return try {
            jsonMapper.readTree(json)
        } catch (ex: Exception) {
            throw BrawlStarsBlogPageException("__NEXT_DATA__ 를 JSON 으로 읽지 못했다 - $url, ${ex.message}")
        }
    }

    companion object {
        private const val BLOG_URL = "https://supercell.com/en/games/brawlstars/blog/"
        private const val ARTICLE_PAGE = "/en/games/brawlstars/blog/[category]/[title]"
        private val NEXT_DATA_PATTERN =
            Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
    }
}
