"""Print current version of each managed app as JSON.

Usage:
    python3 current_versions.py [--app APP]

Output (JSON to stdout):
    {"core-api": "0.1.0", "core-batch": "0.1.0", "core-worker": "0.1.0", "frontend": "0.1.0"}

Or with --app, just that one app's version as a plain string.
"""
from __future__ import annotations

import argparse
import json
import re

from _config import APPS, AppDef, version_file


def _read_gradle_property(app: AppDef) -> str:
    text = version_file(app).read_text()
    pattern = re.compile(rf"^{re.escape(app.version_key)}\s*=\s*(.+?)\s*$", re.MULTILINE)
    m = pattern.search(text)
    if not m:
        raise RuntimeError(
            f"Property '{app.version_key}' not found in {version_file(app)}"
        )
    return m.group(1).strip()


def _read_package_json(app: AppDef) -> str:
    data = json.loads(version_file(app).read_text())
    if app.version_key not in data:
        raise RuntimeError(
            f"Key '{app.version_key}' not found in {version_file(app)}"
        )
    return data[app.version_key]


def get_version(app: AppDef) -> str:
    if app.version_kind == "gradle-property":
        return _read_gradle_property(app)
    if app.version_kind == "package-json":
        return _read_package_json(app)
    raise RuntimeError(f"Unknown version_kind: {app.version_kind}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--app", choices=list(APPS.keys()), help="Print only one app's version")
    args = parser.parse_args()

    if args.app:
        print(get_version(APPS[args.app]))
        return

    versions = {name: get_version(app) for name, app in APPS.items()}
    print(json.dumps(versions, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
