package com.imstargg.core.infrastructure.pubsub

import com.google.cloud.spring.pubsub.core.PubSubTemplate
import com.imstargg.core.application.renewal.RenewalPublisher
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.infrastructure.pubsub.ClubRenewalEvent
import com.imstargg.infrastructure.pubsub.ClubRenewalPubSubProperties
import com.imstargg.infrastructure.pubsub.PlayerRenewalEvent
import com.imstargg.infrastructure.pubsub.PlayerRenewalPubSubProperties
import org.springframework.stereotype.Component

@Component
class PubSubRenewalPublisher(
    private val pubSubTemplate: PubSubTemplate,
    private val playerRenewalPubSubProperties: PlayerRenewalPubSubProperties,
    private val clubRenewalPubSubProperties: ClubRenewalPubSubProperties,
) : RenewalPublisher {
    override fun publishPlayerRenewal(tag: BrawlStarsTag) {
        pubSubTemplate.publish(
            playerRenewalPubSubProperties.topic.name,
            PlayerRenewalEvent(tag.value),
        )
    }

    override fun publishClubRenewal(tag: BrawlStarsTag) {
        pubSubTemplate.publish(
            clubRenewalPubSubProperties.topic.name,
            ClubRenewalEvent(tag.value),
        )
    }
}
