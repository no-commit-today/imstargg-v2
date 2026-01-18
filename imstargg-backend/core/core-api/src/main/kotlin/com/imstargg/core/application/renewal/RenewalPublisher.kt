package com.imstargg.core.application.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag

interface RenewalPublisher {
    fun publishPlayerRenewal(tag: BrawlStarsTag)

    fun publishClubRenewal(tag: BrawlStarsTag)
}
