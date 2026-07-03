package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiSpecJpaRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BrawlStarsApiSpecTrackTaskTest {
    private val fixedInstant = Instant.parse("2026-06-06T07:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val brawlStarsClient = mockk<BrawlStarsClient>()
    private val brawlStarsApiSpecJpaRepository = mockk<BrawlStarsApiSpecJpaRepository>()
    private val alertManager = mockk<AlertManager>()
    private val sut =
        BrawlStarsApiSpecTrackTask(
            clock = clock,
            brawlStarsClient = brawlStarsClient,
            brawlStarsApiSpecJpaRepository = brawlStarsApiSpecJpaRepository,
            alertManager = alertManager,
        )

    @Test
    fun `최초 수집이면 정규화본 스냅샷을 저장하고 알림을 보내지 않는다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns "swagger: \"2.0\""
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns null
        val savedSlot = slot<BrawlStarsApiSpecEntity>()
        every { brawlStarsApiSpecJpaRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        // when
        sut.track()

        // then
        assertThat(savedSlot.captured.content).isEqualTo("swagger: '2.0'\n")
        assertThat(savedSlot.captured.collectedAt).isEqualTo(fixedInstant)
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `직전 스냅샷과 정규화본이 같으면 저장도 알림도 하지 않는다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns "a: 1\nb: 2"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "a: 1\nb: 2\n", collectedAt = fixedInstant.minusSeconds(86400))

        // when
        sut.track()

        // then
        verify(exactly = 0) { brawlStarsApiSpecJpaRepository.save(any()) }
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `맵 키 순서만 다른 응답은 변경으로 판정하지 않는다`() {
        // given — 원문 문자열 비교였다면 오탐 알림이 났을 입력
        every { brawlStarsClient.getApiSpec() } returns "b: 2\na: 1"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "a: 1\nb: 2\n", collectedAt = fixedInstant.minusSeconds(86400))

        // when
        sut.track()

        // then
        verify(exactly = 0) { brawlStarsApiSpecJpaRepository.save(any()) }
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `직전 스냅샷이 정규화 이전 원문이면 알림 없이 정규화본으로 이관 저장한다`() {
        // given — 의미는 같지만 저장형이 비정렬 원문인 레거시 행
        every { brawlStarsClient.getApiSpec() } returns "a: 1\nb: 2"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "b: 2\na: 1", collectedAt = fixedInstant.minusSeconds(86400))
        val savedSlot = slot<BrawlStarsApiSpecEntity>()
        every { brawlStarsApiSpecJpaRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        // when
        sut.track()

        // then
        assertThat(savedSlot.captured.content).isEqualTo("a: 1\nb: 2\n")
        assertThat(savedSlot.captured.collectedAt).isEqualTo(fixedInstant)
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `응답이 비어있으면 예외가 발생하고 저장하지 않는다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns " "

        // when
        val thrown = catchThrowableOfType(IllegalStateException::class.java) { sut.track() }

        // then
        assertThat(thrown).isNotNull()
        verify(exactly = 0) { brawlStarsApiSpecJpaRepository.save(any()) }
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `정규화본이 다르면 새 스냅샷을 저장하고 변경 알림을 보낸다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns "b: 2\na: 1\nnewField: true"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "a: 1\nb: 2\n", collectedAt = fixedInstant.minusSeconds(86400))
        val savedSlot = slot<BrawlStarsApiSpecEntity>()
        every { brawlStarsApiSpecJpaRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
        justRun { alertManager.alert(any()) }

        // when
        sut.track()

        // then
        assertThat(savedSlot.captured.content).isEqualTo("a: 1\nb: 2\nnewField: true\n")
        assertThat(savedSlot.captured.collectedAt).isEqualTo(fixedInstant)
        verify(exactly = 1) { alertManager.alert(any()) }
    }
}
