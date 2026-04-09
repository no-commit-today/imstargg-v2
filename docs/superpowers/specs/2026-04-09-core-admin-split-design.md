# core-admin Frontend/Backend 분리 설계

## 개요

기존 Vaadin 기반 core-admin 모듈을 REST API 백엔드로 전환하고, 별도의 Next.js + shadcn/ui 프론트엔드(imstargg-admin-frontend)를 생성한다.

## 요구사항

- core-admin: Vaadin 제거, REST API 서버로 전환. 기존 기능 모두 지원.
- imstargg-admin-frontend: Next.js + shadcn/ui. 단순하고 깔끔한 관리자 UI.
- 조회와 수정을 명확하게 분리.
- 인증/인가 없음 (단독 사용).
- JPA 엔티티를 도메인 엔티티로 직접 사용 (core-api와 다른 패턴).
- 도메인 단위로 트랜잭션을 적절히 묶기.

---

## Backend (core-admin API)

### 구조

```
core-admin/src/main/kotlin/com/imstargg/core/admin/
├── CoreAdminApplication.kt
├── controller/
│   └── v1/
│       ├── BrawlerAdminController.kt
│       ├── GearAdminController.kt
│       ├── BattleEventAdminController.kt
│       ├── BattleEventModeAdminController.kt
│       ├── MaintenanceAdminController.kt
│       ├── DashboardAdminController.kt
│       ├── request/
│       │   ├── BrawlerCreateRequest.kt
│       │   ├── MessagesUpdateRequest.kt
│       │   ├── GearCreateRequest.kt
│       │   ├── BattleEventCreateRequest.kt
│       │   ├── BattleEventModeCreateRequest.kt
│       │   ├── MaintenanceRegisterRequest.kt
│       │   ├── BrawlerStarPowerCreateRequest.kt
│       │   ├── BrawlerGadgetCreateRequest.kt
│       │   ├── BrawlerHyperChargeCreateRequest.kt
│       │   └── BrawlerGearsUpdateRequest.kt
│       └── response/
│           ├── BrawlerAdminResponse.kt
│           ├── BrawlerDetailAdminResponse.kt
│           ├── GearAdminResponse.kt
│           ├── BattleEventAdminResponse.kt
│           ├── BattleEventModeAdminResponse.kt
│           ├── MaintenanceAdminResponse.kt
│           └── DashboardSummaryAdminResponse.kt
├── application/
│   ├── BrawlerAdminService.kt
│   ├── GearAdminService.kt
│   ├── BattleEventAdminService.kt
│   ├── DashboardAdminService.kt
│   ├── MaintenanceAdminService.kt
│   └── ImageStorageService.kt
└── config/
    └── WebConfig.kt (CORS 설정)
```

### API 엔드포인트

#### Dashboard
| Method | Path | 설명 |
|--------|------|------|
| GET | `/v1/dashboard/summary` | 대시보드 데이터 완성도 요약 |

#### Brawler
| Method | Path | 설명 |
|--------|------|------|
| GET | `/v1/brawlers` | 브롤러 목록 |
| GET | `/v1/brawlers/{brawlStarsId}` | 브롤러 상세 (스타파워, 가젯, 하이퍼차지, 기어 포함) |
| POST | `/v1/brawlers` | 브롤러 생성 |
| PUT | `/v1/brawlers/{brawlStarsId}/messages` | 브롤러 이름 수정 (모든 언어 일괄) |
| PUT | `/v1/brawlers/{brawlStarsId}/image` | 브롤러 메인 이미지 업로드 (multipart) |
| PUT | `/v1/brawlers/{brawlStarsId}/buffy-image` | 버피 이미지 업로드 (multipart + buffyType param) |
| PUT | `/v1/brawlers/{brawlStarsId}/gears` | 브롤러-기어 연결 수정 |
| POST | `/v1/brawlers/{brawlStarsId}/star-powers` | 스타파워 추가 |
| PUT | `/v1/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/messages` | 스타파워 이름 수정 |
| PUT | `/v1/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/image` | 스타파워 이미지 업로드 |
| POST | `/v1/brawlers/{brawlStarsId}/gadgets` | 가젯 추가 |
| PUT | `/v1/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/messages` | 가젯 이름 수정 |
| PUT | `/v1/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/image` | 가젯 이미지 업로드 |
| POST | `/v1/brawlers/{brawlStarsId}/hyper-charges` | 하이퍼차지 추가 |
| PUT | `/v1/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/messages` | 하이퍼차지 이름 수정 |
| PUT | `/v1/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/image` | 하이퍼차지 이미지 업로드 |

#### Gear
| Method | Path | 설명 |
|--------|------|------|
| GET | `/v1/gears` | 기어 목록 |
| POST | `/v1/gears` | 기어 생성 |
| PUT | `/v1/gears/{brawlStarsId}/messages` | 기어 이름 수정 |
| PUT | `/v1/gears/{brawlStarsId}/image` | 기어 이미지 업로드 |

#### Event
| Method | Path | 설명 |
|--------|------|------|
| GET | `/v1/events` | 이벤트 목록 |
| POST | `/v1/events` | 이벤트 생성 |
| PUT | `/v1/events/{brawlStarsId}/messages` | 맵 이름 수정 |

#### Event Mode
| Method | Path | 설명 |
|--------|------|------|
| GET | `/v1/event-modes` | 이벤트 모드 목록 |
| POST | `/v1/event-modes` | 모드 생성 |
| PUT | `/v1/event-modes/{brawlStarsId}/messages` | 모드 이름 수정 |
| PUT | `/v1/event-modes/{brawlStarsId}/image` | 모드 아이콘 업로드 |

#### Maintenance
| Method | Path | 설명 |
|--------|------|------|
| GET | `/v1/maintenances` | 유지보수 목록 |
| POST | `/v1/maintenances` | 유지보수 등록 |
| DELETE | `/v1/maintenances/{id}` | 유지보수 삭제 |

### 주요 변경사항

1. **Vaadin 의존성 제거**: build.gradle.kts에서 Vaadin starter 제거, spring-boot-starter-web 유지
2. **UI 패키지 전체 삭제**: `ui/` 패키지 삭제 (MainLayout, 모든 View)
3. **서비스 레이어 재활용**: 기존 application/ 서비스 대부분 유지, 트랜잭션 경계 조정
4. **Request DTO 추가**: 컨트롤러 입력용 request 클래스 신규 작성
5. **Response DTO 조정**: 기존 response 클래스를 API 응답에 맞게 조정
6. **CORS 설정**: admin frontend 개발 서버(localhost:3001) 허용
7. **이미지 업로드**: MultipartFile 기반 엔드포인트

### 트랜잭션 개선

- **메시지 업데이트**: 모든 언어의 메시지를 단일 요청/트랜잭션으로 처리 (기존: 언어별 개별 저장)
- **기어 연결**: 기존 연결 삭제 + 새 연결 추가를 하나의 트랜잭션으로 묶기
- **이미지 업로드**: 이미지 저장(S3) + DB 레코드 업데이트를 하나의 서비스 메서드에서 처리

---

## Frontend (imstargg-admin-frontend)

### 기술 스택

- Next.js (App Router) + TypeScript
- shadcn/ui + Tailwind CSS
- 클라이언트 중심 (관리자 페이지이므로 SSR 불필요)

### 생성 방법

```bash
pnpm create next-app@latest imstargg-admin-frontend --yes
```

이후 shadcn/ui 초기화 및 필요 컴포넌트 설치.

### 페이지 구조

```
app/
├── layout.tsx              # 사이드바 레이아웃
├── page.tsx                # Dashboard (데이터 완성도 요약)
├── brawlers/
│   ├── page.tsx            # 브롤러 목록 (테이블)
│   └── [brawlStarsId]/
│       └── page.tsx        # 브롤러 상세/수정 (탭: 기본정보, 스타파워, 가젯, 하이퍼차지, 기어)
├── gears/
│   ├── page.tsx            # 기어 목록 + 생성 다이얼로그
│   └── [brawlStarsId]/
│       └── edit/
│           └── page.tsx    # 기어 수정 (이름 + 이미지)
├── events/
│   ├── page.tsx            # 이벤트 목록 + 생성 다이얼로그
│   └── [brawlStarsId]/
│       └── edit/
│           └── page.tsx    # 이벤트 맵 이름 수정
├── event-modes/
│   ├── page.tsx            # 이벤트 모드 목록 + 생성 다이얼로그
│   └── [brawlStarsId]/
│       └── edit/
│           └── page.tsx    # 모드 수정 (이름 + 아이콘)
└── maintenances/
    └── page.tsx            # 유지보수 목록 + 등록/삭제
```

### 조회/수정 분리 원칙

- **목록 페이지**: 조회 전용 테이블. 각 행에 "수정" 링크/버튼.
- **상세/수정 페이지**: 현재 값 표시 + 편집 폼이 명확히 구분됨.
- **생성**: 목록 페이지의 다이얼로그 (단순 필드) 또는 별도 페이지 (복잡한 경우).
- **삭제**: 확인 다이얼로그 후 실행.

### UI 구성

- **레이아웃**: shadcn sidebar + 상단 헤더
- **테이블**: shadcn Table 컴포넌트
- **폼**: shadcn Form + Input + Select
- **다이얼로그**: shadcn Dialog
- **이미지 업로드**: 파일 input + 미리보기
- **알림**: shadcn Toast (성공/실패 피드백)

### API 클라이언트

- fetch 기반 간단한 API 클라이언트
- 환경변수로 API base URL 설정 (`NEXT_PUBLIC_API_URL`)

---

## 배포/설정 고려사항

- core-admin API: 기존 포트 유지 또는 별도 포트
- admin frontend: 개발 시 localhost:3001 (imstargg-frontend와 포트 구분)
- CORS: core-admin에서 admin frontend origin 허용
