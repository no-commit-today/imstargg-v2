package com.imstargg.infrastructure.db.core.lock

data class LockName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "락 이름은 비어있을 수 없습니다." }
        require(value.none { it.isWhitespace() }) { "락 이름에 공백을 포함할 수 없습니다." }
        require(value.length <= MAX_LENGTH) { "락 이름은 ${MAX_LENGTH}자를 초과할 수 없습니다. 현재 길이: ${value.length}" }
    }

    companion object {
        private const val MAX_LENGTH = 64

        fun createPlayerLockName(tag: String) = LockName(value = "player:$tag")

        fun createClubLockName(tag: String) = LockName(value = "club:$tag")

        fun createPlayerStatsBattleLockName(tag: String) = LockName(value = "player:$tag:stats:battle")
    }
}
