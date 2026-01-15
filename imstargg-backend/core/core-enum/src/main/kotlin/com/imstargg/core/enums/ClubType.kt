package com.imstargg.core.enums

enum class ClubType(
    override val code: String,
) : EnumCodeType {
    NOT_FOUND("notFound"),

    OPEN("open"),
    INVITE_ONLY("inviteOnly"),
    CLOSED("closed"),
    UNKNOWN("unknown"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(ClubType::class.java)
        }
    }
}
