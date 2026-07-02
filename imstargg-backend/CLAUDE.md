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
# 로컬 인프라 실행 (PostgreSQL, Valkey(Redis 호환), floci(S3 에뮬레이터))
docker-compose up -d

# 로컬 DB 접속
# URL: jdbc:postgresql://localhost:5432/imstargg
# User: postgres / Password: secret
```

## Project Architecture

멀티모듈 Spring Boot 4 + Kotlin 프로젝트, Java 21 (team-dodn/spring-boot-kotlin-template 기반)

```text
imstargg-backend/
├── core/                      # 핵심 도메인 모듈
│   ├── core-api/             # REST API 서버 (bootJar 실행 가능)
│   ├── core-admin/           # 관리자 API 서버 (bootJar 실행 가능)
│   ├── core-worker/          # 백그라운드 워커 (bootJar 실행 가능)
│   ├── core-batch/           # 배치 작업 (bootJar 실행 가능)
│   └── core-enum/            # 공유 Enum (외부 모듈에서 참조)
├── infrastructure/
│   ├── db-core/              # JPA + QueryDSL (PostgreSQL)
│   ├── redis-core/           # Redis 캐시/세션
│   ├── aws-s3/               # S3 클라이언트 (이미지 업로드)
│   └── aws-sqs/              # SQS 클라이언트
├── client/
│   └── brawlstars-client/    # BrawlStars API 클라이언트 (OpenFeign)
└── support/
    ├── logging/              # 로깅 (Sentry, Micrometer Tracing)
    └── monitoring/           # 모니터링 (Actuator, Prometheus)
```

### Module Dependencies

- `core-api`, `core-worker`, `core-batch` → `core-enum`, `db-core`, `support/*` (api·worker 는 `redis-core`, `aws-sqs` 추가 의존)
- `core-admin` → `core-enum`, `db-core`, `aws-s3` (redis-core·support 미의존)
- `db-core` → `core-enum`
- `brawlstars-client`: 독립적 (OpenFeign 기반 HTTP 클라이언트)
- **Docker 이미지**: backend 루트 단일 `Dockerfile`(`ARG GRADLE_MODULE`)이 core-api/worker/batch 를 빌드. 모듈을 추가/삭제하면 Dockerfile 의 의존성 prefetch `COPY .../build.gradle.kts` 목록도 함께 갱신해야 캐싱이 유지됨.

### Code Organization

- **파일당 1개 클래스 원칙**: 한 파일에 하나의 클래스(또는 data class)만 선언. 사용자가 명시적으로 지시하거나, 제안 후 승인된 경우에만 예외 허용.
- **도메인 히스토리는 KDoc 으로 보존**: 엔티티/도메인 클래스가 갖는 비자명한 집계 규칙·정합성 제약·"왜 이렇게 안 했는지" 같은 결정은 클래스/메서드 KDoc 으로 남긴다. 향후 유사 변경을 시도하는 사람이 코드만 읽고도 의도를 알 수 있어야 한다. (예: `BrawlerBattleRankStatsEntity` 가 자기 팀만 집계하는 이유)

### API 경로 컨벤션

- `/api` prefix 사용하지 않음 (예: `/v1/brawlers`, `/v1/events/rotation`)
- ID가 뒤따르지 않는 경로는 단수형 사용 (예: `/v1/brawler/search`, `/v1/event/search`)

### Key Patterns

- **패키지**: `com.imstargg.*`
- **테스트 베이스 클래스**: `CoreApiContextTest`, `BrawlStarsClientDevelopTest` 등 상속하여 사용
- **JPA Entity**: allOpen 플러그인 적용 (`@Entity`, `@MappedSuperclass`, `@Embeddable`)
- **엔티티 연관관계**: `@OneToMany` 사용 금지 (자식 컬렉션은 Repository 쿼리로 조회). 같은 도메인 내 자식→부모 참조는 `@ManyToOne(fetch = LAZY)` + `@JoinColumn(foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))` 사용 (DB FK 제약 없음, `optional`은 컬럼 null 여부에 맞춤 — 예: `PlayerBrawlerEntity.player`). 도메인 경계를 넘는 참조는 ID로 참조 (Domain ID Reference 원칙). 호출부가 부모 엔티티 없이 bare ID만 들고 조회해야 하는 경우에 한해 ID 컬럼 참조 허용.
- **QueryDSL**: KSP 코드 생성 (`infrastructure/db-core`). Q-Class는 로컬 변수에 할당하지 않고 static import된 인스턴스를 직접 사용 (예: `statsResultBattleEntity.battleDate`)
- **Application Layer Repository**: `core-api`의 Service는 JPA Repository를 직접 참조하지 않고, `application` 패키지의 Repository 클래스를 통해 접근. 캐시가 필요한 경우 `*WithCache` 래퍼 사용 (예: `EventRepository` → `EventRepositoryWithCache`)
- **Service ↛ Service 의존 금지**: 같은 application 계층의 Service 끼리 직접 참조하지 않는다. 다른 도메인의 데이터가 필요하면 해당 Repository (`*WithCache` 포함) 를 직접 주입해 사용 — Service 가 다른 Service 의 얇은 위임 메서드를 호출하기 위해 늘어나는 것을 막고, 데이터 접근 책임을 Repository 계층에 일관되게 둔다.
- **Domain ID Reference**: 도메인 클래스에서 다른 도메인의 객체는 직접 참조하지 않고 ID로 참조
- **QueryComposer**: `api/query/` 패키지의 `@Component`. 조회 API에서 서로 다른 도메인의 데이터를 조합하여 완성된 Response를 생성하는 읽기 전용 레이어. Controller에서 직접 호출. Repository(`application` 패키지) 를 직접 참조하지 않고 Service 만 의존 — 데이터 접근은 Service 가 책임 (Application Layer Repository 패턴과 일관)
- **Named arguments**: 인자가 세개 이상인 메서드(혹은 생성자)를 호출할 경우 사용

### Cache Key Conventions

- **Redis 키 컨벤션 준수**: 로컬 캐시(Caffeine)에서도 향후 Redis 전환을 고려하여 Redis 키 컨벤션(`:` 구분자) 사용
- **로컬 캐시는 버전 관리 불필요**: Caffeine 로컬 캐시(`brawler`, `battleEvent` 등) 는 프로세스 재시작 시 비워지므로 캐싱 객체 형태가 바뀌어도 자연 무효화된다 → 키에 버전 prefix 를 두지 않는다.
- **Redis 캐시는 메서드별 버전 prefix**: Redis 영속 캐시(`statistics` 등) 는 캐싱 객체 형태 변경 시 역직렬화 오류 방지를 위해 키에 `v{N}:` prefix 를 둔다. `RedisCacheConfiguration.computePrefixWith` 같은 전역 버전이 아니라 **각 `@Cacheable(key=...)` 의 키 prefix 에 메서드 단위로** 둔다. 한 메서드의 반환 객체 구조만 바뀌어도 같은 cacheName 의 다른 메서드 캐시까지 함께 무효화되는 부담을 피하기 위함.
- **키 형식**: `cacheName::v{N}:methodKey:param1:param2:...` (예: `statistics::v1:battleEventBrawlerResult:eventId:tierRange:startDate:endDate`)

### DB & Entity Conventions

- **스키마 관리**: DB 변경사항 발생 시 다음 두 파일에 모두 반영
  - `infrastructure/db-core/src/main/resources/schema.sql`: 전체 현재 스키마 스냅샷 (신규 환경 프로비저닝용)
  - `infrastructure/db-core/src/main/resources/db/migration/V{yyMMdd}__{title}.sql`: 증분 마이그레이션 파일 (운영 DB에 수동 적용할 DDL/DML — Flyway 등 자동 마이그레이션 미사용)
    - 예: `V260420__add_buffy_table.sql`
    - 같은 날 여러 변경이 있으면 title로 구분 (필요 시 `V260420a__...`, `V260420b__...` 형태로 순서를 드러내도 됨)
    - DDL은 `create ... if not exists`, DML은 `on conflict ... do nothing` 등 **멱등적**으로 작성
- **@Table 인덱스 명시**: schema.sql 의 인덱스(PK 제외)는 엔티티 `@Table` 어노테이션에도 모두 반영한다. 일반 인덱스(`ix_`)는 `indexes = [Index(...)]`, 유니크 인덱스(`uk_`)는 `@Index(unique = true)` 또는 `uniqueConstraints = [UniqueConstraint(...)]` (둘 다 동등 — 통계 모듈의 복합 컬럼 유니크는 `UniqueConstraint` 관행). 정렬은 `columnList = "col desc"` 로 표현. `varchar_pattern_ops` 등 JPA `@Index` 로 표현 불가한 연산자 클래스/표현식 인덱스는 `columnList` 에 컬럼명만 두고 주석으로 연산자 클래스를 명시한다 (`ddl-auto: none` 이라 어노테이션은 문서화 목적).
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
- `prod`: 프로덕션 배포

## Test Code Conventions

### 검증 라이브러리
- AssertJ 사용 (`assertThat()`)

### 테스트 클래스 구조
- `@Nested` 클래스 사용 기준:
  - 메서드가 여러 개여서 테스트 구분이 어려운 경우 → 메서드명을 `@Nested` 클래스명으로 사용
  - 메서드 구현이 복잡하여 여러 조건이 발생하는 경우 → 조건을 클래스명(한글)으로 하는 등 유연하게 대응
- 테스트 메서드명은 한글로 작성(백틱\`을 사용하면 언더바'_' 없이 공백으로 구분 가능)
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
- 통합 테스트 정리: `CleanUp.all()` 호출로 전체 테이블 TRUNCATE (데이터 삽입은 `JpaRepository.save()` 사용)

### Service/Component 테스트 의존성 처리
의존성 종류별로 처리 방식을 가른다 — stub-and-verify(`every`/`verify`)는 호출 결합도만 검증해 행위 검증이 약하고 프로덕션 동작과 어긋날 위험이 있다.
- **내부 데이터 계층** (application 패키지의 Repository, JPA wrapper): 실 DB (`CoreApiContextTest` + `CleanUp.all()` + `JpaRepository.save()` 로 데이터 주입)
- **외부 시스템** (HTTP client, 외부 API, queue producer 등 side-effect): MockK
- **시간**: `Clock.fixed(...)` 주입

### 예외 검증
- `catchThrowableOfType(Class, ThrowingCallable)` 사용 (Class가 첫 번째 파라미터)
- ❌ `catchThrowableOfType(ThrowingCallable, Class)` - deprecated, 사용 금지
