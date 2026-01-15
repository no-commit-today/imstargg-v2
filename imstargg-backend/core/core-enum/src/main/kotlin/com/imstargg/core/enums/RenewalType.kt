package com.imstargg.core.enums

enum class RenewalType(
    override val code: String,
) : EnumCodeType {
    CLUB("club"),
    PLAYER("player"),
    ;

    override val title: String
        get() = code
}
