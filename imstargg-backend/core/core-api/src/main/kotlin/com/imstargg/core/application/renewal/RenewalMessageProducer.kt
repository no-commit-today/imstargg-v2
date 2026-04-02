package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag

interface RenewalMessageProducer {
    fun producePlayerRenewal(tag: BrawlStarsTag)

    fun produceClubRenewal(tag: BrawlStarsTag)
}
