package com.lectures.core.sync

/** Invalidates queued work when an account listener is stopped or replaced. */
internal class UserSyncSession {
    data class Token(val userId: String, val generation: Long)

    private var generation = 0L
    private var current: Token? = null

    @Synchronized
    fun begin(userId: String): Token {
        require(userId.isNotBlank())
        return current?.takeIf { it.userId == userId }
            ?: Token(userId, ++generation).also { current = it }
    }

    @Synchronized
    fun isCurrent(token: Token): Boolean = current == token

    @Synchronized
    fun currentUserId(): String? = current?.userId

    @Synchronized
    fun stop() {
        current = null
        generation++
    }
}

internal data class RemoteRecordChange<T>(val record: T, val removed: Boolean = false)
