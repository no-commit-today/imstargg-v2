package com.imstargg.infrastructure.db.core.lock

data class LockName(
    val value: String,
) {

    companion object {
        fun createPlayerLockName(tag: String) = LockName(
            value = "player:$tag",
        )

        fun createClubLockName(tag: String) = LockName(
            value = "club:$tag",
        )
    }
}
