package com.imstargg.infrastructure.db.core.lock

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

class MySqlUserLevelLockTemplate(
    private val datasource: DataSource,
) : LockTemplate {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val GET_LOCK = "SELECT GET_LOCK(?, ?)"
        private const val RELEASE_LOCK = "SELECT RELEASE_LOCK(?)"
        private const val EXCEPTION_MESSAGE = "LOCK 을 수행하는 중에 오류가 발생하였습니다."
    }

    override fun <T> execute(
        lockName: LockName,
        timeoutSeconds: Int,
        supplier: () -> T,
    ): T {
        datasource.connection.use {
            try {
                log.info("start getLock=[{}], timeoutSeconds : [{}], connection=[{}]", lockName, timeoutSeconds, it)
                getLock(it, lockName, timeoutSeconds)
                log.info(
                    "success getLock=[{}], timeoutSeconds : [{}], connection=[{}]",
                    lockName,
                    timeoutSeconds,
                    it,
                )
                return supplier()
            } finally {
                log.info("start releaseLock=[{}], connection=[{}]", lockName, it)
                releaseLock(it, lockName)
                log.info("success releaseLock=[{}], connection=[{}]", lockName, it)
            }
        }
    }

    private fun getLock(connection: Connection, lockName: LockName, timeoutSeconds: Int) {
        connection.prepareStatement(GET_LOCK).use { preparedStatement ->
            preparedStatement.setString(1, lockName.value)
            preparedStatement.setInt(2, timeoutSeconds)
            checkResultSet(lockName, preparedStatement, "GetLock_")
        }
    }

    private fun releaseLock(connection: Connection, lockName: LockName) {
        connection.prepareStatement(RELEASE_LOCK).use { preparedStatement ->
            preparedStatement.setString(1, lockName.value)
            checkResultSet(lockName, preparedStatement, "ReleaseLock_")
        }
    }

    private fun checkResultSet(lockName: LockName, preparedStatement: PreparedStatement, type: String) {
        preparedStatement.executeQuery().use { resultSet ->
            if (!resultSet.next()) {
                log.error(
                    "USER LEVEL LOCK 쿼리 결과 값이 없습니다. type = [{}], lockName : [{}], connection=[{}]",
                    type,
                    lockName,
                    preparedStatement.connection,
                )
                throw IllegalStateException(EXCEPTION_MESSAGE)
            }
            val result = resultSet.getInt(1)
            if (result != 1) {
                log.error(
                    "USER LEVEL LOCK 쿼리 결과 값이 1이 아닙니다. type = [{}], result : [{}] lockName : [{}], connection=[{}]",
                    type,
                    result,
                    lockName,
                    preparedStatement.connection,
                )
                throw IllegalStateException(EXCEPTION_MESSAGE)
            }
        }
    }
}