package com.imstargg.core.domain.brawlstars

import com.imstargg.core.enums.Language

data class Message(
    val language: Language,
    val content: String,
)
