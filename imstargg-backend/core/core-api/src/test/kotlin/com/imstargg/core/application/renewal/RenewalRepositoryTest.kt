package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.PlatformTransactionManager
import java.time.Instant

/**
 * 동시 경합의 영속 예외를 RENEWAL_ALREADY_IN_PROGRESS 로 번역하는지 검증한다.
 * 실제 동시성은 재현 불가하므로 이 케이스에 한해 JPA repo·트랜잭션 매니저를 stub 한다
 * (uk 위반은 save() 시점, 낙관락은 커밋 시점에 터진다 — 두 시점 모두 커버).
 */
class RenewalRepositoryTest {
    private val jpaRepository = mockk<RenewalJpaRepository>()
    private val transactionManager = mockk<PlatformTransactionManager>(relaxed = true)
    private val sut = RenewalRepository(jpaRepository, transactionManager)

    private val tag = BrawlStarsTag("#RACE")
    private val now = Instant.parse("2026-07-08T00:00:00Z")

    @Test
    fun `동시 최초 요청의 uk 제약 위반은 RENEWAL_ALREADY_IN_PROGRESS 로 번역된다`() {
        // given - 기존 갱신 없음 → save 시 uk 위반(중복 생성 경합)
        every { jpaRepository.findByBrawlStarsTagAndType(tag.value, RenewalType.PLAYER) } returns null
        every { jpaRepository.save(any()) } throws DataIntegrityViolationException("duplicate key")

        // when
        val ex = catchThrowableOfType(CoreException::class.java) { sut.request(tag, RenewalType.PLAYER, now) }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.RENEWAL_ALREADY_IN_PROGRESS)
    }

    @Test
    fun `동시 재요청의 낙관락 충돌은 RENEWAL_ALREADY_IN_PROGRESS 로 번역된다`() {
        // given - stale 한 기존 갱신(canRenew=true) → 커밋 시 @Version 충돌
        val stale =
            RenewalEntity(
                brawlStarsTag = tag.value,
                type = RenewalType.PLAYER,
                requestedAt = now.minusSeconds(3600),
            )
        every { jpaRepository.findByBrawlStarsTagAndType(tag.value, RenewalType.PLAYER) } returns stale
        every { transactionManager.commit(any()) } throws OptimisticLockingFailureException("version conflict")

        // when
        val ex = catchThrowableOfType(CoreException::class.java) { sut.request(tag, RenewalType.PLAYER, now) }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.RENEWAL_ALREADY_IN_PROGRESS)
    }
}
