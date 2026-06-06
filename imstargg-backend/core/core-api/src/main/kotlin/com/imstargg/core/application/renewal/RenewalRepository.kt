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

    @Transactional
    fun request(
        tag: BrawlStarsTag,
        type: RenewalType,
        requestedAt: Instant,
    ): Renewal {
        val entity = renewalJpaRepository.findByBrawlStarsTagAndType(tag.value, type)
        if (entity != null) {
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
