package com.imstargg.core.domain.brawlstars.player

@JvmInline
value class PlayerSearch private constructor(
    val query: String,
) {
    init {
        require(query.isNotBlank()) { "PlayerSearch query must not be blank" }
    }

    companion object {
        fun of(raw: String): PlayerSearch = PlayerSearch(raw.trim())
    }
}
