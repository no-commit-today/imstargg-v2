package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.domain.renewal.RenewalCreate
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.RenewalJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

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
            renewalJpaRepository.findTopByBrawlStarsTagAndTypeOrderByRequestedAtDesc(
                tag.value,
                type,
            ) ?: return null
        return toRenewal(entity)
    }

    @Transactional
    fun create(renewalCreate: RenewalCreate): Renewal {
        val entity =
            RenewalEntity(
                brawlStarsTag = renewalCreate.tag.value,
                type = renewalCreate.type,
                requestedAt = renewalCreate.requestedAt,
            )
        renewalJpaRepository.save(entity)
        return toRenewal(entity)
    }

    @Transactional
    fun toPending(renewal: Renewal): Renewal {
        val entity =
            renewalJpaRepository.findTopByBrawlStarsTagAndTypeOrderByRequestedAtDesc(
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
