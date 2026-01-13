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

# 실행 (실행 가능 모듈: core-api, core-worker, core-batch)
./gradlew :core:core-api:bootRun
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
│   ├── core-worker/          # 백그라운드 워커 (bootJar 실행 가능)
│   ├── core-batch/           # 배치 작업 (bootJar 실행 가능)
│   └── core-enum/            # 공유 Enum (외부 모듈에서 참조)
├── infrastructure/
│   └── db-core/              # JPA + QueryDSL (MySQL)
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

### Key Patterns

- **패키지**: `com.imstargg.*`
- **테스트 베이스 클래스**: `CoreApiContextTest`, `BrawlStarsClientDevelopTest` 등 상속하여 사용
- **JPA Entity**: allOpen 플러그인 적용 (`@Entity`, `@MappedSuperclass`, `@Embeddable`)
- **QueryDSL**: KSP 코드 생성 (`infrastructure/db-core`)

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
