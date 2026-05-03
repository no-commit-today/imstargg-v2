# Brawler 카탈로그 분리 + PlayerBrawler 아이템 보유 모델링 — Design

**Date:** 2026-05-02
**Scope:** core-api 백엔드 + imstargg-frontend
**Goal:** Brawler 카탈로그를 메타와 아이템(BrawlerItems)으로 분리하고, PlayerBrawler 가 카탈로그 아이템을 `PlayerBrawlerItem<T>(item, owned)` 로 감싸 보유 여부를 1급 개념으로 모델링한다. 모든 응답에 nested 되는 BrawlerResponse 가 가벼워지고, 플레이어 페이지에서 보유 여부를 시각화한다.

---

## 1. 배경 / 동기

현재 `Brawler` 도메인은 메타(id, name, rarity, role, portrait) 와 아이템 풀(gadgets, gears, starPowers, hyperCharge, buffies)을 한 클래스에 담고 있다. 그리고 `BrawlerResponse` 가 모든 곳—전투(`BattlePlayerBrawlerResponse`), 통계(`BrawlerResultStatisticsItemResponse`, `BrawlerRankStatisticsResponse`, `BrawlerEnemyResultStatisticsItemResponse`, `BrawlerPairRankStatisticsResponse`, `BrawlerPairResultStatisticsItemResponse`, `BrawlerResultDailyStatisticsResponse`, `PlayerBrawlerStatisticsItemResponse`), 랭킹(`BrawlerRankingResponse`, `PlayerBrawlerResponse`)—에 nested 되어 있어 페이로드가 부풀린다. 실제로 nested 위치 대부분은 `id/name/rarity/role/portraitImageUrl` 만 사용한다.

또한 `PlayerBrawler.buffies: Set<BuffyType>` 만 있고 가젯/기어/스타파워/하이퍼차지의 보유 여부는 도메인/응답에 노출되지 않는다 (DB 엔티티 `PlayerBrawler*Entity` 4종은 이미 적재되어 있음). 사용자가 자신의 브롤러별 아이템 보유 현황을 확인할 수 없다.

## 2. 설계 원칙

- 카탈로그 아이템 풀이 필요한 곳은 **브롤러 단건 상세 응답 한 곳**뿐이다. 그 외에는 메타만 노출한다.
- "보유 여부"는 도메인의 1급 개념이다. `PlayerBrawlerItem<T>(item, owned)` 로 wrapping 한다.
- Buffies 의 카탈로그 invariant("3종 모두 있거나 없거나")는 유지한다. 플레이어 보유는 각 buffy 독립.
- Backend ↔ Frontend 응답 모양은 같은 PR/배포 단위로 동시 변경 (직전 PR 들의 패턴).

## 3. 도메인 모델

### 3.1 카탈로그 분리

`com/imstargg/core/domain/brawlstars/brawler/Brawler.kt` (수정)

```kotlin
data class Brawler(
    val id: BrawlStarsId,
    val name: Messages,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val portraitImageUrl: String?,
)
```

`com/imstargg/core/domain/brawlstars/brawler/BrawlerItems.kt` (신규)

```kotlin
data class BrawlerItems(
    val gadgets: List<Gadget>,
    val gears: List<Gear>,
    val starPowers: List<StarPower>,
    val hyperCharge: HyperCharge?,
    val buffies: Buffies?,
)
```

`com/imstargg/core/domain/brawlstars/brawler/BrawlerDetail.kt` (신규)

```kotlin
data class BrawlerDetail(
    val brawler: Brawler,
    val items: BrawlerItems,
)
```

`Brawlers` 컬렉션은 그대로 유지 (`find(id): Brawler?`).

### 3.2 PlayerBrawler wrapper

`com/imstargg/core/domain/brawlstars/player/PlayerBrawlerItem.kt` (신규)

```kotlin
data class PlayerBrawlerItem<T>(
    val item: T,
    val owned: Boolean,
)
```

`com/imstargg/core/domain/brawlstars/player/PlayerBrawlerBuffies.kt` (신규)

```kotlin
data class PlayerBrawlerBuffies(
    val gadget: PlayerBrawlerItem<Buffy>,
    val starPower: PlayerBrawlerItem<Buffy>,
    val hyperCharge: PlayerBrawlerItem<Buffy>,
)
```

규칙: 카탈로그 `Buffies` 가 존재할 때만 `PlayerBrawlerBuffies` 가 생성된다. 부재(=브롤러가 buffy 자체가 없음) 시 `PlayerBrawler.buffies = null`. 각 buffy 의 `owned` 는 독립적.

### 3.3 PlayerBrawler

`com/imstargg/core/domain/brawlstars/player/PlayerBrawler.kt` (수정)

```kotlin
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

규칙:
- `brawler == null` (카탈로그 매핑 실패) → `gadgets/gears/starPowers = emptyList()`, `hyperCharge/buffies = null`. 보유 정보를 매칭할 카탈로그가 없는 상태.
- `brawler != null` → 카탈로그의 모든 아이템이 wrapper 로 표현되며, `owned` 는 DB `PlayerBrawler*Entity` 의 보유 ID/Type 집합과 매칭한 결과.

## 4. 응답 (API)

### 4.1 Brawler 메타 / 단건 상세 분리

`BrawlerResponse.kt` (수정 — 가벼움)

```kotlin
data class BrawlerResponse(
    val id: Long,
    val name: MessagesResponse,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val portraitImageUrl: String?,
) {
    companion object {
        fun of(brawler: Brawler) = BrawlerResponse(
            id = brawler.id.value,
            name = MessagesResponse.of(brawler.name),
            rarity = brawler.rarity,
            role = brawler.role,
            portraitImageUrl = brawler.portraitImageUrl,
        )
    }
}
```

`BrawlerItemsResponse.kt` (신규)

```kotlin
data class BrawlerItemsResponse(
    val gadgets: List<GadgetResponse>,
    val gears: List<GearResponse>,
    val starPowers: List<StarPowerResponse>,
    val hyperCharge: HyperChargeResponse?,
    val buffies: BuffiesResponse?,
) {
    companion object {
        fun of(items: BrawlerItems) = BrawlerItemsResponse(
            gadgets = items.gadgets.map { GadgetResponse.of(it) },
            gears = items.gears.map { GearResponse.of(it) },
            starPowers = items.starPowers.map { StarPowerResponse.of(it) },
            hyperCharge = items.hyperCharge?.let { HyperChargeResponse.of(it) },
            buffies = items.buffies?.let { BuffiesResponse.of(it) },
        )
    }
}
```

`BrawlerDetailResponse.kt` (신규)

```kotlin
data class BrawlerDetailResponse(
    val brawler: BrawlerResponse,
    val items: BrawlerItemsResponse,
) {
    companion object {
        fun of(detail: BrawlerDetail) = BrawlerDetailResponse(
            brawler = BrawlerResponse.of(detail.brawler),
            items = BrawlerItemsResponse.of(detail.items),
        )
    }
}
```

### 4.2 PlayerBrawler 응답

`PlayerBrawlerItemResponse.kt` (신규)

```kotlin
data class PlayerBrawlerItemResponse<T>(
    val item: T,
    val owned: Boolean,
)
```

`PlayerBrawlerBuffiesResponse.kt` (신규)

```kotlin
data class PlayerBrawlerBuffiesResponse(
    val gadget: PlayerBrawlerItemResponse<BuffyResponse>,
    val starPower: PlayerBrawlerItemResponse<BuffyResponse>,
    val hyperCharge: PlayerBrawlerItemResponse<BuffyResponse>,
) {
    companion object {
        fun of(buffies: PlayerBrawlerBuffies) = PlayerBrawlerBuffiesResponse(
            gadget = PlayerBrawlerItemResponse(
                item = BuffyResponse(buffies.gadget.item.type, buffies.gadget.item.imageUrl),
                owned = buffies.gadget.owned,
            ),
            starPower = PlayerBrawlerItemResponse(
                item = BuffyResponse(buffies.starPower.item.type, buffies.starPower.item.imageUrl),
                owned = buffies.starPower.owned,
            ),
            hyperCharge = PlayerBrawlerItemResponse(
                item = BuffyResponse(buffies.hyperCharge.item.type, buffies.hyperCharge.item.imageUrl),
                owned = buffies.hyperCharge.owned,
            ),
        )
    }
}
```

`PlayerBrawlerResponse.kt` (수정)

```kotlin
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
        fun of(pb: PlayerBrawler) = PlayerBrawlerResponse(
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

### 4.3 엔드포인트 변경

| 엔드포인트                      | 이전                          | 이후                              |
| ------------------------------ | ----------------------------- | --------------------------------- |
| `GET /v1/brawlers`             | `ListResponse<BrawlerResponse>` (heavy) | `ListResponse<BrawlerResponse>` (light) |
| `GET /v1/brawlers/{id}`        | `BrawlerResponse` (heavy)      | `BrawlerDetailResponse` (신규)     |
| `GET /v1/brawler/search`       | `ListResponse<BrawlerResponse>` (heavy) | `ListResponse<BrawlerResponse>` (light) |
| `GET /v1/players/{tag}/brawlers` | `PlayerBrawlerResponse[]` (buffy 3 필드만) | wrapper 5종 컬렉션 + buffies 객체 |

전투/통계/랭킹 응답에 nested 된 `BrawlerResponse?` 는 자동으로 가벼워진다 (호환성: 클라이언트가 아이템 필드를 안 쓰면 무영향, 쓰는 곳 없음을 검증).

## 5. 조회/매핑 (Backend)

### 5.1 BrawlerRepository 변경

`BrawlerRepository.findAll(): List<Brawler>` 의 리턴 타입은 가벼운 Brawler. 현재 5종 아이템 매핑 코드는 `findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems>` 로 분리.

```kotlin
@Repository
@Transactional(readOnly = true)
class BrawlerRepository(...) {
    fun findAll(): List<Brawler> {
        // brawlerJpaRepository, brawlerNames, portrait images 만 사용
    }

    fun findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems> {
        // 기존 findAll() 의 gadgets/gears/starPowers/hyperCharges/buffies 매핑 코드를 이쪽으로 이동.
        // 모든 카탈로그 Brawler ID 가 키로 존재해야 한다 — 아이템이 없는 브롤러도
        // BrawlerItems(emptyList(), emptyList(), emptyList(), null, null) 로 채운다.
    }
}
```

### 5.2 BrawlerRepositoryWithCache 변경

```kotlin
@Repository
class BrawlerRepositoryWithCache(private val repo: BrawlerRepository) {
    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v2:brawlerList'")
    fun findAllBrawlers(): List<Brawler> = repo.findAll().sortedBy { it.id.value }

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v2:brawlers'")
    fun findBrawlers(): Brawlers = Brawlers(findAllBrawlers())

    @Cacheable(cacheNames = [CacheNames.BRAWLER], key = "'v2:brawlerItems'")
    fun findAllBrawlerItems(): Map<BrawlStarsId, BrawlerItems> = repo.findAllBrawlerItems()
}
```

캐시 키 prefix `v2:` 로 갱신 (기존 `v1:*` 와 자연 무효화).

### 5.3 BrawlerService 변경

```kotlin
@Service
class BrawlerService(private val cache: BrawlerRepositoryWithCache) {
    fun getAll(): List<Brawler> = cache.findAllBrawlers()

    fun get(id: BrawlStarsId): Brawler =
        cache.findAllBrawlers().find { it.id == id }
            ?: throw CoreException(ErrorType.NOT_FOUND, "id=$id")

    fun getDetail(id: BrawlStarsId): BrawlerDetail {
        val brawler = get(id)
        val items = cache.findAllBrawlerItems()[id]
            ?: BrawlerItems(emptyList(), emptyList(), emptyList(), null, null)
        return BrawlerDetail(brawler, items)
    }

    fun search(name: String): List<Brawler> { /* 그대로 */ }
}
```

### 5.4 BrawlerController 변경

```kotlin
@GetMapping("/v1/brawlers/{id}")
fun getBrawler(@PathVariable id: Long): ResponseEntity<BrawlerDetailResponse> =
    ResponseEntity.ok(BrawlerDetailResponse.of(brawlerService.getDetail(BrawlStarsId(id))))
```

`getBrawlers`, `searchBrawlers` 는 호출하는 메서드만 동일, 응답 모양만 가벼워짐.

### 5.5 PlayerRepository 변경

`findAllPlayerBrawler(tag)` 가 보유 4종 + buffies 모두 조회 후 카탈로그와 매칭.

기존 `playerBrawlerBuffyJpaRepository` 외에 4개 JpaRepository (`PlayerBrawlerGadgetJpaRepository`, `PlayerBrawlerGearJpaRepository`, `PlayerBrawlerStarPowerJpaRepository`, `PlayerBrawlerHyperChargeJpaRepository`) 를 주입한다. 4 개 모두 `findAllByPlayerBrawlerIn(List<PlayerBrawlerEntity>): List<...>` 메서드가 이미 존재한다(검증 완료).

```kotlin
fun findAllPlayerBrawler(tag: BrawlStarsTag): List<PlayerBrawler> {
    val playerEntity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return emptyList()
    val pbEntities = playerBrawlerJpaRepository.findAllByPlayer(playerEntity)
    val brawlers = brawlerRepositoryWithCache.findBrawlers()
    val itemsByBrawlerId = brawlerRepositoryWithCache.findAllBrawlerItems()

    val gadgetsByPbId = playerBrawlerGadgetJpaRepository.findAllByPlayerBrawlerIn(pbEntities)
        .groupBy({ it.playerBrawler.id!! }, { it.gadgetBrawlStarsId })
        .mapValues { (_, ids) -> ids.toSet() }
    val gearsByPbId = playerBrawlerGearJpaRepository.findAllByPlayerBrawlerIn(pbEntities)
        .groupBy({ it.playerBrawler.id!! }, { it.gearBrawlStarsId })
        .mapValues { (_, ids) -> ids.toSet() }
    val starPowersByPbId = playerBrawlerStarPowerJpaRepository.findAllByPlayerBrawlerIn(pbEntities)
        .groupBy({ it.playerBrawler.id!! }, { it.starPowerBrawlStarsId })
        .mapValues { (_, ids) -> ids.toSet() }
    val hyperChargesByPbId = playerBrawlerHyperChargeJpaRepository.findAllByPlayerBrawlerIn(pbEntities)
        .groupBy({ it.playerBrawler.id!! }, { it.hyperChargeBrawlStarsId })
        .mapValues { (_, ids) -> ids.toSet() }
    val buffiesByPbId = playerBrawlerBuffyJpaRepository.findAllByPlayerBrawlerIn(pbEntities)
        .groupBy({ it.playerBrawler.id!! }, { it.buffyType })
        .mapValues { (_, types) -> types.toSet() }

    return pbEntities.map { entity ->
        val brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId)
        val brawler = brawlers.find(brawlerId)
        val items = itemsByBrawlerId[brawlerId]
        toPlayerBrawler(
            entity = entity,
            brawler = brawler,
            items = items,
            ownedGadgetIds = gadgetsByPbId[entity.id!!] ?: emptySet(),
            ownedGearIds = gearsByPbId[entity.id!!] ?: emptySet(),
            ownedStarPowerIds = starPowersByPbId[entity.id!!] ?: emptySet(),
            ownedHyperChargeIds = hyperChargesByPbId[entity.id!!] ?: emptySet(),
            ownedBuffyTypes = buffiesByPbId[entity.id!!] ?: emptySet(),
        )
    }
}

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
            brawler = brawler, power = entity.power, rank = entity.rank,
            trophies = entity.trophies, highestTrophies = entity.highestTrophies,
            prestigeLevel = entity.prestigeLevel,
            gadgets = emptyList(), gears = emptyList(), starPowers = emptyList(),
            hyperCharge = null, buffies = null,
        )
    }
    return PlayerBrawler(
        brawler = brawler,
        power = entity.power, rank = entity.rank,
        trophies = entity.trophies, highestTrophies = entity.highestTrophies,
        prestigeLevel = entity.prestigeLevel,
        gadgets = items.gadgets.map { PlayerBrawlerItem(it, it.id.value in ownedGadgetIds) },
        gears = items.gears.map { PlayerBrawlerItem(it, it.id.value in ownedGearIds) },
        starPowers = items.starPowers.map { PlayerBrawlerItem(it, it.id.value in ownedStarPowerIds) },
        hyperCharge = items.hyperCharge?.let { PlayerBrawlerItem(it, it.id.value in ownedHyperChargeIds) },
        buffies = items.buffies?.let {
            PlayerBrawlerBuffies(
                gadget = PlayerBrawlerItem(it.gadget, BuffyType.GADGET in ownedBuffyTypes),
                starPower = PlayerBrawlerItem(it.starPower, BuffyType.STAR_POWER in ownedBuffyTypes),
                hyperCharge = PlayerBrawlerItem(it.hyperCharge, BuffyType.HYPER_CHARGE in ownedBuffyTypes),
            )
        },
    )
}
```

쿼리 횟수: 1 (player) + 1 (player brawlers) + 5 (보유 4종 + buffy) + 캐시 hit (brawlers / brawlerItems). 총 7 쿼리 (캐시 콜드 1회 추가 시 10).

### 5.6 검증

```bash
cd imstargg-backend
./gradlew :core:core-api:compileKotlin
./gradlew :core:core-api:ktlintCheck
./gradlew :core:core-api:unitTest
```

## 6. 프론트엔드

### 6.1 `src/api/types.ts`

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

(통계/전투 응답의 `brawler: BrawlerResponse | null` 은 그대로—타입 변경 없이 모양만 가벼워짐.)

### 6.2 `src/domain/brawler/types.ts`

```ts
export interface BrawlerItem { id: number; name: string; imageUrl: string | null }
export interface Buffies { gadget: BuffyResponse; starPower: BuffyResponse; hyperCharge: BuffyResponse }

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

### 6.3 `src/domain/brawler/mappers.ts`

```ts
export function toBrawler(dto: BrawlerResponse): Brawler {
  return {
    id: dto.id, name: pickName(dto.name),
    rarity: dto.rarity, role: dto.role,
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
  return { brawler: toBrawler(dto.brawler), items: toBrawlerItems(dto.items) };
}
```

### 6.4 `src/domain/player/types.ts`, `mappers.ts`

```ts
export interface PlayerBrawlerOwnedItem {
  item: BrawlerItem;
  owned: boolean;
}

export interface PlayerBrawlerBuffies {
  gadget: { item: BuffyResponse; owned: boolean };
  starPower: { item: BuffyResponse; owned: boolean };
  hyperCharge: { item: BuffyResponse; owned: boolean };
}

export interface PlayerBrawler {
  brawler: Brawler | null;
  power: number; rank: number; trophies: number;
  highestTrophies: number; prestigeLevel: number;
  gadgets: PlayerBrawlerOwnedItem[];
  gears: PlayerBrawlerOwnedItem[];
  starPowers: PlayerBrawlerOwnedItem[];
  hyperCharge: PlayerBrawlerOwnedItem | null;
  buffies: PlayerBrawlerBuffies | null;
}
```

mapper 는 응답을 `toBrawlerItem` 으로 풀고 `owned` 만 옮긴다.

기어의 rarity 는 BrawlerItem 에 포함되지 않으므로 (현재 컨벤션과 동일) 표시하지 않는다.

### 6.5 페이지 변경

#### `app/brawlers/[id]/page.tsx`

서버 컴포넌트가 `BrawlerDetail` 을 받도록. `pickName/role/rarity/items 갯수` 등 메타데이터 생성도 detail.brawler / detail.items 로 분리해서 사용.

#### `app/brawlers/[id]/_components/BrawlerContent.tsx`

```tsx
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

`BrawlerHero` 는 `Brawler` (가벼운) 만 받도록 시그니처 변경.

#### `app/brawlers/[id]/_components/BrawlerItems.tsx`

`brawler: Brawler` 대신 `items: BrawlerItems` 받기.

```tsx
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

#### `app/brawlers/[id]/_components/ItemOwnershipSection.tsx`

`brawler` 대신 `items` 를 받도록 변경. `findItemName` 로직은 `items.gadgets/gears/...` 참조.

#### `src/hooks/queries/useBrawler.ts` → `useBrawlerDetail.ts`

기존 `useBrawler` 가 `Brawler` 를 받았다면 (단건이라 사실은 detail), 이름·시그니처를 `useBrawlerDetail` 로 바꾸고 응답 타입은 `BrawlerDetailResponse` → `BrawlerDetail`.

리스트/검색 훅 (`useBrawlers`, `useSearchBrawlers`) 의 응답 타입은 `Brawler[]` 로 그대로 (가벼워졌을 뿐).

#### `app/players/[tag]/_components/PlayerBrawlers.tsx` → 보유 인디케이터 추가

카드 컴포넌트를 `PlayerBrawlerCard` 로 분리:

```
┌──────────────────────┐
│  [브롤러 아바타]     │
│      Edgar           │
│  파워 11 · 🏆 1,234  │
│ ─────────────────── │
│ 가젯  ●●○ (2/3)     │
│ 스파  ●○ (1/2)      │
│ 기어  ●●○○○ (2/5)  │
│ 하이퍼차지   ✓       │
│ 버피  G◇ S■ HC■     │  ← 카탈로그 buffies 가 있을 때만, 보유는 채움
└──────────────────────┘
```

새 컴포넌트:
- `app/players/[tag]/_components/PlayerBrawlerCard.tsx` — 위 레이아웃
- `app/players/[tag]/_components/PlayerBrawlerOwnership.tsx` — dot row + hyperCharge 인디케이터 + buffy row

스타일:
- 보유: `bg-primary` (또는 도메인 색상)
- 미보유: `bg-muted/30 border border-muted`
- HyperCharge 가 카탈로그에 있고 미보유면 회색 체크 자리, 보유면 색상 체크
- Buffies 카탈로그가 null 이면 buffy row 자체 미렌더

기존 정렬/그리드 (sm:3, md:4) 는 유지.

### 6.6 검증

```bash
cd imstargg-frontend
pnpm typecheck
pnpm lint
pnpm test:run
```

E2E 는 prod 응답이 새 모양 배포되기 전 깨지므로 이번 PR 에서 실행하지 않는다 (PR 설명에 "backend + frontend 동시 배포" 명시).

## 7. 영향받는 파일 (요약)

### Backend (core-api)

생성:
- `core/core-api/.../domain/brawlstars/brawler/BrawlerItems.kt`
- `core/core-api/.../domain/brawlstars/brawler/BrawlerDetail.kt`
- `core/core-api/.../domain/brawlstars/player/PlayerBrawlerItem.kt`
- `core/core-api/.../domain/brawlstars/player/PlayerBrawlerBuffies.kt`
- `core/core-api/.../api/controller/v1/response/BrawlerItemsResponse.kt`
- `core/core-api/.../api/controller/v1/response/BrawlerDetailResponse.kt`
- `core/core-api/.../api/controller/v1/response/PlayerBrawlerItemResponse.kt`
- `core/core-api/.../api/controller/v1/response/PlayerBrawlerBuffiesResponse.kt`

수정:
- `core/core-api/.../domain/brawlstars/brawler/Brawler.kt` (5종 아이템 필드 제거)
- `core/core-api/.../domain/brawlstars/player/PlayerBrawler.kt` (5종 wrapper 컬렉션 추가)
- `core/core-api/.../application/brawlstars/BrawlerRepository.kt` (findAll 분리 + findAllBrawlerItems 추가)
- `core/core-api/.../application/brawlstars/BrawlerRepositoryWithCache.kt` (캐시 키/메서드 추가)
- `core/core-api/.../application/brawlstars/BrawlerService.kt` (getDetail 추가)
- `core/core-api/.../application/brawlstars/PlayerRepository.kt` (보유 4종 조회 + 매핑)
- `core/core-api/.../api/controller/v1/BrawlerController.kt` (getBrawler 응답 타입)
- `core/core-api/.../api/controller/v1/response/BrawlerResponse.kt` (가벼움)
- `core/core-api/.../api/controller/v1/response/PlayerBrawlerResponse.kt` (wrapper 컬렉션)

### Frontend

수정:
- `src/api/types.ts`
- `src/domain/brawler/types.ts`, `mappers.ts`
- `src/domain/player/types.ts`, `mappers.ts`
- `src/api/endpoints/brawler.ts` (단건 응답 타입 변경)
- `src/hooks/queries/useBrawler.ts` (단건 detail 훅으로 정비)
- `app/brawlers/[id]/page.tsx`
- `app/brawlers/[id]/_components/BrawlerContent.tsx`
- `app/brawlers/[id]/_components/BrawlerItems.tsx`
- `app/brawlers/[id]/_components/BrawlerHero.tsx` (시그니처는 가벼운 Brawler 받음 — 실제 사용 필드와 일치)
- `app/brawlers/[id]/_components/ItemOwnershipSection.tsx`
- `app/players/[tag]/page.tsx` (PlayerBrawler 모양 mapper 변경 효과만)
- `app/players/[tag]/_components/PlayerBrawlers.tsx`

생성:
- `app/players/[tag]/_components/PlayerBrawlerCard.tsx`
- `app/players/[tag]/_components/PlayerBrawlerOwnership.tsx`

테스트 영향:
- 변경된 도메인/응답을 사용하는 단위 테스트 (백엔드: PlayerRepository, BrawlerRepository, controller 테스트 / 프론트엔드: domain 매퍼, page snapshot/integration) 동기화 필요.

## 8. 리스크 / 호환성

- **API 호환성 깨짐**: `BrawlerResponse` 의 아이템 필드 제거, `GET /v1/brawlers/{id}` 의 응답 모양 변경, `PlayerBrawlerResponse` 의 buffy 3 필드 → wrapper 5 종 컬렉션으로 재구조. 같은 PR/배포 단위로 Backend + Frontend 동시 적용.
- **다른 클라이언트**: `core-api` 는 `imstargg-frontend` 만 클라이언트로 명시되어 있다. 다른 외부 사용처 없음을 PR 검토 시 재확인.
- **통계 응답**: `BrawlerItemOwnershipStatisticsResponse` 등 통계 자체는 ID 기반이라 변경 대상 아님.
- **캐시**: `BrawlerRepositoryWithCache` 의 키 prefix 를 `v1:` → `v2:` 로 변경하여 자연 무효화. 운영 배포 시 cold start 1회 발생.
- **PlayerBrawler 응답 페이로드 증가**: 한 플레이어당 ~100 브롤러 × ~5 카탈로그 아이템 풀 데이터 wrapping. 응답 크기 증가 trade-off 는 명시적으로 수용 (도메인 표현력 우선).

## 9. 비고

- `BrawlerItemOwnershipStatisticsResponse.hyperCharges` 는 통계 측이라 list 모양 그대로 유지 (브롤러 모델 변화와 별개의 관심사).
- `BattlePlayerBrawlerResponse` 는 `BrawlerResponse?` 만 nested — 자동으로 가벼워짐. 프론트엔드 `BattlePlayerRow` 는 `id/name/portrait/rarity` 만 사용해 영향 없음.
- 기어 `rarity` 표시는 차후 도입 시 BrawlerItem 모델 확장 또는 별도 GearItem 도입 검토 (이번 범위 밖).
