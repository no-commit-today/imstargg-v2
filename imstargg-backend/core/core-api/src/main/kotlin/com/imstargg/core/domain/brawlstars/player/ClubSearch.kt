package com.imstargg.core.domain.brawlstars.player

@JvmInline
value class ClubSearch private constructor(
    val query: String,
) {
    init {
        require(query.isNotBlank()) { "ClubSearch query must not be blank" }
    }

    companion object {
        fun of(raw: String): ClubSearch = ClubSearch(raw.trim())
    }
}
