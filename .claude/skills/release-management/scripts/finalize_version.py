"""Strip -SNAPSHOT from app version.

Usage:
    python3 finalize_version.py <app> [--dry-run]

Output (JSON to stdout):
    {"app": ..., "current": ..., "new": ..., "changed": bool, "written": bool}
"""
from __future__ import annotations

import argparse
import json

from _config import APPS
from bump_version import write_version
from current_versions import get_version


SNAPSHOT_SUFFIX = "-SNAPSHOT"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("app", choices=list(APPS.keys()))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    app = APPS[args.app]
    current = get_version(app)

    if not current.endswith(SNAPSHOT_SUFFIX):
        print(json.dumps({
            "app": args.app,
            "current": current,
            "new": current,
            "changed": False,
            "written": False,
            "note": "Not a SNAPSHOT version",
        }, indent=2, ensure_ascii=False))
        return

    new = current[: -len(SNAPSHOT_SUFFIX)]
    written = False
    if not args.dry_run:
        write_version(app, new)
        written = True

    print(json.dumps({
        "app": args.app,
        "current": current,
        "new": new,
        "changed": True,
        "written": written,
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
