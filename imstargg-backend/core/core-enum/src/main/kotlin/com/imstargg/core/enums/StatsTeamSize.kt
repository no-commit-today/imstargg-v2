package com.imstargg.core.enums

enum class StatsTeamSize(
    val value: Int,
) {
    SIZE_2(2),
    SIZE_3(3),
    ;

    companion object {
        fun fromValue(value: Int): StatsTeamSize =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("Invalid team size: $value")
    }
}
