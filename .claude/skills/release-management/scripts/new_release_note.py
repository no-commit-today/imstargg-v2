"""Create a new release note in docs/release/<calver>.md.

Usage:
    python3 new_release_note.py <calver> --apps '<JSON>' [--changed '<JSON list>'] [--date YYYY-MM-DD]
    python3 new_release_note.py <calver> --apps-json <path> ...

Args:
    calver        e.g. 2026.05.1
    --apps        JSON object: {"core-api": "0.1.1", "core-batch": "0.1.0", ...}
    --apps-json   Path to a file containing the JSON object above
    --changed     JSON array of app names that actually changed (subset of --apps).
                  If omitted, treats all apps as changed.
    --date        Release date (default: today)

Output (JSON to stdout):
    {"created": "<absolute path>", "version": "<calver>"}

Exits non-zero if the target file already exists.
"""
from __future__ import annotations

import argparse
import json
from datetime import date
from pathlib import Path

from _config import repo_root


TEMPLATE = """# Release {version}

- **릴리즈 날짜**: {date}

## 포함 앱 버전

| 앱 | 버전 |
|---|---|
{app_table}

## 변경 사항

{changes}

## ⚠️ Breaking Changes

(있을 경우 — API 스펙 변경, 응답 형식 변경 등. 없으면 "없음")

## 마이그레이션 필요 사항

(있을 경우 — DB 스키마 변경, 환경변수 추가, 캐시 키 버전 업 등. 없으면 "없음")

## 롤백 시 주의 사항

(앱별 롤백 가능 여부. forward-only DB 마이그레이션이나 캐시 키 변경 같이 단순 롤백이 안 되는 경우 명시. 없으면 "특이사항 없음")
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("version", help="CalVer like 2026.05.1")
    parser.add_argument("--apps", help='JSON object mapping app -> version')
    parser.add_argument("--apps-json", type=Path, help="Path to JSON file with the apps map")
    parser.add_argument("--changed", help="JSON array of changed app names (subset of --apps)")
    parser.add_argument("--date", default=date.today().isoformat())
    args = parser.parse_args()

    if args.apps_json:
        apps = json.loads(args.apps_json.read_text())
    elif args.apps:
        apps = json.loads(args.apps)
    else:
        parser.error("Either --apps or --apps-json required")

    changed = json.loads(args.changed) if args.changed else list(apps.keys())

    app_table = "\n".join(f"| {name} | {ver} |" for name, ver in apps.items())
    changes = "\n\n".join(
        f"### {name} {apps[name]}\n- (변경 사항)" for name in changed
    ) or "- (변경 사항)"

    note = TEMPLATE.format(
        version=args.version,
        date=args.date,
        app_table=app_table,
        changes=changes,
    )

    out_path = repo_root() / "docs" / "release" / f"{args.version}.md"
    if out_path.exists():
        print(json.dumps({"error": f"{out_path} already exists"}, ensure_ascii=False))
        raise SystemExit(1)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(note)
    print(json.dumps({"created": str(out_path), "version": args.version}, ensure_ascii=False))


if __name__ == "__main__":
    main()
