package com.imstargg.infrastructure.db.core.lock

import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource

class PostgresAdvisoryLockTemplate(
    private val datasource: DataSource,
) : LockTemplate {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TRY_LOCK = "SELECT pg_try_advisory_lock(?)"
        private const val UNLOCK = "SELECT pg_advisory_unlock(?)"
        private const val EXCEPTION_MESSAGE = "LOCK 을 수행하는 중에 오류가 발생하였습니다."
        private const val RETRY_INTERVAL_MS = 100L
    }

    override fun <T> execute(
        lockName: LockName,
        timeoutSeconds: Int,
        supplier: () -> T,
    ): T {
        val lockKey = lockName.value.hashCode().toLong()
        val connection = datasource.connection
        connection.use {
            try {
                log.debug(
                    "start getLock=[{}], lockKey=[{}], timeoutSeconds=[{}], connection=[{}]",
                    lockName,
                    lockKey,
                    timeoutSeconds,
                    connection,
                )
                acquireLock(connection, lockName, lockKey, timeoutSeconds)
                log.debug(
                    "success getLock=[{}], lockKey=[{}], connection=[{}]",
                    lockName,
                    lockKey,
                    connection,
                )
                return supplier()
            } finally {
                log.debug(
                    "start releaseLock=[{}], lockKey=[{}], connection=[{}]",
                    lockName,
                    lockKey,
                    connection,
                )
                releaseLock(connection, lockKey)
                log.debug(
                    "success releaseLock=[{}], lockKey=[{}], connection=[{}]",
                    lockName,
                    lockKey,
                    connection,
                )
            }
        }
    }

    private fun acquireLock(
        connection: Connection,
        lockName: LockName,
        lockKey: Long,
        timeoutSeconds: Int,
    ) {
        val deadlineMs = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (true) {
            connection.prepareStatement(TRY_LOCK).use { stmt ->
                stmt.setLong(1, lockKey)
                stmt.executeQuery().use { rs ->
                    if (rs.next() && rs.getBoolean(1)) {
                        return
                    }
                }
            }
            if (System.currentTimeMillis() >= deadlineMs) {
                log.error(
                    "pg_try_advisory_lock 타임아웃. lockName=[{}], lockKey=[{}], timeoutSeconds=[{}]",
                    lockName,
                    lockKey,
                    timeoutSeconds,
                )
                throw IllegalStateException(EXCEPTION_MESSAGE)
            }
            Thread.sleep(RETRY_INTERVAL_MS)
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
}
