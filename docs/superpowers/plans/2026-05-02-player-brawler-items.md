# Player Brawler 아이템 보유 모델링 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Brawler 카탈로그를 메타와 아이템으로 분리하고 PlayerBrawler 가 카탈로그 아이템을 `PlayerBrawlerItem<T>(item, owned)` 로 감싸 보유 여부를 1급 개념으로 모델링한다. 프론트엔드 플레이어 페이지에 보유 인디케이터를 노출한다.

**Architecture:** 도메인은 `Brawler`(메타) + `BrawlerItems`(아이템 풀) + `BrawlerDetail` 컴포지트로 분리. `PlayerBrawler` 는 카탈로그 아이템을 `PlayerBrawlerItem<T>` 컬렉션으로 보유. 응답에서 `BrawlerResponse` 는 가벼운 메타만 가지며 단건 상세는 신규 `BrawlerDetailResponse` 로 응답. 같은 PR 로 backend + frontend 동시 배포.

**Tech Stack:** Kotlin + Spring Boot 4 (core-api), Jackson 직렬화, JPA, Next.js + TypeScript + React Query, Vitest, Tailwind.

**Spec:** `docs/superpowers/specs/2026-05-02-player-brawler-items-design.md`

---

## 작업 환경 가정

- 작업 디렉터리: `/Users/chodyoungrae/Projects/imstargg-v2`
- 브랜치: 현재 `dev` (이번 작업 전용으로 분리되어 있음). 필요 시 feature 브랜치로 추가 분리는 사용자 요청 후.
- 백엔드: `imstargg-backend/`. 명령은 `cd imstargg-backend && ./gradlew ...`.
- 프론트엔드: `imstargg-frontend/`. 명령은 `cd imstargg-frontend && pnpm ...`.

---

## Task 1: 백엔드 도메인 — 카탈로그 분리

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/brawler/BrawlerItems.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/brawler/BrawlerDetail.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/brawler/Brawler.kt`

이 task 직후엔 `Brawler` 사용처가 컴파일 깨진다. Task 2 까지 완료해야 컴파일 통과한다. 그래서 Task 1+2 를 한 commit 단위로 묶는다.

- [ ] **Step 1: BrawlerItems.kt 생성**

```kotlin
package com.imstargg.core.domain.brawlstars.brawler

data class BrawlerItems(
    val gadgets: List<Gadget>,
    val gears: List<Gear>,
    val starPowers: List<StarPower>,
    val hyperCharge: HyperCharge?,
    val buffies: Buffies?,
)
```

- [ ] **Step 2: BrawlerDetail.kt 생성**

```kotlin
package com.imstargg.core.domain.brawlstars.brawler

data class BrawlerDetail(
    val brawler: Brawler,
    val items: BrawlerItems,
)
```

- [ ] **Step 3: Brawler.kt 메타화**

```kotlin
package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class Brawler(
    val id: BrawlStarsId,
    val name: Messages,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val portraitImageUrl: String?,
)
```

(Task 2 까지 완료 후 컴파일 검증.)

---

## Task 2: 백엔드 도메인 — PlayerBrawler wrapper

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/player/PlayerBrawlerItem.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/player/PlayerBrawlerBuffies.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/player/PlayerBrawler.kt`

- [ ] **Step 1: PlayerBrawlerItem.kt 생성**

```kotlin
package com.imstargg.core.domain.brawlstars.player

data class PlayerBrawlerItem<T>(
    val item: T,
    val owned: Boolean,
)
```

- [ ] **Step 2: PlayerBrawlerBuffies.kt 생성**

```kotlin
package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.brawler.Buffy

data class PlayerBrawlerBuffies(
    val gadget: PlayerBrawlerItem<Buffy>,
    val starPower: PlayerBrawlerItem<Buffy>,
    val hyperCharge: PlayerBrawlerItem<Buffy>,
)
```

- [ ] **Step 3: PlayerBrawler.kt 변경**

```kotlin
package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.Gadget
import com.imstargg.core.domain.brawlstars.brawler.Gear
import com.imstargg.core.domain.brawlstars.brawler.HyperCharge
import com.imstargg.core.domain.brawlstars.brawler.StarPower

data class PlayerBrawler(
    val brawler: Brawler?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val gadgets: List<PlayerBrawlerItem<Gadget>>,
    val gears: List<PlayerBrawlerItem<Gear>>,
    val starPowers: List<PlayerBrawlerItem<StarPower>>,
    val hyperCharge: PlayerBrawlerItem<HyperCharge>?,
    val buffies: PlayerBrawlerBuffies?,
)
```

(Task 3, 4 의 응답·매퍼 변경 직후 컴파일이 통과한다.)

---

## Task 3: 백엔드 응답 — BrawlerResponse 가벼움 + 신규 응답 4종

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlerResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlerItemsResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/BrawlerDetailResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/PlayerBrawlerItemResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/PlayerBrawlerBuffiesResponse.kt`

- [ ] **Step 1: BrawlerResponse.kt 가벼움**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class BrawlerResponse(
    val id: Long,
    val name: MessagesResponse,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val portraitImageUrl: String?,
) {
    companion object {
        fun of(brawler: Brawler) =
            BrawlerResponse(
                id = brawler.id.value,
                name = MessagesResponse.of(brawler.name),
                rarity = brawler.rarity,
                role = brawler.role,
                portraitImageUrl = brawler.portraitImageUrl,
            )
    }
}
```

- [ ] **Step 2: BrawlerItemsResponse.kt 생성**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems

data class BrawlerItemsResponse(
    val gadgets: List<GadgetResponse>,
    val gears: List<GearResponse>,
    val starPowers: List<StarPowerResponse>,
    val hyperCharge: HyperChargeResponse?,
    val buffies: BuffiesResponse?,
) {
    companion object {
        fun of(items: BrawlerItems) =
            BrawlerItemsResponse(
                gadgets = items.gadgets.map { GadgetResponse.of(it) },
                gears = items.gears.map { GearResponse.of(it) },
                starPowers = items.starPowers.map { StarPowerResponse.of(it) },
                hyperCharge = items.hyperCharge?.let { HyperChargeResponse.of(it) },
                buffies = items.buffies?.let { BuffiesResponse.of(it) },
            )
    }
}
```

- [ ] **Step 3: BrawlerDetailResponse.kt 생성**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.BrawlerDetail

data class BrawlerDetailResponse(
    val brawler: BrawlerResponse,
    val items: BrawlerItemsResponse,
) {
    companion object {
        fun of(detail: BrawlerDetail) =
            BrawlerDetailResponse(
                brawler = BrawlerResponse.of(detail.brawler),
                items = BrawlerItemsResponse.of(detail.items),
            )
    }
}
```

- [ ] **Step 4: PlayerBrawlerItemResponse.kt 생성**

```kotlin
package com.imstargg.core.api.controller.v1.response

data class PlayerBrawlerItemResponse<T>(
    val item: T,
    val owned: Boolean,
)
```

- [ ] **Step 5: PlayerBrawlerBuffiesResponse.kt 생성**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerBrawlerBuffies

data class PlayerBrawlerBuffiesResponse(
    val gadget: PlayerBrawlerItemResponse<BuffyResponse>,
    val starPower: PlayerBrawlerItemResponse<BuffyResponse>,
    val hyperCharge: PlayerBrawlerItemResponse<BuffyResponse>,
) {
    companion object {
        fun of(buffies: PlayerBrawlerBuffies) =
            PlayerBrawlerBuffiesResponse(
                gadget =
                    PlayerBrawlerItemResponse(
                        item = BuffyResponse(buffies.gadget.item.type, buffies.gadget.item.imageUrl),
                        owned = buffies.gadget.owned,
                    ),
                starPower =
                    PlayerBrawlerItemResponse(
                        item = BuffyResponse(buffies.starPower.item.type, buffies.starPower.item.imageUrl),
                        owned = buffies.starPower.owned,
                    ),
                hyperCharge =
                    PlayerBrawlerItemResponse(
                        item = BuffyResponse(buffies.hyperCharge.item.type, buffies.hyperCharge.item.imageUrl),
                        owned = buffies.hyperCharge.owned,
                    ),
            )
    }
}
```

- [ ] **Step 6: PlayerBrawlerResponse.kt 수정**

```kotlin
package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerBrawler

data class PlayerBrawlerResponse(
    val brawler: BrawlerResponse?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val gadgets: List<PlayerBrawlerItemResponse<GadgetResponse>>,
    val gears: List<PlayerBrawlerItemResponse<GearResponse>>,
    val starPowers: List<PlayerBrawlerItemResponse<StarPowerResponse>>,
    val hyperCharge: PlayerBrawlerItemResponse<HyperChargeResponse>?,
    val buffies: PlayerBrawlerBuffiesResponse?,
) {
    companion object {
        fun of(pb: PlayerBrawler) =
            PlayerBrawlerResponse(
                brawler = pb.brawler?.let { BrawlerResponse.of(it) },
                power = pb.power,
                rank = pb.rank,
                trophies = pb.trophies,
                highestTrophies = pb.highestTrophies,
                prestigeLevel = pb.prestigeLevel,
                gadgets = pb.gadgets.map { PlayerBrawlerItemResponse(GadgetResponse.of(it.item), it.owned) },
                gears = pb.gears.map { PlayerBrawlerItemResponse(GearResponse.of(it.item), it.owned) },
                starPowers = pb.starPowers.map { PlayerBrawlerItemResponse(StarPowerResponse.of(it.item), it.owned) },
                hyperCharge = pb.hyperCharge?.let { PlayerBrawlerItemResponse(HyperChargeResponse.of(it.item), it.owned) },
                buffies = pb.buffies?.let { PlayerBrawlerBuffiesResponse.of(it) },
            )
    }
}
```

---

## Task 4: 백엔드 매핑 — BrawlerRepository / Cache / Service / Controller

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BrawlerRepository.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BrawlerRepositoryWithCache.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BrawlerService.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/BrawlerController.kt`

- [ ] **Step 1: BrawlerRepository.kt 의 findAll 분리 + findAllBrawlerItems 추가**

전체 클래스 변경 후 모습:

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CloudFrontProperties
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.domain.brawlstars.brawler.Buffies
import com.imstargg.core.domain.brawlstars.brawler.Buffy
import com.imstargg.core.domain.brawlstars.brawler.Gadget
import com.imstargg.core.domain.brawlstars.brawler.Gear
import com.imstargg.core.domain.brawlstars.brawler.HyperCharge
import com.imstargg.core.domain.brawlstars.brawler.StarPower
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.HyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class BrawlerRepository(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val buffyJpaRepository: BuffyJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val brawlStarsImageJpaRepository: BrawlStarsImageJpaRepository,
    private val cloudFrontProperties: CloudFrontProperties,
) {
    fun findAll(): List<Brawler> {
        val brawlerEntities = brawlerJpaRepository.findAll().toList()
        val brawlerIds = brawlerEntities.map { it.brawlStarsId }
        val brawlerNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, brawlerIds)
                .groupBy { it.brawlStarsId }
        val portraits =
            brawlStarsImageJpaRepository
                .findAll()
                .filter { it.type == BrawlStarsImageType.BRAWLER_PORTRAIT }
                .associateBy { it.brawlStarsId }

        return brawlerEntities.map { entity ->
            Brawler(
                id = BrawlStarsId(entity.brawlStarsId),
                name = toMessages(brawlerNames[entity.brawlStarsId]),
                rarity = entity.rarity,
                role = entity.role,
                portraitImageUrl = portraits[entity.brawlStarsId]?.toImageUrl(),
            )
        }
    }

    fun findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems> {
        val brawlerEntities = brawlerJpaRepository.findAll().toList()
        val gadgetEntities = gadgetJpaRepository.findAll().toList()
        val starPowerEntities = starPowerJpaRepository.findAll().toList()
        val gearEntities = gearJpaRepository.findAll().toList()
        val hyperChargeEntities = hyperChargeJpaRepository.findAll().toList()
        val brawlerGearEntities = brawlerGearJpaRepository.findAll().toList()

        val brawlerIds = brawlerEntities.map { it.brawlStarsId }
        val gadgetIds = gadgetEntities.map { it.brawlStarsId }
        val starPowerIds = starPowerEntities.map { it.brawlStarsId }
        val gearIds = gearEntities.map { it.brawlStarsId }
        val hyperChargeIds = hyperChargeEntities.map { it.brawlStarsId }

        val gadgetNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GADGET_NAME, gadgetIds)
                .groupBy { it.brawlStarsId }
        val starPowerNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, starPowerIds)
                .groupBy { it.brawlStarsId }
        val gearNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.GEAR_NAME, gearIds)
                .groupBy { it.brawlStarsId }
        val hyperChargeNames =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.HYPERCHARGE_NAME, hyperChargeIds)
                .groupBy { it.brawlStarsId }

        val imagesByKey = brawlStarsImageJpaRepository.findAll().associateBy { it.type to it.brawlStarsId }

        val gadgetsByBrawlerId = gadgetEntities.groupBy { it.brawlerBrawlStarsId }
        val starPowersByBrawlerId = starPowerEntities.groupBy { it.brawlerBrawlStarsId }
        val hyperChargesByBrawlerId = hyperChargeEntities.groupBy { it.brawlerBrawlStarsId }
        val gearsByBrawlerId = brawlerGearEntities.groupBy { it.brawlerBrawlStarsId }
        val gearsById = gearEntities.associateBy { it.brawlStarsId }
        val buffiesByBrawlerId =
            buffyJpaRepository
                .findAllByBrawlerBrawlStarsIdIn(brawlerIds)
                .groupBy { it.brawlerBrawlStarsId }

        return brawlerEntities.associate { brawlerEntity ->
            val brawlerId = brawlerEntity.brawlStarsId

            val buffiesByType =
                (buffiesByBrawlerId[brawlerId] ?: emptyList()).associate { buffyEntity ->
                    buffyEntity.type to
                        Buffy(
                            type = buffyEntity.type,
                            imageUrl = imagesByKey[buffyEntity.type.toImageType() to brawlerId]?.toImageUrl(),
                        )
                }
            val buffies =
                buffiesByType[BuffyType.GADGET]?.let { gadget ->
                    val starPower = buffiesByType[BuffyType.STAR_POWER] ?: return@let null
                    val hyperCharge = buffiesByType[BuffyType.HYPER_CHARGE] ?: return@let null
                    Buffies(gadget = gadget, starPower = starPower, hyperCharge = hyperCharge)
                }

            BrawlStarsId(brawlerId) to
                BrawlerItems(
                    gadgets =
                        (gadgetsByBrawlerId[brawlerId] ?: emptyList()).map { toGadget(it, gadgetNames, imagesByKey) },
                    gears =
                        (gearsByBrawlerId[brawlerId] ?: emptyList())
                            .mapNotNull { gearsById[it.gearBrawlStarsId] }
                            .map { toGear(it, gearNames, imagesByKey) },
                    starPowers =
                        (starPowersByBrawlerId[brawlerId] ?: emptyList()).map { toStarPower(it, starPowerNames, imagesByKey) },
                    hyperCharge =
                        (hyperChargesByBrawlerId[brawlerId] ?: emptyList())
                            .firstOrNull()
                            ?.let { toHyperCharge(it, hyperChargeNames, imagesByKey) },
                    buffies = buffies,
                )
        }
    }

    private fun BuffyType.toImageType(): BrawlStarsImageType =
        when (this) {
            BuffyType.GADGET -> BrawlStarsImageType.BUFFY_GADGET
            BuffyType.STAR_POWER -> BrawlStarsImageType.BUFFY_STAR_POWER
            BuffyType.HYPER_CHARGE -> BrawlStarsImageType.BUFFY_HYPER_CHARGE
        }

    private fun toMessages(entities: List<MessageEntity>?): Messages {
        if (entities.isNullOrEmpty()) return Messages(emptyMap())
        return Messages.create(entities.map { Message(it.lang, it.content) })
    }

    private fun toGadget(
        entity: GadgetEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): Gadget =
        Gadget(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.GADGET to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun toGear(
        entity: GearEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): Gear =
        Gear(
            id = BrawlStarsId(entity.brawlStarsId),
            rarity = entity.rarity,
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.GEAR to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun toStarPower(
        entity: StarPowerEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): StarPower =
        StarPower(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.STARPOWER to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun toHyperCharge(
        entity: HyperChargeEntity,
        names: Map<Long, List<MessageEntity>>,
        images: Map<Pair<BrawlStarsImageType, Long>, BrawlStarsImageEntity>,
    ): HyperCharge =
        HyperCharge(
            id = BrawlStarsId(entity.brawlStarsId),
            name = toMessages(names[entity.brawlStarsId]),
            imageUrl = images[BrawlStarsImageType.HYPERCHARGE to entity.brawlStarsId]?.toImageUrl(),
        )

    private fun BrawlStarsImageEntity.toImageUrl(): String = "${cloudFrontProperties.baseUrl}/$storedName"
}
```

- [ ] **Step 2: BrawlerRepositoryWithCache.kt 변경**

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository

@Repository
class BrawlerRepositoryWithCache(
    private val brawlerRepository: BrawlerRepository,
) {
    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v2:brawlers'")
    fun findBrawlers(): Brawlers = Brawlers(findAllBrawlers())

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v2:brawlerList'")
    fun findAllBrawlers(): List<Brawler> =
        brawlerRepository
            .findAll()
            .sortedBy { it.id.value }

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v2:brawlerItems'")
    fun findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems> = brawlerRepository.findAllBrawlerItems()
}
```

- [ ] **Step 3: BrawlerService.kt 변경**

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerDetail
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class BrawlerService(
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun get(id: BrawlStarsId): Brawler =
        brawlerRepositoryWithCache.findAllBrawlers().find { it.id == id }
            ?: throw CoreException(ErrorType.NOT_FOUND, "id=$id")

    fun getAll(): List<Brawler> = brawlerRepositoryWithCache.findAllBrawlers()

    fun getDetail(id: BrawlStarsId): BrawlerDetail {
        val brawler = get(id)
        val items =
            brawlerRepositoryWithCache.findAllBrawlerItems()[id]
                ?: BrawlerItems(emptyList(), emptyList(), emptyList(), null, null)
        return BrawlerDetail(brawler, items)
    }

    fun search(name: String): List<Brawler> {
        val brawlers = brawlerRepositoryWithCache.findAllBrawlers()
        if (name.isBlank()) return brawlers
        return brawlers.filter { it.name.containsIgnoreCase(name) }
    }
}
```

- [ ] **Step 4: BrawlerController.kt 의 단건 응답 타입 변경**

```kotlin
package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerDetailResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class BrawlerController(
    private val brawlerService: BrawlerService,
) {
    @GetMapping("/v1/brawlers")
    fun getBrawlers(): ResponseEntity<ListResponse<BrawlerResponse>> {
        val brawlers = brawlerService.getAll()
        return ResponseEntity.ok(
            ListResponse(brawlers.map { BrawlerResponse.of(it) }),
        )
    }

    @GetMapping("/v1/brawlers/{id}")
    fun getBrawler(
        @PathVariable id: Long,
    ): ResponseEntity<BrawlerDetailResponse> {
        val detail = brawlerService.getDetail(BrawlStarsId(id))
        return ResponseEntity.ok(BrawlerDetailResponse.of(detail))
    }

    @GetMapping("/v1/brawler/search")
    fun searchBrawlers(
        @RequestParam name: String,
    ): ResponseEntity<ListResponse<BrawlerResponse>> {
        val brawlers = brawlerService.search(name)
        return ResponseEntity.ok(
            ListResponse(brawlers.map { BrawlerResponse.of(it) }),
        )
    }
}
```

---

## Task 5: 백엔드 매핑 — PlayerRepository 보유 정보 매핑

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerRepository.kt`

`PlayerBrawlerGadget/Gear/StarPower/HyperChargeJpaRepository` 4종은 이미 존재 (`findAllByPlayerBrawlerIn` 메서드 보유 확인 완료).

- [ ] **Step 1: PlayerRepository.kt 변경**

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.domain.brawlstars.player.PlayerBrawlerBuffies
import com.imstargg.core.domain.brawlstars.player.PlayerBrawlerItem
import com.imstargg.core.domain.brawlstars.player.PlayerClub
import com.imstargg.core.enums.BuffyType
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class PlayerRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val playerBrawlerJpaRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGadgetJpaRepository: PlayerBrawlerGadgetJpaRepository,
    private val playerBrawlerGearJpaRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerStarPowerJpaRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerHyperChargeJpaRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val playerBrawlerBuffyJpaRepository: PlayerBrawlerBuffyJpaRepository,
    private val brawlerRepositoryWithCache: BrawlerRepositoryWithCache,
) {
    fun isDeleted(tag: BrawlStarsTag): Boolean {
        val entity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return false
        return entity.status == PlayerStatus.DELETED
    }

    fun find(tag: BrawlStarsTag): Player? {
        val entity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return null
        return toPlayer(entity)
    }

    fun findAllPlayerBrawler(tag: BrawlStarsTag): List<PlayerBrawler> {
        val playerEntity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return emptyList()
        val pbEntities = playerBrawlerJpaRepository.findAllByPlayer(playerEntity)
        if (pbEntities.isEmpty()) return emptyList()

        val brawlers = brawlerRepositoryWithCache.findBrawlers()
        val itemsByBrawlerId = brawlerRepositoryWithCache.findAllBrawlerItems()

        val gadgetsByPbId =
            playerBrawlerGadgetJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.gadgetBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val gearsByPbId =
            playerBrawlerGearJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.gearBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val starPowersByPbId =
            playerBrawlerStarPowerJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.starPowerBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val hyperChargesByPbId =
            playerBrawlerHyperChargeJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.hyperChargeBrawlStarsId })
                .mapValues { (_, ids) -> ids.toSet() }
        val buffiesByPbId =
            playerBrawlerBuffyJpaRepository
                .findAllByPlayerBrawlerIn(pbEntities)
                .groupBy({ it.playerBrawler.id!! }, { it.buffyType })
                .mapValues { (_, types) -> types.toSet() }

        return pbEntities.map { entity ->
            val brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId)
            toPlayerBrawler(
                entity = entity,
                brawler = brawlers.find(brawlerId),
                items = itemsByBrawlerId[brawlerId],
                ownedGadgetIds = gadgetsByPbId[entity.id!!] ?: emptySet(),
                ownedGearIds = gearsByPbId[entity.id!!] ?: emptySet(),
                ownedStarPowerIds = starPowersByPbId[entity.id!!] ?: emptySet(),
                ownedHyperChargeIds = hyperChargesByPbId[entity.id!!] ?: emptySet(),
                ownedBuffyTypes = buffiesByPbId[entity.id!!] ?: emptySet(),
            )
        }
    }

    private fun toPlayer(entity: PlayerEntity): Player =
        Player(
            tag = BrawlStarsTag(entity.brawlStarsTag),
            name = entity.name,
            nameColor = entity.nameColor,
            iconId = BrawlStarsId(entity.iconBrawlStarsId),
            trophies = entity.trophies,
            highestTrophies = entity.highestTrophies,
            expLevel = entity.expLevel,
            expPoints = entity.expPoints,
            qualifiedFromChampionshipChallenge = entity.qualifiedFromChampionshipChallenge,
            victories3vs3 = entity.victories3vs3,
            victoriesSolo = entity.victoriesSolo,
            victoriesDuo = entity.victoriesDuo,
            bestRoboRumbleTime = entity.bestRoboRumbleTime,
            bestTimeAsBigBrawler = entity.bestTimeAsBigBrawler,
            totalPrestigeLevel = entity.totalPrestigeLevel,
            club = entity.club?.let { PlayerClub(BrawlStarsTag(it.brawlStarsTag), it.name) },
        )

    private fun toPlayerBrawler(
        entity: PlayerBrawlerEntity,
        brawler: Brawler?,
        items: BrawlerItems?,
        ownedGadgetIds: Set<Long>,
        ownedGearIds: Set<Long>,
        ownedStarPowerIds: Set<Long>,
        ownedHyperChargeIds: Set<Long>,
        ownedBuffyTypes: Set<BuffyType>,
    ): PlayerBrawler {
        if (brawler == null || items == null) {
            return PlayerBrawler(
                brawler = brawler,
                power = entity.power,
                rank = entity.rank,
                trophies = entity.trophies,
                highestTrophies = entity.highestTrophies,
                prestigeLevel = entity.prestigeLevel,
                gadgets = emptyList(),
                gears = emptyList(),
                starPowers = emptyList(),
                hyperCharge = null,
                buffies = null,
            )
        }
        return PlayerBrawler(
            brawler = brawler,
            power = entity.power,
            rank = entity.rank,
            trophies = entity.trophies,
            highestTrophies = entity.highestTrophies,
            prestigeLevel = entity.prestigeLevel,
            gadgets = items.gadgets.map { PlayerBrawlerItem(it, it.id.value in ownedGadgetIds) },
            gears = items.gears.map { PlayerBrawlerItem(it, it.id.value in ownedGearIds) },
            starPowers = items.starPowers.map { PlayerBrawlerItem(it, it.id.value in ownedStarPowerIds) },
            hyperCharge = items.hyperCharge?.let { PlayerBrawlerItem(it, it.id.value in ownedHyperChargeIds) },
            buffies =
                items.buffies?.let {
                    PlayerBrawlerBuffies(
                        gadget = PlayerBrawlerItem(it.gadget, BuffyType.GADGET in ownedBuffyTypes),
                        starPower = PlayerBrawlerItem(it.starPower, BuffyType.STAR_POWER in ownedBuffyTypes),
                        hyperCharge = PlayerBrawlerItem(it.hyperCharge, BuffyType.HYPER_CHARGE in ownedBuffyTypes),
                    )
                },
        )
    }
}
```

---

## Task 6: 백엔드 검증 + 커밋

- [ ] **Step 1: compileKotlin**

```bash
cd imstargg-backend && ./gradlew :core:core-api:compileKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: ktlintCheck**

```bash
cd imstargg-backend && ./gradlew :core:core-api:ktlintCheck
```
Expected: `BUILD SUCCESSFUL`. 위반 시 `./gradlew :core:core-api:ktlintFormat` 후 재실행.

- [ ] **Step 3: 단위 테스트**

```bash
cd imstargg-backend && ./gradlew :core:core-api:unitTest
```
Expected: `BUILD SUCCESSFUL`. 실패한 테스트가 있으면 — `Brawler` 의 5종 아이템 필드, `PlayerBrawler.buffies: Set<BuffyType>` 직접 접근 등을 가정한 픽스처/검증부일 것. 새 모델 모양에 맞게 수정 필요. 픽스처 변경 예시:

  - `Brawler(...)` 호출부에서 `gadgets/gears/starPowers/hyperCharge/buffies/portraitImageUrl` 인자를 `Brawler(id, name, rarity, role, portraitImageUrl)` 시그니처에 맞게 정리
  - `PlayerBrawler(buffies = setOf(...))` 같은 호출을 새 시그니처(`gadgets, gears, starPowers, hyperCharge, buffies: PlayerBrawlerBuffies?`) 에 맞게 변경
  - 응답 검증 테스트 — `BrawlerResponse` 가 더 이상 `gadgets` 필드를 갖지 않으니 assertion 정리

- [ ] **Step 4: 백엔드 변경 단일 commit**

```bash
git add imstargg-backend
git commit -m "$(cat <<'EOF'
refactor(core-api): Brawler 카탈로그 분리 + PlayerBrawler 보유 모델링

- Brawler 메타와 BrawlerItems 분리, BrawlerDetail 컴포지트 도입
- PlayerBrawlerItem<T>(item, owned) wrapper + PlayerBrawlerBuffies
- BrawlerResponse 메타만, BrawlerDetailResponse/BrawlerItemsResponse 신규
- PlayerBrawlerResponse 가 5종 보유 wrapper 컬렉션 노출
- BrawlerRepository.findAll/findAllBrawlerItems 분리, 캐시 키 v2 로 갱신
- PlayerRepository 가 4 종 보유 엔티티를 조회해 owned 매칭

Refs: docs/superpowers/specs/2026-05-02-player-brawler-items-design.md
EOF
)"
```

---

## Task 7: 프론트엔드 — api/types.ts

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts`

기존 types 의 `BrawlerResponse`, `PlayerBrawlerResponse`, `BuffiesResponse` 정의 부분을 갱신한다.

- [ ] **Step 1: types.ts 의 Brawler 관련 타입 변경**

`imstargg-frontend/src/api/types.ts` 안에서 다음 블록을 찾아 교체:

기존(예시):
```ts
export interface BrawlerResponse {
  id: number;
  name: MessagesResponse;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  gadgets: GadgetResponse[];
  gears: GearResponse[];
  starPowers: StarPowerResponse[];
  hyperCharge: HyperChargeResponse | null;
  portraitImageUrl: string | null;
  buffies: BuffiesResponse | null;
}

export interface PlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  gadgetBuffy: BuffyResponse | null;
  starPowerBuffy: BuffyResponse | null;
  hyperChargeBuffy: BuffyResponse | null;
}
```

신규 모양:
```ts
export interface BrawlerResponse {
  id: number;
  name: MessagesResponse;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  portraitImageUrl: string | null;
}

export interface BrawlerItemsResponse {
  gadgets: GadgetResponse[];
  gears: GearResponse[];
  starPowers: StarPowerResponse[];
  hyperCharge: HyperChargeResponse | null;
  buffies: BuffiesResponse | null;
}

export interface BrawlerDetailResponse {
  brawler: BrawlerResponse;
  items: BrawlerItemsResponse;
}

export interface PlayerBrawlerItemResponse<T> {
  item: T;
  owned: boolean;
}

export interface PlayerBrawlerBuffiesResponse {
  gadget: PlayerBrawlerItemResponse<BuffyResponse>;
  starPower: PlayerBrawlerItemResponse<BuffyResponse>;
  hyperCharge: PlayerBrawlerItemResponse<BuffyResponse>;
}

export interface PlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  gadgets: PlayerBrawlerItemResponse<GadgetResponse>[];
  gears: PlayerBrawlerItemResponse<GearResponse>[];
  starPowers: PlayerBrawlerItemResponse<StarPowerResponse>[];
  hyperCharge: PlayerBrawlerItemResponse<HyperChargeResponse> | null;
  buffies: PlayerBrawlerBuffiesResponse | null;
}
```

`BuffiesResponse` 인터페이스는 이미 존재하므로 그대로 유지.

- [ ] **Step 2: typecheck (의도적 일시 실패)**

```bash
cd imstargg-frontend && pnpm typecheck
```
Expected: 에러 다수 — `mappers.ts`, `BrawlerItems.tsx`, `PlayerBrawlerCard` 등에서 새 타입과 안 맞음. 다음 task 들에서 수정.

---

## Task 8: 프론트엔드 — domain/brawler types & mappers

**Files:**
- Modify: `imstargg-frontend/src/domain/brawler/types.ts`
- Modify: `imstargg-frontend/src/domain/brawler/mappers.ts`

- [ ] **Step 1: types.ts 변경**

```ts
import type { BrawlerRarity, BrawlerRole, BuffyResponse } from "@/api/types";

export interface BrawlerItem {
  id: number;
  name: string;
  imageUrl: string | null;
}

export interface Buffies {
  gadget: BuffyResponse;
  starPower: BuffyResponse;
  hyperCharge: BuffyResponse;
}

export interface Brawler {
  id: number;
  name: string;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  portraitImageUrl: string | null;
}

export interface BrawlerItems {
  gadgets: BrawlerItem[];
  gears: BrawlerItem[];
  starPowers: BrawlerItem[];
  hyperCharge: BrawlerItem | null;
  buffies: Buffies | null;
}

export interface BrawlerDetail {
  brawler: Brawler;
  items: BrawlerItems;
}
```

- [ ] **Step 2: mappers.ts 변경**

```ts
import type {
  BrawlerResponse,
  BrawlerItemsResponse,
  BrawlerDetailResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type {
  Brawler,
  BrawlerItem,
  BrawlerItems,
  BrawlerDetail,
} from "@/domain/brawler/types";
import { pickName } from "@/domain/i18n";

type ItemDto =
  | GadgetResponse
  | GearResponse
  | StarPowerResponse
  | HyperChargeResponse;

export function toBrawlerItem(dto: ItemDto): BrawlerItem {
  return {
    id: dto.id,
    name: pickName(dto.name),
    imageUrl: dto.imageUrl,
  };
}

export function toBrawler(dto: BrawlerResponse): Brawler {
  return {
    id: dto.id,
    name: pickName(dto.name),
    rarity: dto.rarity,
    role: dto.role,
    portraitImageUrl: dto.portraitImageUrl,
  };
}

export function toBrawlerItems(dto: BrawlerItemsResponse): BrawlerItems {
  return {
    gadgets: dto.gadgets.map(toBrawlerItem),
    gears: dto.gears.map(toBrawlerItem),
    starPowers: dto.starPowers.map(toBrawlerItem),
    hyperCharge: dto.hyperCharge ? toBrawlerItem(dto.hyperCharge) : null,
    buffies: dto.buffies,
  };
}

export function toBrawlerDetail(dto: BrawlerDetailResponse): BrawlerDetail {
  return {
    brawler: toBrawler(dto.brawler),
    items: toBrawlerItems(dto.items),
  };
}
```

---

## Task 9: 프론트엔드 — domain/player types & mappers

**Files:**
- Modify: `imstargg-frontend/src/domain/player/types.ts`
- Modify: `imstargg-frontend/src/domain/player/mappers.ts`

- [ ] **Step 1: types.ts 변경**

```ts
import type { BrawlerRarity, BrawlerRole, BuffyResponse } from "@/api/types";
import type { Brawler, BrawlerItem } from "@/domain/brawler/types";

export interface PlayerClub {
  tag: string;
  name: string;
}

export interface Player {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  victories3vs3: number;
  victoriesSolo: number;
  victoriesDuo: number;
  totalPrestigeLevel: number;
  club: PlayerClub | null;
}

export interface PlayerBrawlerOwnedItem {
  item: BrawlerItem;
  owned: boolean;
}

export interface PlayerBrawlerOwnedBuffy {
  item: BuffyResponse;
  owned: boolean;
}

export interface PlayerBrawlerBuffies {
  gadget: PlayerBrawlerOwnedBuffy;
  starPower: PlayerBrawlerOwnedBuffy;
  hyperCharge: PlayerBrawlerOwnedBuffy;
}

export interface PlayerBrawler {
  brawler: Brawler | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  gadgets: PlayerBrawlerOwnedItem[];
  gears: PlayerBrawlerOwnedItem[];
  starPowers: PlayerBrawlerOwnedItem[];
  hyperCharge: PlayerBrawlerOwnedItem | null;
  buffies: PlayerBrawlerBuffies | null;
}
```

- [ ] **Step 2: mappers.ts 변경**

```ts
import type {
  PlayerResponse,
  PlayerBrawlerResponse,
  PlayerBrawlerItemResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type {
  Player,
  PlayerBrawler,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";
import { toBrawler, toBrawlerItem } from "@/domain/brawler/mappers";

type AnyItemDto =
  | GadgetResponse
  | GearResponse
  | StarPowerResponse
  | HyperChargeResponse;

function toOwnedItem(
  dto: PlayerBrawlerItemResponse<AnyItemDto>,
): PlayerBrawlerOwnedItem {
  return { item: toBrawlerItem(dto.item), owned: dto.owned };
}

export function toPlayer(dto: PlayerResponse): Player {
  return {
    tag: dto.tag,
    name: dto.name,
    nameColor: dto.nameColor,
    iconId: dto.iconId,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    expLevel: dto.expLevel,
    expPoints: dto.expPoints,
    victories3vs3: dto.victories3vs3,
    victoriesSolo: dto.victoriesSolo,
    victoriesDuo: dto.victoriesDuo,
    totalPrestigeLevel: dto.totalPrestigeLevel,
    club: dto.club ? { tag: dto.club.tag, name: dto.club.name } : null,
  };
}

export function toPlayerBrawler(dto: PlayerBrawlerResponse): PlayerBrawler {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    power: dto.power,
    rank: dto.rank,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    prestigeLevel: dto.prestigeLevel,
    gadgets: dto.gadgets.map(toOwnedItem),
    gears: dto.gears.map(toOwnedItem),
    starPowers: dto.starPowers.map(toOwnedItem),
    hyperCharge: dto.hyperCharge ? toOwnedItem(dto.hyperCharge) : null,
    buffies: dto.buffies
      ? {
          gadget: { item: dto.buffies.gadget.item, owned: dto.buffies.gadget.owned },
          starPower: { item: dto.buffies.starPower.item, owned: dto.buffies.starPower.owned },
          hyperCharge: { item: dto.buffies.hyperCharge.item, owned: dto.buffies.hyperCharge.owned },
        }
      : null,
  };
}
```

---

## Task 10: 프론트엔드 — Brawler 단건 상세 흐름

**Files:**
- Modify: `imstargg-frontend/src/api/endpoints/brawler.ts`
- Modify: `imstargg-frontend/src/hooks/queries/useBrawler.ts`
- Modify: `imstargg-frontend/app/brawlers/[id]/page.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerItems.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerHero.tsx`
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/ItemOwnershipSection.tsx`

- [ ] **Step 1: api/endpoints/brawler.ts 변경**

`get(id)` 의 응답 타입만 `BrawlerDetailResponse` 로 변경. `list` 는 그대로.

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import type { BrawlerDetailResponse, BrawlerResponse, ListResponse } from "@/api/types";

export const brawlerApi = {
  list: (opts?: FetchOpts) =>
    apiFetch<ListResponse<BrawlerResponse>>(`/v1/brawlers`, opts),

  get: (id: number | string, opts?: FetchOpts) =>
    apiFetch<BrawlerDetailResponse>(`/v1/brawlers/${id}`, opts),
};
```

- [ ] **Step 2: useBrawler 훅 변경 (응답 타입을 BrawlerDetail 로)**

`src/hooks/queries/useBrawler.ts` 를 새 매퍼·도메인 타입으로 갱신. 훅 이름은 `useBrawler` 그대로 두되 반환 타입을 `BrawlerDetail` 로.

```ts
"use client";

import { useQuery } from "@tanstack/react-query";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawlerDetail } from "@/domain/brawler/mappers";
import type { BrawlerDetail } from "@/domain/brawler/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function useBrawler(
  id: number | string,
  options?: { initialData?: BrawlerDetail },
) {
  return useQuery({
    queryKey: queryKeys.brawler.detail(id),
    queryFn: ({ signal }) => brawlerApi.get(id, { signal }).then(toBrawlerDetail),
    initialData: options?.initialData,
    staleTime: 60 * 60_000,
  });
}
```

`queryKeys.brawler.detail(id)` 는 이미 존재 — 변경 불요.

- [ ] **Step 3: app/brawlers/[id]/page.tsx 변경**

서버 컴포넌트가 `BrawlerDetailResponse` 를 받도록.

```ts
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawlerDetail } from "@/domain/brawler/mappers";
import { BrawlerContent } from "./_components/BrawlerContent";
// ...

export default async function BrawlerPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const detail = toBrawlerDetail(await brawlerApi.get(Number(id)));
  return <BrawlerContent initialDetail={detail} />;
}

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const detail = toBrawlerDetail(await brawlerApi.get(Number(id)));
  const { brawler, items } = detail;
  const role = brawler.role;
  const rarity = brawler.rarity;
  const hcCount = items.hyperCharge ? 1 : 0;
  return {
    title: `${brawler.name} 통계 — ImStarGG`,
    description: `${brawler.name} — ${role} · ${rarity} · 가젯 ${items.gadgets.length} · 스타파워 ${items.starPowers.length} · 기어 ${items.gears.length} · 하이퍼차지 ${hcCount}.`,
  };
}
```

(기존 page.tsx 시그니처 — `params` Promise 방식·메타데이터 함수 — 그대로. detail 구조에만 맞춰 조정.)

- [ ] **Step 4: BrawlerContent.tsx 변경**

```tsx
"use client";

import type { BrawlerDetail } from "@/domain/brawler/types";
import { useBrawlerDetail } from "@/hooks/queries/useBrawler";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";
import { BrawlerBuffies } from "./BrawlerBuffies";

export function BrawlerContent({ initialDetail }: { initialDetail: BrawlerDetail }) {
  const { data: detail = initialDetail } = useBrawlerDetail(initialDetail.brawler.id, {
    initialData: initialDetail,
  });
  return (
    <section className="py-6 space-y-5">
      <BrawlerHero brawler={detail.brawler} />
      <BrawlerItems items={detail.items} />
      {detail.items.buffies && <BrawlerBuffies buffies={detail.items.buffies} />}
    </section>
  );
}
```

- [ ] **Step 5: BrawlerHero.tsx 변경 불요 (확인만)**

기존 `BrawlerHero` 는 `brawler.name/portraitImageUrl/rarity/role` 만 사용한다. 새 가벼운 `Brawler` 와 자연 호환 → **코드 변경 없음**. typecheck 단계에서 깨지지 않음.

- [ ] **Step 6: BrawlerItems.tsx 변경**

```tsx
"use client";

import type { BrawlerItems, BrawlerItem } from "@/domain/brawler/types";
import { BrawlerItemCard } from "./BrawlerItemCard";

function Section({ title, items }: { title: string; items: BrawlerItem[] }) {
  if (items.length === 0) return null;
  return (
    <section className="rounded-lg border bg-card p-4">
      <h2 className="mb-3 text-sm font-bold">
        {title} <span className="text-muted-foreground">({items.length})</span>
      </h2>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {items.map((it) => (
          <BrawlerItemCard key={it.id} item={it} />
        ))}
      </div>
    </section>
  );
}

export function BrawlerItems({ items }: { items: BrawlerItems }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Section title="가젯" items={items.gadgets} />
      <Section title="스타파워" items={items.starPowers} />
      <Section title="기어" items={items.gears} />
      <Section title="하이퍼차지" items={items.hyperCharge ? [items.hyperCharge] : []} />
    </div>
  );
}
```

- [ ] **Step 7: ItemOwnershipSection.tsx 변경**

prop `brawler: Brawler` 를 `items: BrawlerItems` 로 교체. `findItemName` 도 `items` 를 받도록.

```tsx
import type { BrawlerItems } from "@/domain/brawler/types";
import type { ItemOwnership, ItemOwnershipItem } from "@/domain/statistics/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";
import { TrophyRangeSelector } from "@app/_components/statistics/TrophyRangeSelector";

type Props = {
  items: BrawlerItems;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

function findItemName(
  items: BrawlerItems,
  key: "gears" | "gadgets" | "starPowers",
  itemId: number,
): string {
  return items[key].find((x) => x.id === itemId)?.name ?? `#${itemId}`;
}

// ItemRow, CategoryCard 함수는 변경 없음 — 그대로 유지

export function ItemOwnershipSection({ items, ownership, trophy }: Props) {
  const isAllEmpty =
    ownership.gears.length === 0 &&
    ownership.gadgets.length === 0 &&
    ownership.starPowers.length === 0 &&
    ownership.hyperCharges.length === 0 &&
    ownership.buffies.length === 0;

  return (
    <section className="space-y-3">
      <div className="flex items-end justify-between">
        <h3 className="text-sm font-bold">아이템 보유율</h3>
        <TrophyRangeSelector trophy={trophy} />
      </div>
      {isAllEmpty ? (
        <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
          보유율 데이터가 없습니다.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <CategoryCard
            title="기어"
            items={ownership.gears}
            resolveName={(id) => findItemName(items, "gears", id)}
          />
          <CategoryCard
            title="가젯"
            items={ownership.gadgets}
            resolveName={(id) => findItemName(items, "gadgets", id)}
          />
          <CategoryCard
            title="스타파워"
            items={ownership.starPowers}
            resolveName={(id) => findItemName(items, "starPowers", id)}
          />
          <CategoryCard
            title="하이퍼차지"
            items={ownership.hyperCharges}
            resolveName={(id) =>
              items.hyperCharge?.id === id ? items.hyperCharge.name : `#${id}`
            }
          />
          {ownership.buffies.length > 0 && (
            <div className="rounded-lg border border-[#D8DCE5] bg-white p-3">
              <h4 className="mb-2 text-xs font-bold uppercase tracking-wider text-[#5C6271]">버피</h4>
              {ownership.buffies.map((b) => (
                <ItemRow key={b.buffyType} name={buffyTypeLabel(b.buffyType)} rate={b.ownershipRate} />
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
```

(기존 파일의 `ItemRow`, `CategoryCard` 정의는 그대로 유지.)

- [ ] **Step 8: BrawlerStatsSection.tsx 호출부 변경**

`imstargg-frontend/app/brawlers/[id]/_components/BrawlerStatsSection.tsx` 에서 `<ItemOwnershipSection brawler={...} ... />` 가 `<ItemOwnershipSection items={...} ... />` 가 되도록 변경.

이 컴포넌트가 props 로 `brawler` 만 받는다면 호출 시점에서 `items` 를 함께 받도록 props 확장. 예:

기존(개념):
```tsx
export function BrawlerStatsSection({ brawler, ... }: { brawler: Brawler; ... }) {
  // ...
  return (
    <ItemOwnershipSection brawler={brawler} ownership={...} trophy={...} />
  );
}
```

변경:
```tsx
export function BrawlerStatsSection({ brawler, items, ... }: { brawler: Brawler; items: BrawlerItems; ... }) {
  // ...
  return (
    <ItemOwnershipSection items={items} ownership={...} trophy={...} />
  );
}
```

상위(`BrawlerContent.tsx`)에서 `<BrawlerStatsSection brawler={detail.brawler} items={detail.items} ... />` 형태로 호출. 만약 BrawlerStatsSection 이 BrawlerContent 가 아닌 다른 곳에서 렌더된다면 그곳에서도 동일하게 detail/brawler/items 분리. typecheck 결과를 보고 어디서든 사용처를 모두 정리.

---

## Task 11: 프론트엔드 — Player 페이지 보유 인디케이터 컴포넌트

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`
- Create: `imstargg-frontend/tests/components/PlayerBrawlerOwnership.test.tsx`

TDD: 인디케이터 컴포넌트는 새 추가라 테스트 먼저 작성 가능.

- [ ] **Step 1: 실패 테스트 작성**

`imstargg-frontend/tests/components/PlayerBrawlerOwnership.test.tsx`:

```tsx
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerBrawlerOwnership } from "@/../app/players/[tag]/_components/PlayerBrawlerOwnership";
import type { PlayerBrawler } from "@/domain/player/types";

const item = (id: number, owned: boolean) => ({
  item: { id, name: `i${id}`, imageUrl: null },
  owned,
});

const baseBrawler = (overrides: Partial<PlayerBrawler> = {}): PlayerBrawler => ({
  brawler: { id: 1, name: "Edgar", rarity: "EPIC", role: "ASSASSIN", portraitImageUrl: null },
  power: 11,
  rank: 25,
  trophies: 1234,
  highestTrophies: 1500,
  prestigeLevel: 0,
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharge: null,
  buffies: null,
  ...overrides,
});

describe("PlayerBrawlerOwnership", () => {
  it("가젯 보유 갯수와 전체 갯수를 ratio 로 표시한다", () => {
    const pb = baseBrawler({ gadgets: [item(1, true), item(2, false), item(3, true)] });
    render(<PlayerBrawlerOwnership pb={pb} />);
    expect(screen.getByLabelText("가젯 2/3")).toBeInTheDocument();
  });

  it("하이퍼차지 카탈로그가 있고 보유하면 '보유' 표기, 미보유면 '미보유' 표기", () => {
    const owned = baseBrawler({ hyperCharge: item(10, true) });
    const notOwned = baseBrawler({ hyperCharge: item(10, false) });
    const { rerender } = render(<PlayerBrawlerOwnership pb={owned} />);
    expect(screen.getByLabelText("하이퍼차지 보유")).toBeInTheDocument();
    rerender(<PlayerBrawlerOwnership pb={notOwned} />);
    expect(screen.getByLabelText("하이퍼차지 미보유")).toBeInTheDocument();
  });

  it("buffies 카탈로그가 null 이면 buffy row 자체가 렌더되지 않는다", () => {
    render(<PlayerBrawlerOwnership pb={baseBrawler({ buffies: null })} />);
    expect(screen.queryByLabelText(/버피/)).toBeNull();
  });

  it("buffies 가 있으면 3종 표시 (보유는 채움, 미보유는 흐림)", () => {
    const buffy = (owned: boolean) => ({
      item: { type: "GADGET" as const, imageUrl: null },
      owned,
    });
    const pb = baseBrawler({
      buffies: {
        gadget: buffy(true),
        starPower: buffy(false),
        hyperCharge: buffy(true),
      },
    });
    render(<PlayerBrawlerOwnership pb={pb} />);
    expect(screen.getByLabelText("버피 가젯 보유")).toBeInTheDocument();
    expect(screen.getByLabelText("버피 스타파워 미보유")).toBeInTheDocument();
    expect(screen.getByLabelText("버피 하이퍼차지 보유")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
cd imstargg-frontend && pnpm vitest run tests/components/PlayerBrawlerOwnership.test.tsx
```
Expected: FAIL — `Cannot find module ... PlayerBrawlerOwnership`.

- [ ] **Step 3: 컴포넌트 구현**

`imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`:

```tsx
"use client";

import type {
  PlayerBrawler,
  PlayerBrawlerBuffies,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";

function DotRow({
  label,
  items,
}: {
  label: string;
  items: PlayerBrawlerOwnedItem[];
}) {
  if (items.length === 0) return null;
  const ownedCount = items.filter((i) => i.owned).length;
  return (
    <div
      className="flex items-center gap-2 text-[11px] text-muted-foreground"
      aria-label={`${label} ${ownedCount}/${items.length}`}
    >
      <span className="w-12 shrink-0 font-semibold">{label}</span>
      <div className="flex gap-1">
        {items.map((it) => (
          <span
            key={it.item.id}
            className={
              it.owned
                ? "h-2 w-2 rounded-full bg-foreground"
                : "h-2 w-2 rounded-full border border-[#D8DCE5] bg-[#ECEEF3]"
            }
          />
        ))}
      </div>
      <span className="ml-auto tabular-nums">
        {ownedCount}/{items.length}
      </span>
    </div>
  );
}

function HyperChargeRow({
  hc,
}: {
  hc: PlayerBrawlerOwnedItem | null;
}) {
  if (!hc) return null;
  const ownedLabel = hc.owned ? "보유" : "미보유";
  return (
    <div
      className="flex items-center gap-2 text-[11px] text-muted-foreground"
      aria-label={`하이퍼차지 ${ownedLabel}`}
    >
      <span className="w-12 shrink-0 font-semibold">하이퍼차지</span>
      <span
        className={
          hc.owned
            ? "rounded bg-[#FFC60A]/20 px-1.5 py-0.5 text-[10px] font-bold text-[#14161B]"
            : "rounded border border-[#D8DCE5] px-1.5 py-0.5 text-[10px] font-bold text-[#8A90A0]"
        }
      >
        {hc.owned ? "✓" : "—"}
      </span>
    </div>
  );
}

function BuffyRow({ buffies }: { buffies: PlayerBrawlerBuffies }) {
  const cell = (label: string, owned: boolean) => (
    <span
      key={label}
      aria-label={`버피 ${label} ${owned ? "보유" : "미보유"}`}
      className={
        owned
          ? "rounded bg-[#14161B] px-1.5 py-0.5 text-[10px] font-bold text-white"
          : "rounded border border-[#D8DCE5] px-1.5 py-0.5 text-[10px] font-bold text-[#8A90A0]"
      }
    >
      {label}
    </span>
  );
  return (
    <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
      <span className="w-12 shrink-0 font-semibold">버피</span>
      <div className="flex gap-1">
        {cell("가젯", buffies.gadget.owned)}
        {cell("스타파워", buffies.starPower.owned)}
        {cell("하이퍼차지", buffies.hyperCharge.owned)}
      </div>
    </div>
  );
}

export function PlayerBrawlerOwnership({ pb }: { pb: PlayerBrawler }) {
  return (
    <div className="flex flex-col gap-1.5">
      <DotRow label="가젯" items={pb.gadgets} />
      <DotRow label="스타파워" items={pb.starPowers} />
      <DotRow label="기어" items={pb.gears} />
      <HyperChargeRow hc={pb.hyperCharge} />
      {pb.buffies && <BuffyRow buffies={pb.buffies} />}
    </div>
  );
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
cd imstargg-frontend && pnpm vitest run tests/components/PlayerBrawlerOwnership.test.tsx
```
Expected: PASS (4 tests).

---

## Task 12: 프론트엔드 — PlayerBrawlerCard + PlayerBrawlers 통합

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlerCard.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlers.tsx`

- [ ] **Step 1: PlayerBrawlerCard.tsx 생성**

```tsx
"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { Card } from "@/components/ui/card";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { formatNumber } from "@/lib/format";
import { PlayerBrawlerOwnership } from "./PlayerBrawlerOwnership";

export function PlayerBrawlerCard({ pb }: { pb: PlayerBrawler }) {
  return (
    <Card className="flex flex-col gap-2 p-3 text-sm">
      <div className="flex flex-col items-center gap-1 text-center">
        <BrawlerAvatar
          name={pb.brawler?.name ?? "?"}
          imageUrl={pb.brawler?.portraitImageUrl ?? null}
          rarity={pb.brawler?.rarity ?? null}
          size={80}
          showFrame
        />
        <p className="font-medium">{pb.brawler?.name ?? "Unknown"}</p>
        <p className="text-muted-foreground text-xs">
          파워 {pb.power} · 🏆 {formatNumber(pb.trophies)}
        </p>
      </div>
      <hr className="border-muted" />
      <PlayerBrawlerOwnership pb={pb} />
    </Card>
  );
}
```

- [ ] **Step 2: PlayerBrawlers.tsx 변경**

```tsx
"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { PlayerBrawlerCard } from "./PlayerBrawlerCard";

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  if (brawlers.length === 0) {
    return <p className="text-sm text-muted-foreground">보유한 브롤러가 없습니다.</p>;
  }
  const sorted = [...brawlers].sort((a, b) => b.trophies - a.trophies);
  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
      {sorted.map((pb) => (
        <li key={`${pb.brawler?.id ?? "x"}-${pb.brawler?.name ?? "x"}`}>
          <PlayerBrawlerCard pb={pb} />
        </li>
      ))}
    </ul>
  );
}
```

- [ ] **Step 3: BattlePlayerRow.tsx 호환성 확인**

`imstargg-frontend/app/players/[tag]/_components/battle/BattlePlayerRow.tsx` 가 `player.brawler.brawler` 의 `name/portraitImageUrl/rarity` 만 사용 (이미 가벼운 BrawlerResponse 와 호환). 변경 불필요.

---

## Task 13: 프론트엔드 검증 + 커밋

- [ ] **Step 1: typecheck**

```bash
cd imstargg-frontend && pnpm typecheck
```
Expected: 0 errors.

- [ ] **Step 2: lint**

```bash
cd imstargg-frontend && pnpm lint
```
Expected: 0 errors.

- [ ] **Step 3: 테스트 전체**

```bash
cd imstargg-frontend && pnpm test:run
```
Expected: 모든 테스트 PASS.

기존 테스트 중 `Brawler.gadgets` 등 직접 접근하던 픽스처가 있으면 새 모양에 맞춰 수정 (예: `domain/brawler/mappers.test.ts`, `domain/player/mappers.test.ts` 가 있을 경우 입력 dto 모양 갱신).

- [ ] **Step 4: 프론트엔드 변경 단일 commit**

```bash
git add imstargg-frontend
git commit -m "$(cat <<'EOF'
feat(frontend): Brawler 카탈로그 분리 + PlayerBrawler 보유 인디케이터

- BrawlerResponse 메타 + BrawlerDetailResponse 단건 상세 분리
- domain/brawler types/mappers 갱신 (Brawler/BrawlerItems/BrawlerDetail)
- domain/player types/mappers 가 PlayerBrawlerOwnedItem wrapper 사용
- BrawlerItems 컴포넌트가 items prop 받도록 변경
- ItemOwnershipSection 이 items 받도록 변경
- PlayerBrawlerCard / PlayerBrawlerOwnership 컴포넌트 신규
- 플레이어 페이지 카드에 가젯/스타파워/기어/하이퍼차지/버피 보유 인디케이터

Refs: docs/superpowers/specs/2026-05-02-player-brawler-items-design.md
EOF
)"
```

---

## Task 14: 최종 통합 검증

- [ ] **Step 1: 백엔드 전체 빌드**

```bash
cd imstargg-backend && ./gradlew :core:core-api:build -x integrationTest
```
Expected: `BUILD SUCCESSFUL`

(integrationTest 는 외부 인프라 필요 — 본 PR 범위 밖.)

- [ ] **Step 2: 프론트엔드 빌드**

```bash
cd imstargg-frontend && pnpm build
```
Expected: 빌드 성공.

- [ ] **Step 3: 로컬 통합 스모크**

`imstargg-frontend/.env.local` 의 백엔드 주소가 로컬 백엔드를 가리킨다고 가정. 두 프로세스 실행:

```bash
# Terminal A
cd imstargg-backend && ./gradlew :core:core-api:bootRun

# Terminal B
cd imstargg-frontend && pnpm dev
```

브라우저에서 다음 페이지를 한번씩 확인:
- `/brawlers/16000000` (또는 임의 id) — 가젯/스타파워/기어/하이퍼차지/버피가 표시되는지
- `/players/{tag}` — 카드 그리드에 보유 인디케이터 row 가 보이는지 (본인의 plt 태그 사용)
- 배틀/통계 페이지 — 페이지가 정상 로드되는지 (BrawlerResponse 가 가벼워졌어도 화면 영향 없음)

브라우저 콘솔에 에러/경고 없는지 확인.

E2E 자동화는 prod 백엔드 호출이라 새 응답 모양 배포 전엔 깨진다 — 실행하지 않음.

---

## 호환성 노트

같은 PR/배포 단위로 backend + frontend 동시 반영. Cache prefix 가 `v1:` → `v2:` 로 변경되어 운영 배포 시 한 번 cold load 가 발생. 통계 응답(`BrawlerItemOwnershipStatisticsResponse`)은 변경 대상 아님. 다른 외부 API 클라이언트가 없는지 PR 검토 시 재확인.
