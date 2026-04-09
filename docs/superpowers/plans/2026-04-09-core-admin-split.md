# core-admin Frontend/Backend 분리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vaadin 기반 core-admin을 REST API 백엔드로 전환하고, Next.js + shadcn/ui 기반 admin 프론트엔드를 생성한다.

**Architecture:** 기존 core-admin의 서비스 레이어를 재활용하면서 Vaadin UI를 REST 컨트롤러로 교체한다. 프론트엔드는 독립된 Next.js 프로젝트로 API를 호출한다. JPA 엔티티를 도메인 엔티티로 직접 사용하되, API 응답은 별도 DTO로 변환한다.

**Tech Stack:** Spring Boot + Kotlin (backend), Next.js + TypeScript + shadcn/ui (frontend)

---

## Phase 1: Backend - core-admin REST API 전환

### Task 1: Vaadin 제거 및 빌드 설정 변경

**Files:**
- Modify: `imstargg-backend/core/core-admin/build.gradle.kts`
- Modify: `imstargg-backend/core/core-admin/src/main/resources/application.yml`

- [ ] **Step 1: build.gradle.kts에서 Vaadin 제거**

`imstargg-backend/core/core-admin/build.gradle.kts`를 다음으로 교체:

```kotlin
tasks.getByName("bootJar") { enabled = true }

dependencies {
    implementation(project(":core:core-enum"))
    implementation(project(":infrastructure:db-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":infrastructure:aws-s3"))

    testImplementation(testFixtures(project(":infrastructure:db-core")))
}
```

- [ ] **Step 2: application.yml에서 Vaadin 설정 제거, CORS 프로퍼티 추가**

`imstargg-backend/core/core-admin/src/main/resources/application.yml`을 다음으로 교체:

```yaml
spring:
  application:
    name: core-admin

  profiles:
    active: local
    include:
      - db
      - aws-s3

app:
  cors:
    allowed-origins:
      - http://localhost:3000

---
spring:
  config.activate.on-profile: local

---
spring:
  config.activate.on-profile: local-dev

---
spring:
  config.activate.on-profile: dev

app:
  cors:
    allowed-origins:
      - https://admin.imstargg.com

---
spring:
  config.activate.on-profile: prod

app:
  cors:
    allowed-origins:
      - https://admin.imstargg.com
```

- [ ] **Step 3: settings.gradle.kts에서 vaadin 플러그인이 core-admin에만 적용되었는지 확인**

`imstargg-backend/settings.gradle.kts`의 pluginManagement에서 `com.vaadin` 플러그인 선언은 유지한다 (다른 모듈이 사용할 수도 있으므로). build.gradle.kts에서 `plugins { id("com.vaadin") }` 블록을 제거했으므로 core-admin에는 적용되지 않는다.

- [ ] **Step 4: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL (Vaadin import가 있는 UI 파일들이 아직 있으므로 실패할 수 있음 - Task 2에서 삭제)

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-admin/build.gradle.kts imstargg-backend/core/core-admin/src/main/resources/application.yml
git commit -m "chore(core-admin): Vaadin 의존성 제거 및 CORS 프로퍼티 추가"
```

---

### Task 2: Vaadin UI 삭제 및 CORS 설정

**Files:**
- Delete: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/ui/` (전체 디렉터리)
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/config/CorsProperties.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/config/WebConfig.kt`

- [ ] **Step 1: UI 디렉터리 전체 삭제**

```bash
rm -rf imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/ui
```

- [ ] **Step 2: CorsProperties 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/config/CorsProperties.kt`:

```kotlin
package com.imstargg.core.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.cors")
data class CorsProperties(
    val allowedOrigins: List<String>,
)
```

- [ ] **Step 3: WebConfig 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/config/WebConfig.kt`:

```kotlin
package com.imstargg.core.admin.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val corsProperties: CorsProperties,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "DELETE")
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-admin/
git commit -m "refactor(core-admin): Vaadin UI 삭제 및 CORS 설정 추가"
```

---

### Task 3: 컨트롤러 Response DTO 생성

기존 `response/` 패키지의 클래스들은 JPA 엔티티를 직접 참조하므로 JSON 직렬화에 적합하지 않다. 컨트롤러 레이어에 API 응답용 DTO를 생성한다.

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/DashboardSummaryAdminResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BrawlerAdminResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BrawlerDetailAdminResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/GearAdminResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BattleEventAdminResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BattleEventModeAdminResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/MaintenanceAdminResponse.kt`

- [ ] **Step 1: DashboardSummaryAdminResponse 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/DashboardSummaryAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.DashboardSummaryResponse

data class DashboardSummaryAdminResponse(
    val brawlerTotal: Int,
    val brawlerWithName: Int,
    val brawlerWithImage: Int,
    val gearTotal: Int,
    val gearWithName: Int,
    val gearWithImage: Int,
    val eventModeTotal: Int,
    val eventModeWithName: Int,
    val eventModeWithIcon: Int,
    val eventMapTotal: Int,
    val eventMapWithName: Int,
) {
    companion object {
        fun from(response: DashboardSummaryResponse) = DashboardSummaryAdminResponse(
            brawlerTotal = response.brawlerTotal,
            brawlerWithName = response.brawlerWithName,
            brawlerWithImage = response.brawlerWithImage,
            gearTotal = response.gearTotal,
            gearWithName = response.gearWithName,
            gearWithImage = response.gearWithImage,
            eventModeTotal = response.eventModeTotal,
            eventModeWithName = response.eventModeWithName,
            eventModeWithIcon = response.eventModeWithIcon,
            eventMapTotal = response.eventMapTotal,
            eventMapWithName = response.eventMapWithName,
        )
    }
}
```

- [ ] **Step 2: BrawlerAdminResponse 생성 (목록용)**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BrawlerAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language

data class BrawlerAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val hasImage: Boolean,
) {
    companion object {
        fun from(response: BrawlerResponse) = BrawlerAdminResponse(
            brawlStarsId = response.brawler.brawlStarsId,
            names = response.names,
            rarity = response.brawler.rarity,
            role = response.brawler.role,
            hasImage = response.image != null,
        )
    }
}
```

- [ ] **Step 3: BrawlerDetailAdminResponse 생성 (상세용)**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BrawlerDetailAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.response.GadgetResponse
import com.imstargg.core.admin.response.HyperChargeResponse
import com.imstargg.core.admin.response.StarPowerResponse
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType

data class BrawlerDetailAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val imageUrl: String?,
    val buffyImages: Map<BrawlStarsImageType, String?>,
    val starPowers: List<StarPowerAdminResponse>,
    val gadgets: List<GadgetAdminResponse>,
    val hyperCharges: List<HyperChargeAdminResponse>,
    val linkedGearBrawlStarsIds: Set<Long>,
    val allGears: List<GearSummaryAdminResponse>,
) {
    companion object {
        fun from(response: BrawlerResponse) = BrawlerDetailAdminResponse(
            brawlStarsId = response.brawler.brawlStarsId,
            names = response.names,
            rarity = response.brawler.rarity,
            role = response.brawler.role,
            imageUrl = response.image?.url,
            buffyImages = response.buffyImages.mapValues { it.value?.url },
            starPowers = response.starPowers.map { StarPowerAdminResponse.from(it) },
            gadgets = response.gadgets.map { GadgetAdminResponse.from(it) },
            hyperCharges = response.hyperCharges.map { HyperChargeAdminResponse.from(it) },
            linkedGearBrawlStarsIds = response.linkedGearIds,
            allGears = response.allGears.map { GearSummaryAdminResponse.from(it) },
        )
    }
}

data class StarPowerAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(response: StarPowerResponse) = StarPowerAdminResponse(
            brawlStarsId = response.starPower.brawlStarsId,
            names = response.names,
            imageUrl = response.image?.url,
        )
    }
}

data class GadgetAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(response: GadgetResponse) = GadgetAdminResponse(
            brawlStarsId = response.gadget.brawlStarsId,
            names = response.names,
            imageUrl = response.image?.url,
        )
    }
}

data class HyperChargeAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
) {
    companion object {
        fun from(response: HyperChargeResponse) = HyperChargeAdminResponse(
            brawlStarsId = response.hyperCharge.brawlStarsId,
            names = response.names,
            imageUrl = response.image?.url,
        )
    }
}

data class GearSummaryAdminResponse(
    val brawlStarsId: Long,
    val rarity: com.imstargg.core.enums.GearRarity,
) {
    companion object {
        fun from(response: com.imstargg.core.admin.response.GearResponse) = GearSummaryAdminResponse(
            brawlStarsId = response.gear.brawlStarsId,
            rarity = response.gear.rarity,
        )
    }
}
```

- [ ] **Step 4: GearAdminResponse 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/GearAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language

data class GearAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: GearRarity,
    val hasImage: Boolean,
) {
    companion object {
        fun from(response: com.imstargg.core.admin.response.GearResponse) = GearAdminResponse(
            brawlStarsId = response.gear.brawlStarsId,
            names = response.names,
            rarity = response.gear.rarity,
            hasImage = response.image != null,
        )
    }
}
```

- [ ] **Step 5: BattleEventAdminResponse 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BattleEventAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.core.enums.Language

data class BattleEventAdminResponse(
    val brawlStarsId: Long,
    val modeBrawlStarsName: String?,
    val mapName: String?,
    val mapNames: Map<Language, String>,
) {
    companion object {
        fun from(response: com.imstargg.core.admin.response.BattleEventResponse) = BattleEventAdminResponse(
            brawlStarsId = response.event.brawlStarsId,
            modeBrawlStarsName = response.event.mode?.brawlStarsName,
            mapName = response.event.mapName,
            mapNames = response.mapNames,
        )
    }
}
```

- [ ] **Step 6: BattleEventModeAdminResponse 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/BattleEventModeAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.core.enums.Language

data class BattleEventModeAdminResponse(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String>,
    val hasIcon: Boolean,
) {
    companion object {
        fun from(response: com.imstargg.core.admin.response.BattleEventModeResponse) = BattleEventModeAdminResponse(
            brawlStarsId = response.mode.brawlStarsId,
            brawlStarsName = response.mode.brawlStarsName,
            names = response.names,
            hasIcon = response.icon != null,
        )
    }
}
```

- [ ] **Step 7: MaintenanceAdminResponse 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/response/MaintenanceAdminResponse.kt`:

```kotlin
package com.imstargg.core.admin.controller.response

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import java.time.Instant
import java.time.OffsetDateTime

data class MaintenanceAdminResponse(
    val id: Long,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val createdAt: Instant?,
) {
    companion object {
        fun from(entity: BrawlStarsApiMaintenanceEntity) = MaintenanceAdminResponse(
            id = entity.id!!,
            startTime = entity.startTime,
            endTime = entity.endTime,
            createdAt = entity.createdAt,
        )
    }
}
```

- [ ] **Step 8: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/
git commit -m "feat(core-admin): API 응답 DTO 추가"
```

---

### Task 4: 컨트롤러 Request DTO 생성

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BrawlerCreateRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/MessagesUpdateRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/GearCreateRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BattleEventCreateRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BattleEventModeCreateRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/MaintenanceRegisterRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BrawlerSubItemCreateRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BrawlerGearsUpdateRequest.kt`

- [ ] **Step 1: BrawlerCreateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BrawlerCreateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class BrawlerCreateRequest(
    val brawlStarsId: Long,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
)
```

- [ ] **Step 2: MessagesUpdateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/MessagesUpdateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

import com.imstargg.core.enums.Language

data class MessagesUpdateRequest(
    val messages: Map<Language, String>,
)
```

- [ ] **Step 3: GearCreateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/GearCreateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

import com.imstargg.core.enums.GearRarity

data class GearCreateRequest(
    val brawlStarsId: Long,
    val rarity: GearRarity,
)
```

- [ ] **Step 4: BattleEventCreateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BattleEventCreateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

data class BattleEventCreateRequest(
    val brawlStarsId: Long,
    val modeId: Long?,
    val mapName: String?,
)
```

- [ ] **Step 5: BattleEventModeCreateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BattleEventModeCreateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
)
```

- [ ] **Step 6: MaintenanceRegisterRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/MaintenanceRegisterRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

import java.time.OffsetDateTime

data class MaintenanceRegisterRequest(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)
```

- [ ] **Step 7: BrawlerSubItemCreateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BrawlerSubItemCreateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

data class BrawlerSubItemCreateRequest(
    val brawlStarsId: Long,
)
```

- [ ] **Step 8: BrawlerGearsUpdateRequest 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/BrawlerGearsUpdateRequest.kt`:

```kotlin
package com.imstargg.core.admin.controller.request

data class BrawlerGearsUpdateRequest(
    val gearBrawlStarsIds: Set<Long>,
)
```

- [ ] **Step 9: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 10: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/request/
git commit -m "feat(core-admin): API 요청 DTO 추가"
```

---

### Task 5: REST 컨트롤러 생성 - Dashboard, Maintenance

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/DashboardAdminController.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/MaintenanceAdminController.kt`

- [ ] **Step 1: DashboardAdminController 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/DashboardAdminController.kt`:

```kotlin
package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.DashboardService
import com.imstargg.core.admin.controller.response.DashboardSummaryAdminResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardAdminController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/dashboard/summary")
    fun getSummary(): ResponseEntity<DashboardSummaryAdminResponse> {
        val summary = dashboardService.getSummary()
        return ResponseEntity.ok(DashboardSummaryAdminResponse.from(summary))
    }
}
```

- [ ] **Step 2: MaintenanceAdminController 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/MaintenanceAdminController.kt`:

```kotlin
package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BrawlStarsApiMaintenanceAdminService
import com.imstargg.core.admin.controller.request.MaintenanceRegisterRequest
import com.imstargg.core.admin.controller.response.MaintenanceAdminResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MaintenanceAdminController(
    private val maintenanceAdminService: BrawlStarsApiMaintenanceAdminService,
) {
    @GetMapping("/maintenances")
    fun getAll(): ResponseEntity<List<MaintenanceAdminResponse>> {
        val maintenances = maintenanceAdminService.findAll()
        return ResponseEntity.ok(maintenances.map { MaintenanceAdminResponse.from(it) })
    }

    @PostMapping("/maintenances")
    fun register(@RequestBody request: MaintenanceRegisterRequest): ResponseEntity<MaintenanceAdminResponse> {
        val maintenance = maintenanceAdminService.register(request.startTime, request.endTime)
        return ResponseEntity.ok(MaintenanceAdminResponse.from(maintenance))
    }

    @DeleteMapping("/maintenances/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        maintenanceAdminService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/DashboardAdminController.kt imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/MaintenanceAdminController.kt
git commit -m "feat(core-admin): Dashboard, Maintenance REST 컨트롤러 추가"
```

---

### Task 6: REST 컨트롤러 생성 - Gear, Event, EventMode

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/GearAdminController.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventAdminController.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventModeAdminController.kt`

- [ ] **Step 1: GearAdminController 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/GearAdminController.kt`:

```kotlin
package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.GearAdminService
import com.imstargg.core.admin.controller.request.GearCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.GearAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class GearAdminController(
    private val gearAdminService: GearAdminService,
) {
    @GetMapping("/gears")
    fun getAll(): ResponseEntity<List<GearAdminResponse>> {
        val gears = gearAdminService.findAll()
        return ResponseEntity.ok(gears.map { GearAdminResponse.from(it) })
    }

    @PostMapping("/gears")
    fun create(@RequestBody request: GearCreateRequest): ResponseEntity<Void> {
        gearAdminService.create(request.brawlStarsId, request.rarity)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/gears/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        gearAdminService.updateMessages(MessageType.GEAR_NAME, brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/gears/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        gearAdminService.updateImage(
            BrawlStarsImageType.GEAR,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
```

- [ ] **Step 2: BattleEventAdminController 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventAdminController.kt`:

```kotlin
package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BattleEventAdminService
import com.imstargg.core.admin.controller.request.BattleEventCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BattleEventAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BattleEventAdminController(
    private val battleEventAdminService: BattleEventAdminService,
) {
    @GetMapping("/events")
    fun getAll(): ResponseEntity<List<BattleEventAdminResponse>> {
        val events = battleEventAdminService.findAllEvents()
        return ResponseEntity.ok(events.map { BattleEventAdminResponse.from(it) })
    }

    @PostMapping("/events")
    fun create(@RequestBody request: BattleEventCreateRequest): ResponseEntity<Void> {
        battleEventAdminService.createEvent(request.brawlStarsId, request.modeId, request.mapName)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/events/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        battleEventAdminService.updateMessages(MessageType.BATTLE_EVENT_NAME, brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }
}
```

- [ ] **Step 3: BattleEventModeAdminController 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BattleEventModeAdminController.kt`:

```kotlin
package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BattleEventAdminService
import com.imstargg.core.admin.controller.request.BattleEventModeCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BattleEventModeAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class BattleEventModeAdminController(
    private val battleEventAdminService: BattleEventAdminService,
) {
    @GetMapping("/event-modes")
    fun getAll(): ResponseEntity<List<BattleEventModeAdminResponse>> {
        val modes = battleEventAdminService.findAllModes()
        return ResponseEntity.ok(modes.map { BattleEventModeAdminResponse.from(it) })
    }

    @PostMapping("/event-modes")
    fun create(@RequestBody request: BattleEventModeCreateRequest): ResponseEntity<Void> {
        battleEventAdminService.createMode(request.brawlStarsId, request.brawlStarsName)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        battleEventAdminService.updateMessages(
            MessageType.BATTLE_EVENT_MODE_NAME,
            brawlStarsId,
            request.messages,
        )
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        battleEventAdminService.updateImage(
            BrawlStarsImageType.BATTLE_EVENT_MODE_ICON,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/
git commit -m "feat(core-admin): Gear, Event, EventMode REST 컨트롤러 추가"
```

---

### Task 7: REST 컨트롤러 생성 - Brawler

Brawler는 하위 항목(스타파워, 가젯, 하이퍼차지)과 이미지, 기어 연결 등 엔드포인트가 많으므로 별도 Task로 분리한다.

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BrawlerAdminController.kt`

- [ ] **Step 1: BrawlerAdminController 생성**

`imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BrawlerAdminController.kt`:

```kotlin
package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BrawlerAdminService
import com.imstargg.core.admin.controller.request.BrawlerCreateRequest
import com.imstargg.core.admin.controller.request.BrawlerGearsUpdateRequest
import com.imstargg.core.admin.controller.request.BrawlerSubItemCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BrawlerAdminResponse
import com.imstargg.core.admin.controller.response.BrawlerDetailAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class BrawlerAdminController(
    private val brawlerAdminService: BrawlerAdminService,
) {
    @GetMapping("/brawlers")
    fun getAll(): ResponseEntity<List<BrawlerAdminResponse>> {
        val brawlers = brawlerAdminService.findAll()
        return ResponseEntity.ok(brawlers.map { BrawlerAdminResponse.from(it) })
    }

    @GetMapping("/brawlers/{brawlStarsId}")
    fun getDetail(@PathVariable brawlStarsId: Long): ResponseEntity<BrawlerDetailAdminResponse> {
        val brawler = brawlerAdminService.findDetail(brawlStarsId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(BrawlerDetailAdminResponse.from(brawler))
    }

    @PostMapping("/brawlers")
    fun create(@RequestBody request: BrawlerCreateRequest): ResponseEntity<Void> {
        brawlerAdminService.createBrawler(request.brawlStarsId, request.rarity, request.role)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.BRAWLER_NAME, brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.BRAWLER_PORTRAIT,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/buffy-image")
    fun updateBuffyImage(
        @PathVariable brawlStarsId: Long,
        @RequestParam buffyType: BrawlStarsImageType,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            buffyType,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gears")
    fun updateGears(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerGearsUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateBrawlerGears(brawlStarsId, request.gearBrawlStarsIds)
        return ResponseEntity.ok().build()
    }

    // Star Power
    @PostMapping("/brawlers/{brawlStarsId}/star-powers")
    fun createStarPower(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createStarPower(request.brawlStarsId, brawlStarsId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/messages")
    fun updateStarPowerMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable starPowerBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.STARPOWER_NAME, starPowerBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/image")
    fun updateStarPowerImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable starPowerBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.STARPOWER,
            starPowerBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    // Gadget
    @PostMapping("/brawlers/{brawlStarsId}/gadgets")
    fun createGadget(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createGadget(request.brawlStarsId, brawlStarsId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/messages")
    fun updateGadgetMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable gadgetBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.GADGET_NAME, gadgetBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/image")
    fun updateGadgetImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable gadgetBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.GADGET,
            gadgetBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    // Hyper Charge
    @PostMapping("/brawlers/{brawlStarsId}/hyper-charges")
    fun createHyperCharge(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createHyperCharge(request.brawlStarsId, brawlStarsId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/messages")
    fun updateHyperChargeMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable hyperChargeBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.HYPERCHARGE_NAME, hyperChargeBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/image")
    fun updateHyperChargeImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable hyperChargeBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.HYPERCHARGE,
            hyperChargeBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
```

- [ ] **Step 2: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/BrawlerAdminController.kt
git commit -m "feat(core-admin): Brawler REST 컨트롤러 추가"
```

---

### Task 8: 기존 response 패키지 정리 및 ktlint 확인

기존 `response/` 패키지의 클래스들은 서비스 레이어의 내부 DTO로 계속 사용된다. Vaadin import가 없으므로 변경 불필요. 전체 빌드 및 lint 확인만 수행한다.

**Files:**
- No changes (verification only)

- [ ] **Step 1: 전체 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: ktlint 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:ktlintCheck
```

Expected: BUILD SUCCESSFUL. 실패 시 `./gradlew :core:core-admin:ktlintFormat`으로 수정.

- [ ] **Step 3: ktlint 수정 필요 시 커밋**

```bash
git add imstargg-backend/core/core-admin/
git commit -m "style(core-admin): ktlint 포맷 적용"
```

---

## Phase 2: Frontend - imstargg-admin-frontend 생성

### Task 9: Next.js 프로젝트 생성 및 shadcn 초기화

**Files:**
- Create: `imstargg-admin-frontend/` (전체 프로젝트)

- [ ] **Step 1: Next.js 프로젝트 생성**

프로젝트 루트(`/Users/chodyoungrae/Projects/imstargg-v2/`)에서 실행:

```bash
pnpm create next-app@latest imstargg-admin-frontend --yes
```

- [ ] **Step 2: shadcn 초기화**

```bash
cd imstargg-admin-frontend && pnpm dlx shadcn@latest init -d
```

- [ ] **Step 3: 필요한 shadcn 컴포넌트 설치**

```bash
cd imstargg-admin-frontend && pnpm dlx shadcn@latest add button card dialog input label select table tabs toast sonner sidebar separator breadcrumb badge sheet
```

- [ ] **Step 4: 환경변수 설정**

`imstargg-admin-frontend/.env.local` 생성:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

- [ ] **Step 5: 개발 서버 실행 확인**

```bash
cd imstargg-admin-frontend && pnpm dev
```

Expected: localhost:3000에서 기본 Next.js 페이지 표시

- [ ] **Step 6: 커밋**

```bash
git add imstargg-admin-frontend/
git commit -m "chore: Next.js admin frontend 프로젝트 생성 및 shadcn 초기화"
```

---

### Task 10: API 클라이언트 및 TypeScript 타입 정의

**Files:**
- Create: `imstargg-admin-frontend/lib/api.ts`
- Create: `imstargg-admin-frontend/lib/types.ts`

- [ ] **Step 1: TypeScript 타입 정의**

`imstargg-admin-frontend/lib/types.ts`:

```typescript
export interface DashboardSummary {
  brawlerTotal: number;
  brawlerWithName: number;
  brawlerWithImage: number;
  gearTotal: number;
  gearWithName: number;
  gearWithImage: number;
  eventModeTotal: number;
  eventModeWithName: number;
  eventModeWithIcon: number;
  eventMapTotal: number;
  eventMapWithName: number;
}

export type Language = "KOREAN" | "ENGLISH";

export interface BrawlerListItem {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  role: string;
  hasImage: boolean;
}

export interface StarPower {
  brawlStarsId: number;
  names: Record<Language, string>;
  imageUrl: string | null;
}

export interface Gadget {
  brawlStarsId: number;
  names: Record<Language, string>;
  imageUrl: string | null;
}

export interface HyperCharge {
  brawlStarsId: number;
  names: Record<Language, string>;
  imageUrl: string | null;
}

export interface GearSummary {
  brawlStarsId: number;
  rarity: string;
}

export interface BrawlerDetail {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  role: string;
  imageUrl: string | null;
  buffyImages: Record<string, string | null>;
  starPowers: StarPower[];
  gadgets: Gadget[];
  hyperCharges: HyperCharge[];
  linkedGearBrawlStarsIds: number[];
  allGears: GearSummary[];
}

export interface GearListItem {
  brawlStarsId: number;
  names: Record<Language, string>;
  rarity: string;
  hasImage: boolean;
}

export interface BattleEvent {
  brawlStarsId: number;
  modeBrawlStarsName: string | null;
  mapName: string | null;
  mapNames: Record<Language, string>;
}

export interface BattleEventMode {
  brawlStarsId: number;
  brawlStarsName: string;
  names: Record<Language, string>;
  hasIcon: boolean;
}

export interface Maintenance {
  id: number;
  startTime: string;
  endTime: string;
  createdAt: string | null;
}
```

- [ ] **Step 2: API 클라이언트 생성**

`imstargg-admin-frontend/lib/api.ts`:

```typescript
const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options?.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...options?.headers,
    },
  });
  if (!res.ok) {
    throw new Error(`API error: ${res.status} ${res.statusText}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

// Dashboard
export const getDashboardSummary = () =>
  request<import("./types").DashboardSummary>("/dashboard/summary");

// Brawler
export const getBrawlers = () =>
  request<import("./types").BrawlerListItem[]>("/brawlers");

export const getBrawlerDetail = (brawlStarsId: number) =>
  request<import("./types").BrawlerDetail>(`/brawlers/${brawlStarsId}`);

export const createBrawler = (data: { brawlStarsId: number; rarity: string; role: string }) =>
  request<void>("/brawlers", { method: "POST", body: JSON.stringify(data) });

export const updateBrawlerMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/brawlers/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadBrawlerImage = (brawlStarsId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/brawlers/${brawlStarsId}/image`, { method: "PUT", body: formData });
};

export const uploadBrawlerBuffyImage = (brawlStarsId: number, buffyType: string, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/brawlers/${brawlStarsId}/buffy-image?buffyType=${buffyType}`, {
    method: "PUT",
    body: formData,
  });
};

export const updateBrawlerGears = (brawlStarsId: number, gearBrawlStarsIds: number[]) =>
  request<void>(`/brawlers/${brawlStarsId}/gears`, {
    method: "PUT",
    body: JSON.stringify({ gearBrawlStarsIds }),
  });

export const createStarPower = (brawlerBrawlStarsId: number, brawlStarsId: number) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/star-powers`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId }),
  });

export const updateStarPowerMessages = (
  brawlerBrawlStarsId: number,
  starPowerBrawlStarsId: number,
  messages: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/star-powers/${starPowerBrawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadStarPowerImage = (
  brawlerBrawlStarsId: number,
  starPowerBrawlStarsId: number,
  file: File,
) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(
    `/brawlers/${brawlerBrawlStarsId}/star-powers/${starPowerBrawlStarsId}/image`,
    { method: "PUT", body: formData },
  );
};

export const createGadget = (brawlerBrawlStarsId: number, brawlStarsId: number) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/gadgets`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId }),
  });

export const updateGadgetMessages = (
  brawlerBrawlStarsId: number,
  gadgetBrawlStarsId: number,
  messages: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/gadgets/${gadgetBrawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadGadgetImage = (
  brawlerBrawlStarsId: number,
  gadgetBrawlStarsId: number,
  file: File,
) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(
    `/brawlers/${brawlerBrawlStarsId}/gadgets/${gadgetBrawlStarsId}/image`,
    { method: "PUT", body: formData },
  );
};

export const createHyperCharge = (brawlerBrawlStarsId: number, brawlStarsId: number) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/hyper-charges`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId }),
  });

export const updateHyperChargeMessages = (
  brawlerBrawlStarsId: number,
  hyperChargeBrawlStarsId: number,
  messages: Record<string, string>,
) =>
  request<void>(
    `/brawlers/${brawlerBrawlStarsId}/hyper-charges/${hyperChargeBrawlStarsId}/messages`,
    { method: "PUT", body: JSON.stringify({ messages }) },
  );

export const uploadHyperChargeImage = (
  brawlerBrawlStarsId: number,
  hyperChargeBrawlStarsId: number,
  file: File,
) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(
    `/brawlers/${brawlerBrawlStarsId}/hyper-charges/${hyperChargeBrawlStarsId}/image`,
    { method: "PUT", body: formData },
  );
};

// Gear
export const getGears = () =>
  request<import("./types").GearListItem[]>("/gears");

export const createGear = (data: { brawlStarsId: number; rarity: string }) =>
  request<void>("/gears", { method: "POST", body: JSON.stringify(data) });

export const updateGearMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/gears/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadGearImage = (brawlStarsId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/gears/${brawlStarsId}/image`, { method: "PUT", body: formData });
};

// Event
export const getEvents = () =>
  request<import("./types").BattleEvent[]>("/events");

export const createEvent = (data: { brawlStarsId: number; modeId?: number; mapName?: string }) =>
  request<void>("/events", { method: "POST", body: JSON.stringify(data) });

export const updateEventMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/events/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

// Event Mode
export const getEventModes = () =>
  request<import("./types").BattleEventMode[]>("/event-modes");

export const createEventMode = (data: { brawlStarsId: number; brawlStarsName: string }) =>
  request<void>("/event-modes", { method: "POST", body: JSON.stringify(data) });

export const updateEventModeMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/event-modes/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadEventModeImage = (brawlStarsId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/event-modes/${brawlStarsId}/image`, { method: "PUT", body: formData });
};

// Maintenance
export const getMaintenances = () =>
  request<import("./types").Maintenance[]>("/maintenances");

export const registerMaintenance = (data: { startTime: string; endTime: string }) =>
  request<void>("/maintenances", { method: "POST", body: JSON.stringify(data) });

export const deleteMaintenance = (id: number) =>
  request<void>(`/maintenances/${id}`, { method: "DELETE" });
```

- [ ] **Step 3: 커밋**

```bash
git add imstargg-admin-frontend/lib/
git commit -m "feat(admin-frontend): API 클라이언트 및 TypeScript 타입 정의"
```

---

### Task 11: 레이아웃 및 사이드바 네비게이션

**Files:**
- Modify: `imstargg-admin-frontend/app/layout.tsx`
- Create: `imstargg-admin-frontend/app/admin-layout.tsx`

- [ ] **Step 1: 루트 레이아웃 수정**

`imstargg-admin-frontend/app/layout.tsx` 전체를 다음으로 교체:

```tsx
import type { Metadata } from "next";
import "./globals.css";
import { Toaster } from "sonner";

export const metadata: Metadata = {
  title: "imstargg Admin",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>
        {children}
        <Toaster />
      </body>
    </html>
  );
}
```

- [ ] **Step 2: AdminLayout 컴포넌트 생성**

`imstargg-admin-frontend/app/admin-layout.tsx`:

```tsx
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/", label: "대시보드" },
  { href: "/brawlers", label: "브롤러 관리" },
  { href: "/gears", label: "기어 관리" },
  { href: "/events", label: "이벤트 관리" },
  { href: "/event-modes", label: "이벤트 모드 관리" },
  { href: "/maintenances", label: "API 점검 관리" },
];

export function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex h-screen">
      <aside className="w-64 border-r bg-muted/40 p-4">
        <h1 className="mb-6 text-lg font-semibold">imstargg Admin</h1>
        <nav className="space-y-1">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "block rounded-md px-3 py-2 text-sm",
                (item.href === "/" ? pathname === "/" : pathname.startsWith(item.href))
                  ? "bg-primary text-primary-foreground"
                  : "hover:bg-muted"
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
```

- [ ] **Step 3: 메인 페이지에 레이아웃 적용 (임시 placeholder)**

`imstargg-admin-frontend/app/page.tsx` 전체를 다음으로 교체:

```tsx
import { AdminLayout } from "./admin-layout";

export default function DashboardPage() {
  return (
    <AdminLayout>
      <h2 className="text-2xl font-bold">대시보드</h2>
      <p className="text-muted-foreground">로딩 중...</p>
    </AdminLayout>
  );
}
```

- [ ] **Step 4: 개발 서버에서 레이아웃 확인**

```bash
cd imstargg-admin-frontend && pnpm dev
```

Expected: 사이드바와 메인 영역이 표시됨

- [ ] **Step 5: 커밋**

```bash
git add imstargg-admin-frontend/app/
git commit -m "feat(admin-frontend): 사이드바 레이아웃 구성"
```

---

### Task 12: 대시보드 페이지 구현

**Files:**
- Modify: `imstargg-admin-frontend/app/page.tsx`

- [ ] **Step 1: 대시보드 페이지 구현**

`imstargg-admin-frontend/app/page.tsx` 전체를 다음으로 교체:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "./admin-layout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getDashboardSummary } from "@/lib/api";
import type { DashboardSummary } from "@/lib/types";
import Link from "next/link";

function StatCard({
  title,
  href,
  stats,
}: {
  title: string;
  href: string;
  stats: { label: string; value: number; total: number }[];
}) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">
          <Link href={href} className="hover:underline">
            {title}
          </Link>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-1 text-sm">
        {stats.map((s) => (
          <div key={s.label} className="flex justify-between">
            <span className="text-muted-foreground">{s.label}</span>
            <span>
              {s.value} / {s.total}
            </span>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);

  useEffect(() => {
    getDashboardSummary().then(setSummary);
  }, []);

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">대시보드</h2>
      {summary ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            title="브롤러"
            href="/brawlers"
            stats={[
              { label: "이름 등록", value: summary.brawlerWithName, total: summary.brawlerTotal },
              { label: "이미지 등록", value: summary.brawlerWithImage, total: summary.brawlerTotal },
            ]}
          />
          <StatCard
            title="기어"
            href="/gears"
            stats={[
              { label: "이름 등록", value: summary.gearWithName, total: summary.gearTotal },
              { label: "이미지 등록", value: summary.gearWithImage, total: summary.gearTotal },
            ]}
          />
          <StatCard
            title="이벤트 모드"
            href="/event-modes"
            stats={[
              { label: "이름 등록", value: summary.eventModeWithName, total: summary.eventModeTotal },
              { label: "아이콘 등록", value: summary.eventModeWithIcon, total: summary.eventModeTotal },
            ]}
          />
          <StatCard
            title="이벤트 맵"
            href="/events"
            stats={[
              { label: "이름 등록", value: summary.eventMapWithName, total: summary.eventMapTotal },
            ]}
          />
        </div>
      ) : (
        <p className="text-muted-foreground">로딩 중...</p>
      )}
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/app/page.tsx
git commit -m "feat(admin-frontend): 대시보드 페이지 구현"
```

---

### Task 13: 브롤러 목록 페이지

**Files:**
- Create: `imstargg-admin-frontend/app/brawlers/page.tsx`

- [ ] **Step 1: 브롤러 목록 페이지 구현**

`imstargg-admin-frontend/app/brawlers/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getBrawlers, createBrawler } from "@/lib/api";
import type { BrawlerListItem } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

const RARITIES = [
  "STARTING_BRAWLER",
  "RARE",
  "SUPER_RARE",
  "EPIC",
  "MYTHIC",
  "LEGENDARY",
  "TIME_LIMITED_BRAWLER",
];

const ROLES = [
  "TANK",
  "ASSASSIN",
  "SUPPORT",
  "CONTROLLER",
  "DAMAGE_DEALER",
  "MARKSMAN",
  "ARTILLERY",
];

export default function BrawlerListPage() {
  const [brawlers, setBrawlers] = useState<BrawlerListItem[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", role: "" });

  const loadBrawlers = () => getBrawlers().then(setBrawlers);

  useEffect(() => {
    loadBrawlers();
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity || !form.role) {
      toast.error("모든 필드를 입력해주세요.");
      return;
    }
    try {
      await createBrawler({ brawlStarsId: id, rarity: form.rarity, role: form.role });
      toast.success("브롤러가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", rarity: "", role: "" });
      loadBrawlers();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">브롤러 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>브롤러 등록</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>브롤러 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>Rarity</Label>
                <Select value={form.rarity} onValueChange={(v) => setForm({ ...form, rarity: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {RARITIES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Role</Label>
                <Select value={form.role} onValueChange={(v) => setForm({ ...form, role: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {ROLES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <Button onClick={handleCreate} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>Role</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {brawlers.map((b) => (
            <TableRow key={b.brawlStarsId}>
              <TableCell>{b.brawlStarsId}</TableCell>
              <TableCell>{b.names.KOREAN || "-"}</TableCell>
              <TableCell>{b.names.ENGLISH || "-"}</TableCell>
              <TableCell>{b.rarity}</TableCell>
              <TableCell>{b.role}</TableCell>
              <TableCell>{b.hasImage ? "O" : "X"}</TableCell>
              <TableCell>
                <Link href={`/brawlers/${b.brawlStarsId}`}>
                  <Button variant="outline" size="sm">상세</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/app/brawlers/
git commit -m "feat(admin-frontend): 브롤러 목록 페이지 구현"
```

---

### Task 14: 브롤러 상세 페이지

**Files:**
- Create: `imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx`

- [ ] **Step 1: 브롤러 상세 페이지 구현**

`imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx`:

이 페이지는 탭으로 구분된다: 기본 정보, 스타파워, 가젯, 하이퍼차지, 기어. 각 탭은 조회/수정이 명확히 분리된다.

파일이 길어지므로 구현 시 다음 구조를 따른다:

```tsx
"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  getBrawlerDetail,
  updateBrawlerMessages,
  uploadBrawlerImage,
  uploadBrawlerBuffyImage,
  updateBrawlerGears,
  createStarPower,
  updateStarPowerMessages,
  uploadStarPowerImage,
  createGadget,
  updateGadgetMessages,
  uploadGadgetImage,
  createHyperCharge,
  updateHyperChargeMessages,
  uploadHyperChargeImage,
} from "@/lib/api";
import type { BrawlerDetail, StarPower, Gadget, HyperCharge } from "@/lib/types";
import { toast } from "sonner";

// Sub-item (StarPower/Gadget/HyperCharge) edit dialog
function SubItemEditDialog({
  item,
  label,
  onSaveMessages,
  onUploadImage,
}: {
  item: StarPower | Gadget | HyperCharge;
  label: string;
  onSaveMessages: (messages: Record<string, string>) => Promise<void>;
  onUploadImage: (file: File) => Promise<void>;
}) {
  const [ko, setKo] = useState(item.names.KOREAN || "");
  const [en, setEn] = useState(item.names.ENGLISH || "");

  const handleSave = async () => {
    try {
      await onSaveMessages({ KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await onUploadImage(file);
      toast.success("이미지가 업로드되었습니다.");
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  return (
    <DialogContent>
      <DialogHeader>
        <DialogTitle>
          {label} 편집 - {item.brawlStarsId}
        </DialogTitle>
      </DialogHeader>
      <div className="space-y-4">
        <div>
          <Label>이름 (KO)</Label>
          <Input value={ko} onChange={(e) => setKo(e.target.value)} />
        </div>
        <div>
          <Label>이름 (EN)</Label>
          <Input value={en} onChange={(e) => setEn(e.target.value)} />
        </div>
        <Button onClick={handleSave}>이름 저장</Button>
        <div>
          <Label>이미지</Label>
          {item.imageUrl ? (
            <img src={item.imageUrl} alt="" className="mb-2 max-h-24" />
          ) : (
            <p className="text-sm text-muted-foreground">이미지 없음</p>
          )}
          <Input type="file" accept="image/*" onChange={handleUpload} />
        </div>
      </div>
    </DialogContent>
  );
}

// Sub-item grid and management
function SubItemSection({
  title,
  items,
  onAdd,
  onSaveMessages,
  onUploadImage,
}: {
  title: string;
  items: (StarPower | Gadget | HyperCharge)[];
  onAdd: (brawlStarsId: number) => Promise<void>;
  onSaveMessages: (itemBrawlStarsId: number, messages: Record<string, string>) => Promise<void>;
  onUploadImage: (itemBrawlStarsId: number, file: File) => Promise<void>;
}) {
  const [addOpen, setAddOpen] = useState(false);
  const [addId, setAddId] = useState("");
  const [editItem, setEditItem] = useState<(StarPower | Gadget | HyperCharge) | null>(null);

  const handleAdd = async () => {
    const id = Number(addId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    try {
      await onAdd(id);
      toast.success("등록되었습니다.");
      setAddOpen(false);
      setAddId("");
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">{title}</h3>
        <Dialog open={addOpen} onOpenChange={setAddOpen}>
          <DialogTrigger asChild>
            <Button size="sm">{title} 등록</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{title} 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={addId}
                  onChange={(e) => setAddId(e.target.value)}
                />
              </div>
              <Button onClick={handleAdd} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.brawlStarsId}>
              <TableCell>{item.brawlStarsId}</TableCell>
              <TableCell>{item.names.KOREAN || "-"}</TableCell>
              <TableCell>{item.names.ENGLISH || "-"}</TableCell>
              <TableCell>{item.imageUrl ? "O" : "X"}</TableCell>
              <TableCell>
                <Dialog
                  open={editItem?.brawlStarsId === item.brawlStarsId}
                  onOpenChange={(open) => setEditItem(open ? item : null)}
                >
                  <DialogTrigger asChild>
                    <Button variant="outline" size="sm">편집</Button>
                  </DialogTrigger>
                  <SubItemEditDialog
                    item={item}
                    label={title}
                    onSaveMessages={(msgs) => onSaveMessages(item.brawlStarsId, msgs)}
                    onUploadImage={(file) => onUploadImage(item.brawlStarsId, file)}
                  />
                </Dialog>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

export default function BrawlerDetailPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [detail, setDetail] = useState<BrawlerDetail | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");
  const [selectedGearIds, setSelectedGearIds] = useState<Set<number>>(new Set());

  const loadDetail = useCallback(() => {
    getBrawlerDetail(brawlStarsId).then((d) => {
      setDetail(d);
      setKo(d.names.KOREAN || "");
      setEn(d.names.ENGLISH || "");
      setSelectedGearIds(new Set(d.linkedGearBrawlStarsIds));
    });
  }, [brawlStarsId]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  if (!detail) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  const handleSaveMessages = async () => {
    try {
      await updateBrawlerMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadDetail();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleUploadImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadBrawlerImage(brawlStarsId, file);
      toast.success("이미지가 업로드되었습니다.");
      loadDetail();
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  const handleUploadBuffyImage = async (buffyType: string, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadBrawlerBuffyImage(brawlStarsId, buffyType, file);
      toast.success("이미지가 업로드되었습니다.");
      loadDetail();
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  const handleSaveGears = async () => {
    try {
      await updateBrawlerGears(brawlStarsId, Array.from(selectedGearIds));
      toast.success("기어 연결이 저장되었습니다.");
      loadDetail();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const toggleGear = (gearId: number) => {
    setSelectedGearIds((prev) => {
      const next = new Set(prev);
      if (next.has(gearId)) next.delete(gearId);
      else next.add(gearId);
      return next;
    });
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">브롤러 상세 - {brawlStarsId}</h2>

      <Tabs defaultValue="info" className="space-y-4">
        <TabsList>
          <TabsTrigger value="info">기본 정보</TabsTrigger>
          <TabsTrigger value="star-powers">스타파워</TabsTrigger>
          <TabsTrigger value="gadgets">가젯</TabsTrigger>
          <TabsTrigger value="hyper-charges">하이퍼차지</TabsTrigger>
          <TabsTrigger value="gears">기어</TabsTrigger>
        </TabsList>

        <TabsContent value="info" className="space-y-6">
          <Card>
            <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <p>BrawlStars ID: {detail.brawlStarsId}</p>
              <p>Rarity: {detail.rarity}</p>
              <p>Role: {detail.role}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>이름</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={ko} onChange={(e) => setKo(e.target.value)} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={en} onChange={(e) => setEn(e.target.value)} />
              </div>
              <Button onClick={handleSaveMessages}>이름 저장</Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>메인 이미지</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {detail.imageUrl ? (
                <img src={detail.imageUrl} alt="" className="max-h-32" />
              ) : (
                <p className="text-sm text-muted-foreground">이미지 없음</p>
              )}
              <Input type="file" accept="image/*" onChange={handleUploadImage} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>버피 이미지</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              {[
                { type: "BUFFY_GADGET", label: "가젯 버피" },
                { type: "BUFFY_STAR_POWER", label: "스타파워 버피" },
                { type: "BUFFY_HYPER_CHARGE", label: "하이퍼차지 버피" },
              ].map(({ type, label }) => (
                <div key={type}>
                  <p className="mb-1 text-sm font-medium">{label}</p>
                  {detail.buffyImages[type] ? (
                    <img src={detail.buffyImages[type]!} alt="" className="mb-2 max-h-24" />
                  ) : (
                    <p className="text-sm text-muted-foreground">이미지 없음</p>
                  )}
                  <Input
                    type="file"
                    accept="image/*"
                    onChange={(e) => handleUploadBuffyImage(type, e)}
                  />
                </div>
              ))}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="star-powers">
          <SubItemSection
            title="스타파워"
            items={detail.starPowers}
            onAdd={async (id) => {
              await createStarPower(brawlStarsId, id);
              loadDetail();
            }}
            onSaveMessages={async (itemId, msgs) => {
              await updateStarPowerMessages(brawlStarsId, itemId, msgs);
              loadDetail();
            }}
            onUploadImage={async (itemId, file) => {
              await uploadStarPowerImage(brawlStarsId, itemId, file);
              loadDetail();
            }}
          />
        </TabsContent>

        <TabsContent value="gadgets">
          <SubItemSection
            title="가젯"
            items={detail.gadgets}
            onAdd={async (id) => {
              await createGadget(brawlStarsId, id);
              loadDetail();
            }}
            onSaveMessages={async (itemId, msgs) => {
              await updateGadgetMessages(brawlStarsId, itemId, msgs);
              loadDetail();
            }}
            onUploadImage={async (itemId, file) => {
              await uploadGadgetImage(brawlStarsId, itemId, file);
              loadDetail();
            }}
          />
        </TabsContent>

        <TabsContent value="hyper-charges">
          <SubItemSection
            title="하이퍼차지"
            items={detail.hyperCharges}
            onAdd={async (id) => {
              await createHyperCharge(brawlStarsId, id);
              loadDetail();
            }}
            onSaveMessages={async (itemId, msgs) => {
              await updateHyperChargeMessages(brawlStarsId, itemId, msgs);
              loadDetail();
            }}
            onUploadImage={async (itemId, file) => {
              await uploadHyperChargeImage(brawlStarsId, itemId, file);
              loadDetail();
            }}
          />
        </TabsContent>

        <TabsContent value="gears">
          <Card>
            <CardHeader><CardTitle>기어 연결</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="flex flex-wrap gap-2">
                {detail.allGears.map((gear) => (
                  <Button
                    key={gear.brawlStarsId}
                    variant={selectedGearIds.has(gear.brawlStarsId) ? "default" : "outline"}
                    size="sm"
                    onClick={() => toggleGear(gear.brawlStarsId)}
                  >
                    {gear.brawlStarsId} ({gear.rarity})
                  </Button>
                ))}
              </div>
              <Button onClick={handleSaveGears}>기어 연결 저장</Button>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/app/brawlers/
git commit -m "feat(admin-frontend): 브롤러 상세 페이지 구현"
```

---

### Task 15: 기어 페이지

**Files:**
- Create: `imstargg-admin-frontend/app/gears/page.tsx`
- Create: `imstargg-admin-frontend/app/gears/[brawlStarsId]/edit/page.tsx`

- [ ] **Step 1: 기어 목록 페이지 구현**

`imstargg-admin-frontend/app/gears/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getGears, createGear } from "@/lib/api";
import type { GearListItem } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

const GEAR_RARITIES = ["SUPER_RARE", "EPIC", "MYTHIC"];

export default function GearListPage() {
  const [gears, setGears] = useState<GearListItem[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "" });

  const loadGears = () => getGears().then(setGears);

  useEffect(() => {
    loadGears();
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity) {
      toast.error("모든 필드를 입력해주세요.");
      return;
    }
    try {
      await createGear({ brawlStarsId: id, rarity: form.rarity });
      toast.success("기어가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", rarity: "" });
      loadGears();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">기어 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>기어 등록</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>기어 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>Rarity</Label>
                <Select value={form.rarity} onValueChange={(v) => setForm({ ...form, rarity: v })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {GEAR_RARITIES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <Button onClick={handleCreate} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {gears.map((g) => (
            <TableRow key={g.brawlStarsId}>
              <TableCell>{g.brawlStarsId}</TableCell>
              <TableCell>{g.names.KOREAN || "-"}</TableCell>
              <TableCell>{g.names.ENGLISH || "-"}</TableCell>
              <TableCell>{g.rarity}</TableCell>
              <TableCell>{g.hasImage ? "O" : "X"}</TableCell>
              <TableCell>
                <Link href={`/gears/${g.brawlStarsId}/edit`}>
                  <Button variant="outline" size="sm">편집</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 기어 편집 페이지 구현**

`imstargg-admin-frontend/app/gears/[brawlStarsId]/edit/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getGears, updateGearMessages, uploadGearImage } from "@/lib/api";
import type { GearListItem } from "@/lib/types";
import { toast } from "sonner";

export default function GearEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [gear, setGear] = useState<GearListItem | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");

  const loadGear = () => {
    getGears().then((gears) => {
      const found = gears.find((g) => g.brawlStarsId === brawlStarsId);
      if (found) {
        setGear(found);
        setKo(found.names.KOREAN || "");
        setEn(found.names.ENGLISH || "");
      }
    });
  };

  useEffect(() => {
    loadGear();
  }, [brawlStarsId]);

  if (!gear) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  const handleSaveMessages = async () => {
    try {
      await updateGearMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadGear();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadGearImage(brawlStarsId, file);
      toast.success("이미지가 업로드되었습니다.");
      loadGear();
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">기어 편집 - {brawlStarsId}</h2>

      <div className="space-y-6">
        <Card>
          <CardHeader><CardTitle>이름</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label>이름 (KO)</Label>
              <Input value={ko} onChange={(e) => setKo(e.target.value)} />
            </div>
            <div>
              <Label>이름 (EN)</Label>
              <Input value={en} onChange={(e) => setEn(e.target.value)} />
            </div>
            <Button onClick={handleSaveMessages}>이름 저장</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>이미지</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {gear.hasImage ? (
              <p className="text-sm text-green-600">이미지 등록됨</p>
            ) : (
              <p className="text-sm text-muted-foreground">이미지 없음</p>
            )}
            <Input type="file" accept="image/*" onChange={handleUpload} />
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
```

- [ ] **Step 3: 커밋**

```bash
git add imstargg-admin-frontend/app/gears/
git commit -m "feat(admin-frontend): 기어 목록/편집 페이지 구현"
```

---

### Task 16: 이벤트 페이지

**Files:**
- Create: `imstargg-admin-frontend/app/events/page.tsx`
- Create: `imstargg-admin-frontend/app/events/[brawlStarsId]/edit/page.tsx`

- [ ] **Step 1: 이벤트 목록 페이지 구현**

`imstargg-admin-frontend/app/events/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getEvents, getEventModes, createEvent } from "@/lib/api";
import type { BattleEvent, BattleEventMode } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

export default function EventListPage() {
  const [events, setEvents] = useState<BattleEvent[]>([]);
  const [modes, setModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", modeId: "", mapName: "" });

  const loadData = () => {
    getEvents().then(setEvents);
    getEventModes().then(setModes);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    try {
      await createEvent({
        brawlStarsId: id,
        modeId: form.modeId ? Number(form.modeId) : undefined,
        mapName: form.mapName || undefined,
      });
      toast.success("이벤트가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", modeId: "", mapName: "" });
      loadData();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>이벤트 등록</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>이벤트 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>Mode (선택)</Label>
                <Select value={form.modeId} onValueChange={(v) => setForm({ ...form, modeId: v })}>
                  <SelectTrigger><SelectValue placeholder="선택 안 함" /></SelectTrigger>
                  <SelectContent>
                    {modes.map((m) => (
                      <SelectItem key={m.brawlStarsId} value={String(m.brawlStarsId)}>
                        {m.brawlStarsId} - {m.brawlStarsName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Map Name (선택)</Label>
                <Input
                  value={form.mapName}
                  onChange={(e) => setForm({ ...form, mapName: e.target.value })}
                />
              </div>
              <Button onClick={handleCreate} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>Mode</TableHead>
            <TableHead>Map (원본)</TableHead>
            <TableHead>Map Name (KO)</TableHead>
            <TableHead>Map Name (EN)</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((e) => (
            <TableRow key={e.brawlStarsId}>
              <TableCell>{e.brawlStarsId}</TableCell>
              <TableCell>{e.modeBrawlStarsName || "-"}</TableCell>
              <TableCell>{e.mapName || "-"}</TableCell>
              <TableCell>{e.mapNames.KOREAN || "-"}</TableCell>
              <TableCell>{e.mapNames.ENGLISH || "-"}</TableCell>
              <TableCell>
                {e.mapName ? (
                  <Link href={`/events/${e.brawlStarsId}/edit`}>
                    <Button variant="outline" size="sm">편집</Button>
                  </Link>
                ) : (
                  <Button variant="outline" size="sm" disabled>편집 불가</Button>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 이벤트 편집 페이지 구현**

`imstargg-admin-frontend/app/events/[brawlStarsId]/edit/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getEvents, updateEventMessages } from "@/lib/api";
import type { BattleEvent } from "@/lib/types";
import { toast } from "sonner";

export default function EventEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [event, setEvent] = useState<BattleEvent | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");

  const loadEvent = () => {
    getEvents().then((events) => {
      const found = events.find((e) => e.brawlStarsId === brawlStarsId);
      if (found) {
        setEvent(found);
        setKo(found.mapNames.KOREAN || "");
        setEn(found.mapNames.ENGLISH || "");
      }
    });
  };

  useEffect(() => {
    loadEvent();
  }, [brawlStarsId]);

  if (!event) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  const handleSave = async () => {
    try {
      await updateEventMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("맵 이름이 저장되었습니다.");
      loadEvent();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">맵 이름 편집 - {event.mapName}</h2>

      <Card>
        <CardHeader><CardTitle>맵 이름</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div>
            <Label>이름 (KO)</Label>
            <Input value={ko} onChange={(e) => setKo(e.target.value)} />
          </div>
          <div>
            <Label>이름 (EN)</Label>
            <Input value={en} onChange={(e) => setEn(e.target.value)} />
          </div>
          <Button onClick={handleSave}>저장</Button>
        </CardContent>
      </Card>
    </AdminLayout>
  );
}
```

- [ ] **Step 3: 커밋**

```bash
git add imstargg-admin-frontend/app/events/
git commit -m "feat(admin-frontend): 이벤트 목록/편집 페이지 구현"
```

---

### Task 17: 이벤트 모드 페이지

**Files:**
- Create: `imstargg-admin-frontend/app/event-modes/page.tsx`
- Create: `imstargg-admin-frontend/app/event-modes/[brawlStarsId]/edit/page.tsx`

- [ ] **Step 1: 이벤트 모드 목록 페이지 구현**

`imstargg-admin-frontend/app/event-modes/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getEventModes, createEventMode } from "@/lib/api";
import type { BattleEventMode } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

export default function EventModeListPage() {
  const [modes, setModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", brawlStarsName: "" });

  const loadModes = () => getEventModes().then(setModes);

  useEffect(() => {
    loadModes();
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.brawlStarsName) {
      toast.error("모든 필드를 입력해주세요.");
      return;
    }
    try {
      await createEventMode({ brawlStarsId: id, brawlStarsName: form.brawlStarsName });
      toast.success("이벤트 모드가 등록되었습니다.");
      setOpen(false);
      setForm({ brawlStarsId: "", brawlStarsName: "" });
      loadModes();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 모드 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>이벤트 모드 등록</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>이벤트 모드 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>BrawlStars Name</Label>
                <Input
                  value={form.brawlStarsName}
                  onChange={(e) => setForm({ ...form, brawlStarsName: e.target.value })}
                />
              </div>
              <Button onClick={handleCreate} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>BrawlStars Name</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>아이콘</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {modes.map((m) => (
            <TableRow key={m.brawlStarsId}>
              <TableCell>{m.brawlStarsId}</TableCell>
              <TableCell>{m.brawlStarsName}</TableCell>
              <TableCell>{m.names.KOREAN || "-"}</TableCell>
              <TableCell>{m.names.ENGLISH || "-"}</TableCell>
              <TableCell>{m.hasIcon ? "O" : "X"}</TableCell>
              <TableCell>
                <Link href={`/event-modes/${m.brawlStarsId}/edit`}>
                  <Button variant="outline" size="sm">편집</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 이벤트 모드 편집 페이지 구현**

`imstargg-admin-frontend/app/event-modes/[brawlStarsId]/edit/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getEventModes, updateEventModeMessages, uploadEventModeImage } from "@/lib/api";
import type { BattleEventMode } from "@/lib/types";
import { toast } from "sonner";

export default function EventModeEditPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [mode, setMode] = useState<BattleEventMode | null>(null);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");

  const loadMode = () => {
    getEventModes().then((modes) => {
      const found = modes.find((m) => m.brawlStarsId === brawlStarsId);
      if (found) {
        setMode(found);
        setKo(found.names.KOREAN || "");
        setEn(found.names.ENGLISH || "");
      }
    });
  };

  useEffect(() => {
    loadMode();
  }, [brawlStarsId]);

  if (!mode) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  const handleSaveMessages = async () => {
    try {
      await updateEventModeMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadMode();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      await uploadEventModeImage(brawlStarsId, file);
      toast.success("아이콘이 업로드되었습니다.");
      loadMode();
    } catch {
      toast.error("업로드에 실패했습니다.");
    }
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">이벤트 모드 편집 - {mode.brawlStarsName}</h2>

      <div className="space-y-6">
        <Card>
          <CardHeader><CardTitle>이름</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div>
              <Label>이름 (KO)</Label>
              <Input value={ko} onChange={(e) => setKo(e.target.value)} />
            </div>
            <div>
              <Label>이름 (EN)</Label>
              <Input value={en} onChange={(e) => setEn(e.target.value)} />
            </div>
            <Button onClick={handleSaveMessages}>이름 저장</Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>아이콘</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {mode.hasIcon ? (
              <p className="text-sm text-green-600">아이콘 등록됨</p>
            ) : (
              <p className="text-sm text-muted-foreground">아이콘 없음</p>
            )}
            <Input type="file" accept="image/*" onChange={handleUpload} />
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
```

- [ ] **Step 3: 커밋**

```bash
git add imstargg-admin-frontend/app/event-modes/
git commit -m "feat(admin-frontend): 이벤트 모드 목록/편집 페이지 구현"
```

---

### Task 18: API 점검 관리 페이지

**Files:**
- Create: `imstargg-admin-frontend/app/maintenances/page.tsx`

- [ ] **Step 1: 점검 관리 페이지 구현**

`imstargg-admin-frontend/app/maintenances/page.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getMaintenances, registerMaintenance, deleteMaintenance } from "@/lib/api";
import type { Maintenance } from "@/lib/types";
import { toast } from "sonner";

export default function MaintenancePage() {
  const [maintenances, setMaintenances] = useState<Maintenance[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ startTime: "", endTime: "" });

  const loadMaintenances = () => getMaintenances().then(setMaintenances);

  useEffect(() => {
    loadMaintenances();
  }, []);

  const handleRegister = async () => {
    if (!form.startTime || !form.endTime) {
      toast.error("시작 시간과 종료 시간을 입력해주세요.");
      return;
    }
    const start = new Date(form.startTime);
    const end = new Date(form.endTime);
    if (end <= start) {
      toast.error("종료 시간은 시작 시간 이후여야 합니다.");
      return;
    }
    try {
      await registerMaintenance({
        startTime: start.toISOString(),
        endTime: end.toISOString(),
      });
      toast.success("점검이 등록되었습니다.");
      setOpen(false);
      setForm({ startTime: "", endTime: "" });
      loadMaintenances();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await deleteMaintenance(id);
      toast.success("삭제되었습니다.");
      loadMaintenances();
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  const formatDateTime = (iso: string) => {
    return new Date(iso).toLocaleString("ko-KR");
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">API 점검 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button>점검 등록</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>점검 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>시작 시간</Label>
                <Input
                  type="datetime-local"
                  value={form.startTime}
                  onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                />
              </div>
              <div>
                <Label>종료 시간</Label>
                <Input
                  type="datetime-local"
                  value={form.endTime}
                  onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                />
              </div>
              <Button onClick={handleRegister} className="w-full">등록</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>ID</TableHead>
            <TableHead>시작 시간</TableHead>
            <TableHead>종료 시간</TableHead>
            <TableHead>생성일</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {maintenances.map((m) => (
            <TableRow key={m.id}>
              <TableCell>{m.id}</TableCell>
              <TableCell>{formatDateTime(m.startTime)}</TableCell>
              <TableCell>{formatDateTime(m.endTime)}</TableCell>
              <TableCell>{m.createdAt ? formatDateTime(m.createdAt) : "-"}</TableCell>
              <TableCell>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => handleDelete(m.id)}
                >
                  삭제
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/app/maintenances/
git commit -m "feat(admin-frontend): API 점검 관리 페이지 구현"
```

---

### Task 19: 최종 검증

**Files:**
- No new files (verification only)

- [ ] **Step 1: Backend 전체 빌드**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Backend ktlint 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-admin:ktlintCheck
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Frontend 빌드 확인**

```bash
cd imstargg-admin-frontend && pnpm build
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Frontend lint 확인**

```bash
cd imstargg-admin-frontend && pnpm lint
```

Expected: No errors
