# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build
./gradlew build                              # 전체 빌드 (테스트 포함)
./gradlew build -x test                      # 테스트 제외 빌드

# Lint
./gradlew ktlintCheck                        # Kotlin 린트 검사
./gradlew ktlintFormat                       # 린트 자동 수정

# Test
./gradlew test                               # CI용 테스트 (develop 태그 제외)
./gradlew unitTest                           # 단위 테스트 (context, develop 태그 제외)
./gradlew contextTest                        # Spring Context 통합 테스트
./gradlew developTest                        # 개발용 테스트 (CI에서 실행 안됨)

# 단일 테스트 실행
./gradlew :core:core-api:test --tests "com.imstargg.SomeTest"

# 실행 (실행 가능 모듈: core-api, core-admin, core-worker, core-batch)
./gradlew :core:core-api:bootRun
./gradlew :core:core-admin:bootRun
./gradlew :core:core-worker:bootRun
./gradlew :core:core-batch:bootRun
```

## Local Development

```bash
# 로컬 인프라 실행 (MySQL, Redis)
docker-compose up -d

# 로컬 DB 접속
# URL: jdbc:mysql://localhost:3306/imstargg
# User: root / Password: secret
```

## Project Architecture

멀티모듈 Spring Boot Kotlin 프로젝트 (team-dodn/spring-boot-kotlin-template 기반)

```text
imstargg-backend/
├── core/                      # 핵심 도메인 모듈
│   ├── core-api/             # REST API 서버 (bootJar 실행 가능)
│   ├── core-admin/           # Vaadin 기반 관리자 UI (bootJar 실행 가능)
│   ├── core-worker/          # 백그라운드 워커 (bootJar 실행 가능)
│   ├── core-batch/           # 배치 작업 (bootJar 실행 가능)
│   └── core-enum/            # 공유 Enum (외부 모듈에서 참조)
├── infrastructure/
│   ├── db-core/              # JPA + QueryDSL (MySQL)
│   └── redis-core/           # Redis 캐시/세션
├── client/
│   └── brawlstars-client/    # BrawlStars API 클라이언트 (OpenFeign)
└── support/
    ├── logging/              # 로깅 (Sentry, Micrometer Tracing)
    └── monitoring/           # 모니터링 (Actuator, Prometheus)
```

### Module Dependencies

- `core-api`, `core-worker`, `core-batch` → `core-enum`, `db-core`, `support/*`
- `db-core` → `core-enum`
- `brawlstars-client`: 독립적 (OpenFeign 기반 HTTP 클라이언트)

### Code Organization

- **파일당 1개 클래스 원칙**: 한 파일에 하나의 클래스(또는 data class)만 선언. 사용자가 명시적으로 지시하거나, 제안 후 승인된 경우에만 예외 허용.

### Key Patterns

- **패키지**: `com.imstargg.*`
- **테스트 베이스 클래스**: `CoreApiContextTest`, `BrawlStarsClientDevelopTest` 등 상속하여 사용
- **JPA Entity**: allOpen 플러그인 적용 (`@Entity`, `@MappedSuperclass`, `@Embeddable`)
- **QueryDSL**: KSP 코드 생성 (`infrastructure/db-core`)
- **Application Layer Repository**: `core-api`의 Service는 JPA Repository를 직접 참조하지 않고, `application` 패키지의 Repository 클래스를 통해 접근. 캐시가 필요한 경우 `*WithCache` 래퍼 사용 (예: `EventRepository` → `EventRepositoryWithCache`)

### DB & Entity Conventions

- **스키마 관리**: DB 변경사항 발생 시 `infrastructure/db-core/src/main/resources/schema.sql`에 반영
- **BaseEntity 상속**: 모든 엔티티는 `BaseEntity` 상속 (createdAt, updatedAt, deleted 필드 포함)
- **변경 가능한 컬럼 패턴**: 생성자 파라미터 → `var` + `protected set`

```kotlin
class SomeEntity(
    @Column(name = "immutable_field", updatable = false, nullable = false)
    val immutableField: String,  // 변경 불가 필드는 val
    mutableField: String,        // 변경 가능 필드는 생성자 파라미터로
) : BaseEntity() {
    @Column(name = "mutable_field", nullable = false)
    var mutableField: String = mutableField
        protected set            // 외부에서 직접 변경 방지
}
```

- **JPA 쿼리 메서드 네이밍**: 리스트 반환 시 `findAll` 접두사 사용 (예: `findAllByEndTimeAfter`), 단건 반환 시 `find` 접두사 사용 (예: `findByBrawlStarsId`)
- **JPQL 지양**: JPQL을 직접 사용하는 것은 최대한 지양. JpaRepository 쿼리 메서드(+EntityGraph)를 최대한 활용하고, 복잡한 쿼리의 경우 QueryDSL 사용

## Test Tags

```kotlin
@Tag("context")  // SpringContext 로드 필요한 통합 테스트
@Tag("develop")  // 개발 중 수동 실행 테스트 (CI 제외)
// 태그 없음     // 단위 테스트
```

## Runtime Profiles

- `local`: 로컬 Docker 인프라 연결 (기본값)
- `local-dev`: DEV 환경 연결
- `dev`: DEV 배포
- `live`: 프로덕션 배포

## Test Code Conventions

### 검증 라이브러리
- AssertJ 사용 (`assertThat()`)

### 테스트 클래스 구조
- `@Nested` 클래스 사용 기준:
  - 메서드가 여러 개여서 테스트 구분이 어려운 경우 → 메서드명을 `@Nested` 클래스명으로 사용
  - 메서드 구현이 복잡하여 여러 조건이 발생하는 경우 → 조건을 클래스명(한글)으로 하는 등 유연하게 대응
- 테스트 메서드명은 한글로 작성
- 테스트 대상 변수명은 `sut` (System Under Test) 사용
- `// given`, `// when`, `// then` 주석 패턴 사용
- 타입은 직접 명시하지 않고 타입 추론 사용

### 테스트 픽스처 패턴
- `{Entity}Fixture` 클래스: `a{Entity}()` → `with{Field}()` → `build()`

```kotlin
// 사용 예시
val entity = aPlayer().withName("test").build()
val sut = SomeClass(entity)
val result = sut.someMethod()
assertThat(result).isEqualTo(expected)
```

### 테스트 베이스 클래스
- Repository: `DbCoreContextTest` (`@DataJpaTest`)
- Service/Component: `CoreApiContextTest` 등 (`@SpringBootTest`)
- 통합 테스트 데이터 관리: `TestDataManager` 클래스 참고

### 예외 검증
- `catchThrowableOfType(Class, ThrowingCallable)` 사용 (Class가 첫 번째 파라미터)
- ❌ `catchThrowableOfType(ThrowingCallable, Class)` - deprecated, 사용 금지
