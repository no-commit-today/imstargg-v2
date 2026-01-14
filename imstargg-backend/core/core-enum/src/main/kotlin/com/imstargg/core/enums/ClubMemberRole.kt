package com.imstargg.core.enums

enum class ClubMemberRole(
    override val code: String,
) : EnumCodeType {
    NOT_FOUND("notFound"),

    NOT_MEMBER("notMember"),
    MEMBER("member"),
    PRESIDENT("president"),
    SENIOR("senior"),
    VICE_PRESIDENT("vicePresident"),
    UNKNOWN("unknown"),
    ;

    override val title: String
        get() = code
}
