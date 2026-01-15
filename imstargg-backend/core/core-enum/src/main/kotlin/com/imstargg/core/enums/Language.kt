package com.imstargg.core.enums

enum class Language(
    override val code: String,
) : EnumCodeType {
    KOREAN("ko"),
    ENGLISH("en"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(Language::class.java)
        }

        val DEFAULT: Language = ENGLISH
    }
}
