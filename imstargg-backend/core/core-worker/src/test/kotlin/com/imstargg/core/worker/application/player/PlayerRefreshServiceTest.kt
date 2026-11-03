package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

/**
 * ACTIVE 플레이어를 한 명만 저장한다 — 무작위 시작 ID 가 늘 그 플레이어 ID 이하라 매번 그 플레이어가 뽑힌다.
 */
class PlayerRefreshServiceTest(
    private val sut: PlayerRefreshService,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRepository: PlayerJpaRepository,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `플레이어 조회가 404 면 플레이어를 DELETED 로 바꾼다`() {
        // given
        val tag = "#REFRESH404"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        every { brawlStarsClient.getPlayerInformation(tag) } throws notFound()

        // when
        sut.refresh()

        // then
        assertThat(playerRepository.findByBrawlStarsTag(tag)!!.status).isEqualTo(PlayerStatus.DELETED)
    }

    @Test
    fun `503 이면 점검을 등록한다`() {
        // given
        val tag = "#REFRESH503"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())

        every { brawlStarsClient.getPlayerInformation(tag) } throws serviceUnavailable()

        // when
        sut.refresh()

        // then
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    private fun notFound(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)

    private fun serviceUnavailable(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)
}
