package com.imstargg.client.brawlstars.response

data class ScheduledEventLocationResponse(
    val id: Long,
    val mode: String,
    val modeId: Int,
    val map: String,
    val modifiers: List<String>?,
)
