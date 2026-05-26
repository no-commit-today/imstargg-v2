# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

브롤스타즈 게임 통계 서비스. 모노레포 구조.

## 도메인 지식 (브롤스타즈)

- **브롤러**: 약 100개 존재하는 플레이어블 캐릭터
- **전투 유형**:
  - 결과형(승/패/무): 주로 3v3, 1v1/2v2/5v5도 존재
  - 순위형(1위~N위): 개인전(1명×10팀), 듀오(2명×5팀), 트리오(3명×4팀)
- **이벤트**: 전투 맵. 총 약 100개 맵 중 로테이션으로 약 10개 활성화 (활성화 시간은 맵마다 상이, 브롤패스 시즌에 따라 변경)
  - 새로운 이벤트가 시즌별로 추가
- **경쟁전(Solo Rank)**: 약 20개 맵, 경쟁전 시즌마다 변경 가능

## 모노레포 구조

```
imstargg-v2/
├── imstargg-backend/           # Spring Boot + Kotlin 백엔드
│   └── core/
│       ├── core-api            # 사용자용 REST API (프론트엔드: imstargg-frontend)
│       ├── core-admin          # 관리자용 REST API (프론트엔드: imstargg-admin-frontend)
│       ├── core-worker         # 백그라운드 워커 (배틀/플레이어 동기화 등)
│       └── core-batch          # 배치 작업 (스케줄링/집계 등)
├── imstargg-frontend/          # Next.js + TypeScript — core-api 클라이언트
└── imstargg-admin-frontend/    # Next.js + TypeScript — core-admin 클라이언트
```

**API 변경 시 동기화**: 백엔드 API 스펙(요청/응답 DTO, 경로, 상태코드 등)이 변경되면 대응되는 프론트엔드에 반드시 반영

- `core-api` 변경 → `imstargg-frontend`
- `core-admin` 변경 → `imstargg-admin-frontend`

각 프로젝트의 상세 가이드는 하위 CLAUDE.md 참조:

- `imstargg-backend/CLAUDE.md` - 백엔드 빌드, 테스트, 아키텍처
- `imstargg-frontend/CLAUDE.md` - 사용자 프론트엔드 개발 가이드
- `imstargg-admin-frontend/CLAUDE.md` - 관리자 프론트엔드 개발 가이드
