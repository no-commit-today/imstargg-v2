package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
@Transactional(readOnly = true)
class RenewalRepository(
    private val renewalJpaRepository: RenewalJpaRepository,
) {
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
     * 갱신 요청을 저장한다. 진행 중(재요청 불가) 갱신이 이미 있으면 null.
     * 최초 요청 동시 경합은 uk_renewal__brawlstarstag_type 제약 위반(DataIntegrityViolationException)으로
     * 드러나며 호출부(RenewalService)가 RENEWAL_ALREADY_IN_PROGRESS 로 매핑한다.
     */
    @Transactional
    fun request(
        tag: BrawlStarsTag,
        type: RenewalType,
        requestedAt: Instant,
    ): Renewal? {
        val entity = renewalJpaRepository.findByBrawlStarsTagAndType(tag.value, type)
        if (entity != null) {
            if (!toRenewal(entity).canRenew(requestedAt)) return null
            entity.reRequest(requestedAt)
            return toRenewal(entity)
        }
        val newEntity =
            RenewalEntity(
                brawlStarsTag = tag.value,
                type = type,
                requestedAt = requestedAt,
            )
        renewalJpaRepository.save(newEntity)
        return toRenewal(newEntity)
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
