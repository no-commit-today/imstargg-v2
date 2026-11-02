package com.imstargg.client.brawlstars

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset

class BrawlStarsClientTest {
    private val builder = RestClient.builder().baseUrl("http://localhost")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val sut =
        BrawlStarsClient(
            HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(BrawlStarsApi::class.java),
        )

    @AfterEach
    fun tearDown() {
        server.verify()
    }

    @Test
    fun `태그의 # 을 퍼센트 인코딩해 battlelog 를 GET 한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV/battlelog"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(ClassPathResource("brawlstars/battlelog.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerRecentBattles("#88J8Q9RPV")

        // then
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `battleTime 을 읽고 모르는 필드는 무시한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV/battlelog"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/battlelog.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerRecentBattles("#88J8Q9RPV")

        // then
        assertThat(result.items.single().battleTime).isEqualTo(OffsetDateTime.of(2026, 11, 2, 1, 2, 3, 0, ZoneOffset.UTC))
    }

    @Test
    fun `플레이어 응답의 3vs3Victories·soloVictories·duoVictories·isQualifiedFromChampionshipChallenge 를 이름대로 읽는다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/player.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerInformation("#88J8Q9RPV")

        // then
        assertThat(result.victories3vs3).isEqualTo(7000)
        assertThat(result.victoriesSolo).isEqualTo(800)
        assertThat(result.victoriesDuo).isEqualTo(900)
        assertThat(result.isQualifiedFromChampionshipChallenge).isTrue()
    }

    @Test
    fun `브롤러 이름이 null 인 battlelog 를 디코딩한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%23P1/battlelog"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/battlelog-hidden-brawler.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerRecentBattles("#P1")

        // then
        val battle = result.items.single().battle
        val teams = battle.teams!!
        assertThat(teams[0][0].brawler!!.name).isNull()
        assertThat(battle.starPlayer!!.brawler!!.name).isNull()
        assertThat(teams[0][1].brawler!!.name).isEqualTo("SHADE")
    }

    @Test
    fun `nameColor 가 null 인 플레이어 응답을 디코딩한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/player-name-color-null.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerInformation("#88J8Q9RPV")

        // then
        assertThat(result.nameColor).isNull()
        assertThat(result.name).isEqualTo("Player")
    }

    @Test
    fun `빈 club 객체가 있는 플레이어 응답을 디코딩한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/player-empty-club.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerInformation("#88J8Q9RPV")

        // then
        assertThat(result.club!!.tag).isNull()
        assertThat(result.club!!.name).isNull()
    }

    @Test
    fun `nameColor 가 null 인 클럽 멤버를 디코딩한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/clubs/%232U8CVLQQ9"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/club-name-color-null.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getClub("#2U8CVLQQ9")

        // then
        assertThat(result.members.single().nameColor).isNull()
        assertThat(result.members.single().name).isEqualTo("Player")
    }

    @Test
    fun `nameColor 가 null 인 랭킹 항목을 디코딩한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/rankings/kr/players"))
            .andRespond(withSuccess(ClassPathResource("brawlstars/ranking-name-color-null.json"), MediaType.APPLICATION_JSON))

        // when
        val result = sut.getPlayerRanking("kr")

        // then
        assertThat(result.items.single().nameColor).isNull()
        assertThat(result.items.single().name).isEqualTo("Player")
    }

    @Test
    fun `API 명세는 application octet-stream 본문을 문자열로 받는다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1"))
            .andRespond(withSuccess("swagger: \"2.0\"", MediaType.APPLICATION_OCTET_STREAM))

        // when
        val result = sut.getApiSpec()

        // then
        assertThat(result).isEqualTo("swagger: \"2.0\"")
    }

    @Test
    fun `브롤러 랭킹은 국가와 브롤러 ID 경로로 쿼리 없이 GET 한다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/rankings/kr/brawlers/16000000"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""{"items":[],"paging":null}""", MediaType.APPLICATION_JSON))

        // when
        val result = sut.getBrawlerRanking("kr", 16000000L)

        // then
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `404 는 HttpClientErrorException NotFound 로 올린다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV/battlelog"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        // when
        val thrown = catchThrowableOfType(HttpClientErrorException.NotFound::class.java) { sut.getPlayerRecentBattles("#88J8Q9RPV") }

        // then
        assertThat(thrown).isNotNull()
    }

    @Test
    fun `503 은 HttpServerErrorException ServiceUnavailable 로 올린다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV/battlelog"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        // when
        val thrown =
            catchThrowableOfType(HttpServerErrorException.ServiceUnavailable::class.java) {
                sut.getPlayerRecentBattles("#88J8Q9RPV")
            }

        // then
        assertThat(thrown).isNotNull()
    }

    @Test
    fun `해석 실패는 RestClientException 이고 RestClientResponseException 이 아니다`() {
        // given
        server
            .expect(requestTo("http://localhost/v1/players/%2388J8Q9RPV/battlelog"))
            .andRespond(withSuccess("{", MediaType.APPLICATION_JSON))

        // when
        val thrown = catchThrowableOfType(RestClientException::class.java) { sut.getPlayerRecentBattles("#88J8Q9RPV") }

        // then
        assertThat(thrown).isNotNull()
        assertThat(thrown).isNotInstanceOf(RestClientResponseException::class.java)
    }
}
