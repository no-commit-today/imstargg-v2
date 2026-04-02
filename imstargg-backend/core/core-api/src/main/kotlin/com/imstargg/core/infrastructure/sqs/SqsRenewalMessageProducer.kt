package com.imstargg.core.infrastructure.sqs

import com.imstargg.core.application.renewal.RenewalMessageProducer
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.infrastructure.aws.sqs.RenewalSqsProperties
import com.imstargg.infrastructure.aws.sqs.message.ClubRenewalMessage
import com.imstargg.infrastructure.aws.sqs.message.PlayerRenewalMessage
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.springframework.stereotype.Component

@Component
class SqsRenewalMessageProducer(
    private val sqsTemplate: SqsTemplate,
    private val renewalSqsProperties: RenewalSqsProperties,
) : RenewalMessageProducer {
    override fun producePlayerRenewal(tag: BrawlStarsTag) {
        sqsTemplate.send { it.queue(renewalSqsProperties.playerQueueName).payload(PlayerRenewalMessage(tag.value)) }
    }

    override fun produceClubRenewal(tag: BrawlStarsTag) {
        sqsTemplate.send { it.queue(renewalSqsProperties.clubQueueName).payload(ClubRenewalMessage(tag.value)) }
    }
}
