package com.imstargg.core.enums

enum class RenewalType(
    override val code: String,
) : EnumCodeType {
    CLUB("club"),
    PLAYER("player"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(RenewalType::class.java)
        }
    }
}
