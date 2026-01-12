package com.imstargg.client.brawlstars.response

data class ScheduledEventLocationResponse(
    val id: Long,
    val mode: String,
    val map: String,
    val modifiers: List<String>?,
)
