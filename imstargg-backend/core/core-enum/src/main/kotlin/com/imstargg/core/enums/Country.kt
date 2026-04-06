package com.imstargg.core.enums

enum class Country(
    override val code: String,
) : EnumCodeType {
    GLOBAL("global"),
    KOREA("kr"),
    CHINA("cn"),
    JAPAN("jp"),
    GERMANY("de"),
    UNITED_STATES("us"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(Country::class.java)
        }
    }
}
