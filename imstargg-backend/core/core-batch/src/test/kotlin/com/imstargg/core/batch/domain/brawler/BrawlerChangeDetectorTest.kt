package com.imstargg.core.batch.domain.brawler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerChangeDetectorTest {
    private val sut = BrawlerChangeDetector()

    @Test
    fun `신규 브롤러는 BrawlerAdded 로만 감지하고 항목 변경은 방출하지 않는다`() {
        // given
        val apiBrawlers =
            listOf(
                BrawlerSnapshot(
                    id = 16000000,
                    name = "Shelly",
                    items =
                        mapOf(
                            BrawlerItemType.STAR_POWER to setOf(BrawlerItemRef(id = 23000076, name = "Shell Shock")),
                            BrawlerItemType.GEAR to setOf(BrawlerItemRef(id = 62000001, name = "Speed")),
                        ),
                ),
            )
        val stored = StoredBrawlers(brawlerIds = emptySet(), itemIds = emptyMap())

        // when
        val result = sut.detect(apiBrawlers = apiBrawlers, stored = stored)

        // then
        assertThat(result).containsExactly(
            BrawlerChange.BrawlerAdded(brawlerId = 16000000, brawlerName = "Shelly"),
        )
    }

    @Test
    fun `삭제된 브롤러는 BrawlerRemoved 로만 감지하고 항목 변경은 방출하지 않는다`() {
        // given
        val stored =
            StoredBrawlers(
                brawlerIds = setOf(16000000),
                itemIds =
                    mapOf(
                        16000000L to mapOf(BrawlerItemType.STAR_POWER to setOf(23000076L)),
                    ),
            )

        // when
        val result = sut.detect(apiBrawlers = emptyList(), stored = stored)

        // then
        assertThat(result).containsExactly(
            BrawlerChange.BrawlerRemoved(brawlerId = 16000000),
        )
    }

    @Test
    fun `기존 브롤러의 항목 4종 추가를 감지한다`() {
        // given
        val apiBrawlers =
            listOf(
                BrawlerSnapshot(
                    id = 16000000,
                    name = "Shelly",
                    items =
                        mapOf(
                            BrawlerItemType.STAR_POWER to
                                setOf(
                                    BrawlerItemRef(id = 23000076, name = "Shell Shock"),
                                    BrawlerItemRef(id = 23000077, name = "Band-Aid"),
                                ),
                            BrawlerItemType.GADGET to setOf(BrawlerItemRef(id = 23000255, name = "Fast Forward")),
                            BrawlerItemType.HYPER_CHARGE to setOf(BrawlerItemRef(id = 26000001, name = "Double Barrel")),
                            BrawlerItemType.GEAR to setOf(BrawlerItemRef(id = 62000001, name = "Speed")),
                        ),
                ),
            )
        val stored =
            StoredBrawlers(
                brawlerIds = setOf(16000000),
                itemIds =
                    mapOf(
                        16000000L to mapOf(BrawlerItemType.STAR_POWER to setOf(23000076L)),
                    ),
            )

        // when
        val result = sut.detect(apiBrawlers = apiBrawlers, stored = stored)

        // then
        assertThat(result).containsExactlyInAnyOrder(
            BrawlerChange.ItemAdded(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.STAR_POWER,
                itemId = 23000077,
                itemName = "Band-Aid",
            ),
            BrawlerChange.ItemAdded(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.GADGET,
                itemId = 23000255,
                itemName = "Fast Forward",
            ),
            BrawlerChange.ItemAdded(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.HYPER_CHARGE,
                itemId = 26000001,
                itemName = "Double Barrel",
            ),
            BrawlerChange.ItemAdded(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.GEAR,
                itemId = 62000001,
                itemName = "Speed",
            ),
        )
    }

    @Test
    fun `기존 브롤러의 기어 삭제를 감지한다`() {
        // given
        val apiBrawlers =
            listOf(
                BrawlerSnapshot(
                    id = 16000000,
                    name = "Shelly",
                    items =
                        mapOf(
                            BrawlerItemType.GEAR to setOf(BrawlerItemRef(id = 62000001, name = "Speed")),
                        ),
                ),
            )
        val stored =
            StoredBrawlers(
                brawlerIds = setOf(16000000),
                itemIds =
                    mapOf(
                        16000000L to mapOf(BrawlerItemType.GEAR to setOf(62000001L, 62000002L)),
                    ),
            )

        // when
        val result = sut.detect(apiBrawlers = apiBrawlers, stored = stored)

        // then
        assertThat(result).containsExactly(
            BrawlerChange.ItemRemoved(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.GEAR,
                itemId = 62000002,
            ),
        )
    }

    @Test
    fun `같은 항목 타입의 추가와 삭제를 동시에 감지한다`() {
        // given
        val apiBrawlers =
            listOf(
                BrawlerSnapshot(
                    id = 16000000,
                    name = "Shelly",
                    items =
                        mapOf(
                            BrawlerItemType.STAR_POWER to
                                setOf(
                                    BrawlerItemRef(id = 1, name = "New Star Power"),
                                    BrawlerItemRef(id = 2, name = "Kept Star Power"),
                                ),
                        ),
                ),
            )
        val stored =
            StoredBrawlers(
                brawlerIds = setOf(16000000),
                itemIds =
                    mapOf(
                        16000000L to mapOf(BrawlerItemType.STAR_POWER to setOf(2L, 3L)),
                    ),
            )

        // when
        val result = sut.detect(apiBrawlers = apiBrawlers, stored = stored)

        // then
        assertThat(result).containsExactlyInAnyOrder(
            BrawlerChange.ItemAdded(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.STAR_POWER,
                itemId = 1,
                itemName = "New Star Power",
            ),
            BrawlerChange.ItemRemoved(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.STAR_POWER,
                itemId = 3,
            ),
        )
    }

    @Test
    fun `DB 에 항목 기록이 없는 기존 브롤러는 모든 항목을 추가로 감지한다`() {
        // given
        val apiBrawlers =
            listOf(
                BrawlerSnapshot(
                    id = 16000000,
                    name = "Shelly",
                    items =
                        mapOf(
                            BrawlerItemType.GADGET to setOf(BrawlerItemRef(id = 23000255, name = "Fast Forward")),
                        ),
                ),
            )
        val stored = StoredBrawlers(brawlerIds = setOf(16000000), itemIds = emptyMap())

        // when
        val result = sut.detect(apiBrawlers = apiBrawlers, stored = stored)

        // then
        assertThat(result).containsExactly(
            BrawlerChange.ItemAdded(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerItemType.GADGET,
                itemId = 23000255,
                itemName = "Fast Forward",
            ),
        )
    }

    @Test
    fun `변경이 없으면 빈 리스트를 반환한다`() {
        // given
        val apiBrawlers =
            listOf(
                BrawlerSnapshot(
                    id = 16000000,
                    name = "Shelly",
                    items =
                        mapOf(
                            BrawlerItemType.STAR_POWER to setOf(BrawlerItemRef(id = 23000076, name = "Shell Shock")),
                            BrawlerItemType.GEAR to setOf(BrawlerItemRef(id = 62000001, name = "Speed")),
                        ),
                ),
            )
        val stored =
            StoredBrawlers(
                brawlerIds = setOf(16000000),
                itemIds =
                    mapOf(
                        16000000L to
                            mapOf(
                                BrawlerItemType.STAR_POWER to setOf(23000076L),
                                BrawlerItemType.GEAR to setOf(62000001L),
                            ),
                    ),
            )

        // when
        val result = sut.detect(apiBrawlers = apiBrawlers, stored = stored)

        // then
        assertThat(result).isEmpty()
    }
}
