package com.imstargg.client.brawlstars.response

data class BrawlerResponse(
    val id: Long,
    val name: String,
    val starPowers: List<StarPowerResponse>,
    val gadgets: List<AccessoryResponse>,
)
