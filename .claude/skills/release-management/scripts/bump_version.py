"""Bump app version per SNAPSHOT rules.

Usage:
    python3 bump_version.py <app> <patch|minor|major> [--dry-run] [--prev PREV]

SNAPSHOT rules:
    finalized X.Y.Z + patch  -> X.Y.(Z+1)-SNAPSHOT
    finalized X.Y.Z + minor  -> X.(Y+1).0-SNAPSHOT
    finalized X.Y.Z + major  -> (X+1).0.0-SNAPSHOT
    SNAPSHOT (current_kind) + same_or_smaller  -> no change
    SNAPSHOT (smaller_kind) + larger_kind      -> escalate to larger SNAPSHOT

"Current snapshot kind" inferred from version shape when --prev not given:
    X.0.0-SNAPSHOT  -> major kind
    X.Y.0-SNAPSHOT  -> minor kind  (Y > 0)
    X.Y.Z-SNAPSHOT  -> patch kind  (Z > 0)

Output (JSON to stdout):
    {"app": ..., "current": ..., "new": ..., "changed": bool, "kind": ..., "written": bool}
"""
from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass

from _config import APPS, AppDef, version_file
from current_versions import get_version


VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)(-SNAPSHOT)?$")
KIND_RANK = {"patch": 0, "minor": 1, "major": 2}


@dataclass(frozen=True)
class Version:
    major: int
    minor: int
    patch: int
    snapshot: bool

    @classmethod
    def parse(cls, s: str) -> "Version":
        m = VERSION_RE.match(s)
        if not m:
            raise ValueError(f"Invalid version: {s!r}")
        return cls(
            major=int(m.group(1)),
            minor=int(m.group(2)),
            patch=int(m.group(3)),
            snapshot=bool(m.group(4)),
        )

    def with_snapshot(self) -> str:
        return f"{self.major}.{self.minor}.{self.patch}-SNAPSHOT"


def infer_snapshot_kind(v: Version, prev: Version | None) -> str:
    """Determine the bump kind that produced this SNAPSHOT.

    With prev (the previous finalized): exact answer (compare major/minor/patch).
    Without prev: heuristic from version shape.
    """
    if prev is not None:
        if v.major > prev.major:
            return "major"
        if v.minor > prev.minor:
            return "minor"
        return "patch"
    # Heuristic.
    if v.minor == 0 and v.patch == 0:
        return "major"
    if v.patch == 0:
        return "minor"
    return "patch"


def compute_new_version(current: str, new_kind: str, prev: str | None = None) -> tuple[str, bool, str]:
    """Return (new_version_string, changed, reason)."""
    cur = Version.parse(current)

    if not cur.snapshot:
        # Finalized -> SNAPSHOT
        if new_kind == "patch":
            return f"{cur.major}.{cur.minor}.{cur.patch + 1}-SNAPSHOT", True, "finalized→snapshot patch"
        if new_kind == "minor":
            return f"{cur.major}.{cur.minor + 1}.0-SNAPSHOT", True, "finalized→snapshot minor"
        if new_kind == "major":
            return f"{cur.major + 1}.0.0-SNAPSHOT", True, "finalized→snapshot major"
        raise ValueError(f"Unknown kind: {new_kind}")

    # Already SNAPSHOT
    prev_v = Version.parse(prev) if prev else None
    cur_kind = infer_snapshot_kind(cur, prev_v)

    if KIND_RANK[new_kind] <= KIND_RANK[cur_kind]:
        return current, False, f"already {cur_kind} SNAPSHOT, requested {new_kind} (no escalation)"

    # Escalate
    if new_kind == "minor":
        return f"{cur.major}.{cur.minor + 1}.0-SNAPSHOT", True, f"escalate {cur_kind}→minor"
    if new_kind == "major":
        return f"{cur.major + 1}.0.0-SNAPSHOT", True, f"escalate {cur_kind}→major"
    raise ValueError(f"Cannot escalate to {new_kind}")


def write_gradle_version(app: AppDef, new_version: str) -> None:
    path = version_file(app)
    text = path.read_text()
    pattern = re.compile(
        rf"^({re.escape(app.version_key)}\s*=\s*).+?(\s*)$",
        re.MULTILINE,
    )
    new_text, n = pattern.subn(rf"\g<1>{new_version}\g<2>", text)
    if n != 1:
        raise RuntimeError(f"Failed to replace '{app.version_key}' in {path} (matches={n})")
    path.write_text(new_text)


def write_package_version(app: AppDef, new_version: str) -> None:
    path = version_file(app)
    text = path.read_text()
    # Replace the first "version": "..." occurrence; preserve formatting (don't json.dump).
    pattern = re.compile(r'("version"\s*:\s*)"[^"]+"')
    new_text, n = pattern.subn(rf'\g<1>"{new_version}"', text, count=1)
    if n != 1:
        raise RuntimeError(f"Failed to replace version in {path} (matches={n})")
    path.write_text(new_text)


def write_version(app: AppDef, new_version: str) -> None:
    if app.version_kind == "gradle-property":
        write_gradle_version(app, new_version)
        return
    if app.version_kind == "package-json":
        write_package_version(app, new_version)
        return
    raise RuntimeError(f"Unknown version_kind: {app.version_kind}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("app", choices=list(APPS.keys()))
    parser.add_argument("kind", choices=["patch", "minor", "major"])
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--prev", help="Previous finalized version (improves SNAPSHOT escalation accuracy)")
    args = parser.parse_args()

    app = APPS[args.app]
    current = get_version(app)
    new_version, changed, reason = compute_new_version(current, args.kind, args.prev)

    written = False
    if changed and not args.dry_run:
        write_version(app, new_version)
        written = True

    print(json.dumps({
        "app": args.app,
        "current": current,
        "new": new_version,
        "changed": changed,
        "kind": args.kind,
        "reason": reason,
        "written": written,
    }, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
