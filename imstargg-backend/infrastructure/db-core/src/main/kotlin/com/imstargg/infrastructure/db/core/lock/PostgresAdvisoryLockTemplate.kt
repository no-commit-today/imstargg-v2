package com.imstargg.infrastructure.db.core.lock

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class PostgresAdvisoryLockTemplate(
    private val datasource: DataSource,
) : LockTemplate {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val LOCK = "SELECT pg_advisory_lock(?)"
        private const val UNLOCK = "SELECT pg_advisory_unlock(?)"
        private const val EXCEPTION_MESSAGE = "LOCK 을 수행하는 중에 오류가 발생하였습니다."
    }

    override fun <T> execute(
        lockName: LockName,
        timeoutSeconds: Int,
        supplier: () -> T,
    ): T {
        val lockKey = stable64BitKey(lockName.value)
        val connection = datasource.connection
        connection.use {
            var acquired = false
            try {
                log.debug(
                    "start getLock=[{}], lockKey=[{}], timeoutSeconds=[{}], connection=[{}]",
                    lockName,
                    lockKey,
                    timeoutSeconds,
                    connection,
                )
                acquireLock(connection, lockName, lockKey, timeoutSeconds)
                acquired = true
                log.debug(
                    "success getLock=[{}], lockKey=[{}], connection=[{}]",
                    lockName,
                    lockKey,
                    connection,
                )
                return supplier()
            } finally {
                if (acquired) {
                    log.debug(
                        "start releaseLock=[{}], lockKey=[{}], connection=[{}]",
                        lockName,
                        lockKey,
                        connection,
                    )
                    runCatching { releaseLock(connection, lockKey) }
                        .onFailure { log.error("pg_advisory_unlock 실패. lockKey=[{}]", lockKey, it) }
                    log.debug(
                        "success releaseLock=[{}], lockKey=[{}], connection=[{}]",
                        lockName,
                        lockKey,
                        connection,
                    )
                }
            }
        }
    }

    private fun acquireLock(
        connection: Connection,
        lockName: LockName,
        lockKey: Long,
        timeoutSeconds: Int,
    ) {
        connection.createStatement().use { it.execute("SET lock_timeout = '${timeoutSeconds}s'") }
        try {
            connection.prepareStatement(LOCK).use { stmt ->
                stmt.setLong(1, lockKey)
                stmt.execute()
            }
        } catch (e: SQLException) {
            log.error(
                "pg_advisory_lock 타임아웃. lockName=[{}], lockKey=[{}], timeoutSeconds=[{}]",
                lockName,
                lockKey,
                timeoutSeconds,
            )
            throw IllegalStateException(EXCEPTION_MESSAGE, e)
        } finally {
            connection.createStatement().use { it.execute("RESET lock_timeout") }
        }
    }

    private fun releaseLock(
        connection: Connection,
        lockKey: Long,
    ) {
        connection.prepareStatement(UNLOCK).use { stmt ->
            stmt.setLong(1, lockKey)
            stmt.executeQuery().use { rs ->
                if (!rs.next() || !rs.getBoolean(1)) {
                    log.warn("pg_advisory_unlock 실패. lockKey=[{}]", lockKey)
                }
            }
        }
    }

    private fun stable64BitKey(value: String): Long {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return ByteBuffer.wrap(digest, 0, Long.SIZE_BYTES).long
    }
}
