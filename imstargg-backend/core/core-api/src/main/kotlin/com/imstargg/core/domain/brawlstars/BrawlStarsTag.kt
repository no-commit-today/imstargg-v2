package com.imstargg.core.domain.brawlstars

@JvmInline
value class BrawlStarsTag(
    val value: String,
) {
    init {
        require(isValid(value)) { "Invalid BrawlStars tag: $value" }
    }

    companion object {
        private val PATTERN = Regex("#[A-Z0-9]+")

        fun isValid(value: String): Boolean = PATTERN.matches(value)
    }
}
