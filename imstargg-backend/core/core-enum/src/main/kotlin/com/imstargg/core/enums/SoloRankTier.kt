package com.imstargg.core.enums

enum class SoloRankTier(
    val value: Int,
) {
    BRONZE_1(1),
    BRONZE_2(2),
    BRONZE_3(3),
    SILVER_1(4),
    SILVER_2(5),
    SILVER_3(6),
    GOLD_1(7),
    GOLD_2(8),
    GOLD_3(9),
    DIAMOND_1(10),
    DIAMOND_2(11),
    DIAMOND_3(12),
    MYTHIC_1(13),
    MYTHIC_2(14),
    MYTHIC_3(15),
    LEGENDARY_1(16),
    LEGENDARY_2(17),
    LEGENDARY_3(18),
    MASTER_1(19),
    MASTER_2(20),
    MASTER_3(21),
    PRO(22),
    ;

    companion object {
        fun of(value: Int): SoloRankTier =
            entries.find { it.value == value }
                ?: if (value < 1) BRONZE_1 else PRO
    }
}
