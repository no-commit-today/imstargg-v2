package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.response.GadgetResponse
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.HyperChargeResponse
import com.imstargg.core.admin.response.StarPowerResponse
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType

data class BrawlerDetailAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val imageUrl: String?,
    val buffyImages: Map<BrawlStarsImageType, String?>,
    val starPowers: List<StarPowerAdminResponse>,
    val gadgets: List<GadgetAdminResponse>,
    val hyperCharges: List<HyperChargeAdminResponse>,
    val linkedGearBrawlStarsIds: Set<Long>,
    val allGears: List<GearSummaryAdminResponse>,
) {
    companion object {
        fun from(response: BrawlerResponse) =
            BrawlerDetailAdminResponse(
                brawlStarsId = response.brawler.brawlStarsId,
                names = response.names,
                rarity = response.brawler.rarity,
                role = response.brawler.role,
                imageUrl = response.image?.url,
                buffyImages = response.buffyImages.mapValues { it.value?.url },
                starPowers = response.starPowers.map { StarPowerAdminResponse.from(it) },
                gadgets = response.gadgets.map { GadgetAdminResponse.from(it) },
                hyperCharges = response.hyperCharges.map { HyperChargeAdminResponse.from(it) },
                linkedGearBrawlStarsIds = response.linkedGearIds,
                allGears = response.allGears.map { GearSummaryAdminResponse.from(it) },
            )
    }
}

data class StarPowerAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(response: StarPowerResponse) =
            StarPowerAdminResponse(
                brawlStarsId = response.starPower.brawlStarsId,
                names = response.names,
                imageUrl = response.image?.url,
            )
    }
}

data class GadgetAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(response: GadgetResponse) =
            GadgetAdminResponse(
                brawlStarsId = response.gadget.brawlStarsId,
                names = response.names,
                imageUrl = response.image?.url,
            )
    }
}

data class HyperChargeAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(response: HyperChargeResponse) =
            HyperChargeAdminResponse(
                brawlStarsId = response.hyperCharge.brawlStarsId,
                names = response.names,
                imageUrl = response.image?.url,
            )
    }
}

data class GearSummaryAdminResponse(
    val brawlStarsId: Long,
    val rarity: GearRarity,
) {
    companion object {
        fun from(response: GearResponse) =
            GearSummaryAdminResponse(
                brawlStarsId = response.gear.brawlStarsId,
                rarity = response.gear.rarity,
            )
    }
}
