---
description: Release management — bump (develop/feature), prepare (main PR), hotfix (main 직접), status (조회)
argument-hint: '[bump|prepare|hotfix|status] [--auto]'
---

`release-management` 스킬을 다음 인자로 실행: `$ARGUMENTS`

해석 규칙:
- 첫 번째 인자가 `bump` / `prepare` / `hotfix` / `status` 중 하나이면 해당 모드.
- 인자가 비어있으면 현재 브랜치 + git diff 컨텍스트로 모드 자동 판단.
- `--auto` 플래그가 있으면 검수 단계 생략하고 즉시 적용 (단, breaking change 의심 시 검수 강제).

스킬 정의: [.claude/skills/release-management/SKILL.md](.claude/skills/release-management/SKILL.md)
모드별 워크플로 / 엣지 케이스: [.claude/skills/release-management/references/workflow.md](.claude/skills/release-management/references/workflow.md)
SNAPSHOT 매트릭스 / bump 종류 가이드: [.claude/skills/release-management/references/version-rules.md](.claude/skills/release-management/references/version-rules.md)
