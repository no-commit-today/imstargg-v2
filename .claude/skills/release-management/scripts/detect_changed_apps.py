"""Detect which apps are affected by current changes.

Usage:
    python3 detect_changed_apps.py [--base BASE]

Args:
    --base BASE   Base ref to diff against (default: origin/main).

Output (JSON to stdout):
    {
      "base": "origin/main",
      "base_resolved": true,
      "apps": ["core-api", "frontend"],
      "library_changes": ["imstargg-backend/support/monitoring/..."],
      "ignored_changes": ["imstargg-backend/core/core-admin/..."]
    }

Includes both committed changes (BASE..HEAD) and uncommitted (working tree + staged)
so the skill can be invoked mid-work or after commit.

The default is the remote-tracking ref, not the local `main` branch: worktrees and
long-lived checkouts often leave local `main` behind, and since the diff is taken
from the merge-base, a stale base reports everything accumulated on develop as
changed. `base_resolved: false` means the ref was not found and only uncommitted
changes were inspected — committed work is invisible in that case, so bump results
are not trustworthy until a valid base is given.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys

from _config import DIRECT_PATH_MAP, LIBRARY_AFFECTS, LIBRARY_PATHS, repo_root


def _git_diff_names(args: list[str]) -> list[str]:
    result = subprocess.run(
        ["git", "diff", "--name-only", *args],
        capture_output=True, text=True, check=True, cwd=repo_root(),
    )
    return [line for line in result.stdout.splitlines() if line]


def changed_files(base: str) -> tuple[list[str], bool]:
    files: set[str] = set()
    # Committed changes since base.
    base_resolved = True
    try:
        files.update(_git_diff_names([f"{base}...HEAD"]))
    except subprocess.CalledProcessError:
        # base may not exist locally; fall back to current diff only.
        base_resolved = False
    # Uncommitted (working tree vs HEAD, includes staged).
    files.update(_git_diff_names(["HEAD"]))
    # Untracked (newly added files not yet staged).
    untracked = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard"],
        capture_output=True, text=True, check=True, cwd=repo_root(),
    )
    files.update(line for line in untracked.stdout.splitlines() if line)
    return sorted(files), base_resolved


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


def _warn_unresolved_base(base: str) -> None:
    print(
        f"warning: base ref '{base}' not found — only uncommitted changes were "
        f"inspected. Fetch it (git fetch origin main) or pass --base <ref>.",
        file=sys.stderr,
    )


def main():
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--base", default="origin/main", help="Base ref (default: origin/main)"
    )
    args = parser.parse_args()

    files, base_resolved = changed_files(args.base)
    if not base_resolved:
        _warn_unresolved_base(args.base)
    result = {"base": args.base, "base_resolved": base_resolved, **map_to_apps(files)}
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
