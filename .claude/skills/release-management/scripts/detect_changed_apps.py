"""Detect which apps are affected by current changes.

Usage:
    python3 detect_changed_apps.py [--base BASE]

Args:
    --base BASE   Base ref to diff against (default: main).

Output (JSON to stdout):
    {
      "apps": ["core-api", "frontend"],
      "library_changes": ["imstargg-backend/support/monitoring/..."],
      "ignored_changes": ["imstargg-backend/core/core-admin/..."]
    }

Includes both committed changes (BASE..HEAD) and uncommitted (working tree + staged)
so the skill can be invoked mid-work or after commit.
"""
from __future__ import annotations

import argparse
import json
import subprocess

from _config import DIRECT_PATH_MAP, LIBRARY_AFFECTS, LIBRARY_PATHS, repo_root


def _git_diff_names(args: list[str]) -> list[str]:
    result = subprocess.run(
        ["git", "diff", "--name-only", *args],
        capture_output=True, text=True, check=True, cwd=repo_root(),
    )
    return [line for line in result.stdout.splitlines() if line]


def changed_files(base: str) -> list[str]:
    files: set[str] = set()
    # Committed changes since base.
    try:
        files.update(_git_diff_names([f"{base}...HEAD"]))
    except subprocess.CalledProcessError:
        # base may not exist locally; fall back to current diff only.
        pass
    # Uncommitted (working tree vs HEAD, includes staged).
    files.update(_git_diff_names(["HEAD"]))
    # Untracked (newly added files not yet staged).
    untracked = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard"],
        capture_output=True, text=True, check=True, cwd=repo_root(),
    )
    files.update(line for line in untracked.stdout.splitlines() if line)
    return sorted(files)


def map_to_apps(files: list[str]) -> dict:
    affected: set[str] = set()
    library_changes: list[str] = []
    ignored: list[str] = []

    for f in files:
        matched_direct = False
        for prefix, apps in DIRECT_PATH_MAP:
            if f.startswith(prefix):
                if apps:
                    affected.update(apps)
                else:
                    ignored.append(f)
                matched_direct = True
                break
        if matched_direct:
            continue

        # Library / shared backend module?
        for lib_prefix in LIBRARY_PATHS:
            if f.startswith(lib_prefix):
                library_changes.append(f)
                affected.update(LIBRARY_AFFECTS)
                break

    return {
        "apps": sorted(affected),
        "library_changes": sorted(library_changes),
        "ignored_changes": sorted(ignored),
    }


def main():
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--base", default="main", help="Base ref (default: main)")
    args = parser.parse_args()

    files = changed_files(args.base)
    result = map_to_apps(files)
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
