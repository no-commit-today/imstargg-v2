package com.imstargg

import com.imstargg.infrastructure.db.core.test.TestJpaConfig
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor

@Tag("context")
@DataJpaTest
@Import(TestJpaConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class DbCoreContextTest
