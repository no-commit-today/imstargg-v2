package com.imstargg

import com.imstargg.core.application.renewal.RenewalProducer
import com.imstargg.infrastructure.db.core.test.TestJpaConfig
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor

@Tag("context")
@SpringBootTest
@Import(TestJpaConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class CoreApiContextTest {
    @MockkBean
    private lateinit var renewalProducer: RenewalProducer
}
