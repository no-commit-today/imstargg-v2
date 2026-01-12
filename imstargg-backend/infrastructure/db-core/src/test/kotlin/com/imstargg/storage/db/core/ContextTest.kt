package com.imstargg.storage.db.core

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestConstructor

@Tag("context")
@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class ContextTest
