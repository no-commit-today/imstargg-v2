package com.imstargg.core.enums

enum class Language(
    override val code: String,
) : EnumCodeType {
    KOREAN("ko"),
    ENGLISH("en"),
    ;

    override val title: String
        get() = code

    companion object {
        val DEFAULT: Language = ENGLISH
    }
}
