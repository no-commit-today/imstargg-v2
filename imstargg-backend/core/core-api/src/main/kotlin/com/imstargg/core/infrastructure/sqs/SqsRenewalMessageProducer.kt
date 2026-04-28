package com.imstargg.core.infrastructure.sqs

import com.imstargg.core.application.renewal.RenewalMessageProducer
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.infrastructure.aws.sqs.RenewalSqsProperties
import com.imstargg.infrastructure.aws.sqs.message.ClubRenewalMessage
import com.imstargg.infrastructure.aws.sqs.message.PlayerRenewalMessage
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SqsRenewalMessageProducer(
    private val sqsTemplate: SqsTemplate,
    private val renewalSqsProperties: RenewalSqsProperties,
) : RenewalMessageProducer {
    companion object {
        private val log = LoggerFactory.getLogger(SqsRenewalMessageProducer::class.java)
    }

    override fun producePlayerRenewal(tag: BrawlStarsTag) {
        sqsTemplate.send { it.queue(renewalSqsProperties.playerQueueName).payload(PlayerRenewalMessage(tag.value)) }
        log.debug("[SQS] PlayerRenewalMessage 발행 tag={}", tag.value)
    }

    override fun produceClubRenewal(tag: BrawlStarsTag) {
        sqsTemplate.send { it.queue(renewalSqsProperties.clubQueueName).payload(ClubRenewalMessage(tag.value)) }
        log.debug("[SQS] ClubRenewalMessage 발행 tag={}", tag.value)
    }
}
