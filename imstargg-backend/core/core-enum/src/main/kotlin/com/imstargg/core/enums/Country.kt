package com.imstargg.core.enums

enum class Country(
    override val code: String,
) : EnumCodeType {
    GLOBAL("global"),
    KOREA("kr"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(Country::class.java)
        }
    }
}
