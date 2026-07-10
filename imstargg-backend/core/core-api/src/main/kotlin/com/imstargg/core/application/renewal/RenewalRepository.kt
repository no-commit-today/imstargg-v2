package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Repository
class RenewalRepository(
    private val renewalJpaRepository: RenewalJpaRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @Transactional(readOnly = true)
    fun find(
        tag: BrawlStarsTag,
        type: RenewalType,
    ): Renewal? {
        val entity =
            renewalJpaRepository.findByBrawlStarsTagAndType(
                tag.value,
                type,
            ) ?: return null
        return toRenewal(entity)
    }

    /**
     * 갱신을 요청 상태로 만들고 그 [Renewal] 을 반환한다. 이미 진행 중이면 [ErrorType.RENEWAL_ALREADY_IN_PROGRESS].
     *
     * "이미 진행 중" 은 세 경로로 감지되며 모두 이 application 계층에서 CoreException 으로 번역한다:
     *  - 논리: 아직 stale 하지 않은 기존 갱신 → [Renewal.canRenew] 검증
     *  - 물리(중복): 동시 최초 요청 경합 → uk_renewal__brawlstarstag_type 위반(DataIntegrityViolationException)
     *  - 물리(낙관락): 동시 재요청 경합 → @Version 충돌(OptimisticLockingFailureException)
     *
     * 낙관락 예외는 flush(커밋) 시점에 터진다. `@Transactional` 메서드였다면 커밋이 프록시로 넘어가 메서드
     * 내부에서 잡을 수 없으므로, TransactionTemplate 으로 커밋 경계를 메서드 안에 두어 execute() 바깥에서 잡는다.
     */
    fun request(
        tag: BrawlStarsTag,
        type: RenewalType,
        requestedAt: Instant,
    ): Renewal =
        try {
            requireNotNull(
                transactionTemplate.execute {
                    val entity = renewalJpaRepository.findByBrawlStarsTagAndType(tag.value, type)
                    if (entity == null) {
                        toRenewal(
                            renewalJpaRepository.save(
                                RenewalEntity(brawlStarsTag = tag.value, type = type, requestedAt = requestedAt),
                            ),
                        )
                    } else {
                        val current = toRenewal(entity)
                        if (!current.canRenew(requestedAt)) {
                            throw CoreException(ErrorType.RENEWAL_ALREADY_IN_PROGRESS, "tag=$tag, type=$type")
                        }
                        val requested = current.request(requestedAt)
                        entity.update(
                            status = requested.status,
                            requestedAt = requested.requestedAt,
                            executedAt = requested.executedAt,
                            completedAt = requested.completedAt,
                        )
                        requested
                    }
                },
            )
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.RENEWAL_ALREADY_IN_PROGRESS, "tag=$tag, type=$type")
        } catch (e: OptimisticLockingFailureException) {
            throw CoreException(ErrorType.RENEWAL_ALREADY_IN_PROGRESS, "tag=$tag, type=$type")
        }

    @Transactional
    fun toPending(renewal: Renewal): Renewal {
        val entity =
            renewalJpaRepository.findByBrawlStarsTagAndType(
                renewal.tag.value,
                renewal.type,
            ) ?: throw NoSuchElementException("Renewal not found: tag=${renewal.tag}, type=${renewal.type}")
        entity.toPending()
        return toRenewal(entity)
    }

    private fun toRenewal(entity: RenewalEntity): Renewal =
        Renewal(
            tag = BrawlStarsTag(entity.brawlStarsTag),
            type = entity.type,
            status = entity.status,
            requestedAt = entity.requestedAt,
            executedAt = entity.executedAt,
            completedAt = entity.completedAt,
        )
}
