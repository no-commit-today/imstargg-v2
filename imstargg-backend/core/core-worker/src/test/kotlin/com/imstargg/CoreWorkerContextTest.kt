package com.imstargg

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.storage.db.core.test.TestJpaConfig
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor

@Tag("context")
@SpringBootTest
@Import(TestJpaConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class CoreWorkerContextTest {
    @MockkBean
    private lateinit var brawlStarsClient: BrawlStarsClient
}
