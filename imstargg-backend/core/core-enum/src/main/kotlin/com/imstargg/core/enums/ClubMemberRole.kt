package com.imstargg.core.enums

enum class ClubMemberRole(
    override val code: String,
) : EnumCodeType {
    NOT_MEMBER("notMember"),
    MEMBER("member"),
    PRESIDENT("president"),
    SENIOR("senior"),
    VICE_PRESIDENT("vicePresident"),
    UNKNOWN("unknown"),
    ;

    companion object {
        init {
            EnumCodeType.validateNoDuplicateCodes(ClubMemberRole::class.java)
        }
    }
}
