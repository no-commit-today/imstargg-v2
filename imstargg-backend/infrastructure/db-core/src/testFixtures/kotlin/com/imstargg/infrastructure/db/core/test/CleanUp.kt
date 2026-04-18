package com.imstargg.infrastructure.db.core.test

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import org.springframework.jdbc.core.JdbcTemplate

open class CleanUp(
    private val jdbcTemplate: JdbcTemplate,
    private val entityManagerFactory: EntityManagerFactory,
) {
    open fun all() {
        entityManagerFactory.metamodel.entities
            .forEach { entityType ->
                val tableName = getTableName(entityType.javaType)
                jdbcTemplate.execute("TRUNCATE TABLE $tableName")
            }
    }

    private fun <T> getTableName(entityType: Class<T>): String =
        entityType
            .getAnnotation(Table::class.java)
            ?.name
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException(
                "Entity[${entityType.name}] does not have @Table annotation or name attribute is empty.",
            )
}
