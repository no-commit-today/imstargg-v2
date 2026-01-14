package com.imstargg.storage.db.core.test

import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Table
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

open class TestDataManager(
    private val jdbcTemplate: JdbcTemplate,
    entityManagerFactory: EntityManagerFactory,
) {
    private val tem = TestEntityManager(entityManagerFactory)

    @Transactional
    open fun add(entity: Any) {
        tem.persist(entity)
    }

    @Transactional
    open fun <E : Any> find(
        entityClass: Class<E>,
        primaryKey: Any,
    ): E? = tem.find(entityClass, primaryKey)

    @Transactional(readOnly = true)
    open fun <E : Any> findAll(entityClass: Class<E>): List<E> = tem.entityManager
        .createQuery("SELECT e FROM ${entityClass.simpleName} e", entityClass)
        .resultList

    @Transactional
    open fun cleanUp() {
        tem.entityManager.metamodel.entities
            .forEach { entityType ->
                val tableName = getTableName(entityType.javaType)
                jdbcTemplate.execute("TRUNCATE TABLE $tableName")
            }
    }

    fun <T> getTableName(entityType: Class<T>): String = entityType
        .getAnnotation(Table::class.java)
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException(
            "Entity[${entityType.name}] does not have @Table annotation or name attribute is empty.",
        )
}
