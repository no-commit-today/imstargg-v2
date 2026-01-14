package com.imstargg.core.enums

enum class Country(
    override val code: String,
) : EnumCodeType {
    GLOBAL("global"),
    KOREA("kr"),
    ;

    override val title: String
        get() = code
}
