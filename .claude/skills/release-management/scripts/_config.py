"""Shared config for release-management scripts.

App definitions, version file mappings, and changed-path → app mappings.
Single source of truth for the skill — keep in sync with docs/release/README.md.
"""
from __future__ import annotations

import subprocess
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class AppDef:
    name: str
    version_file_rel: str          # path relative to repo root
    version_kind: str              # "gradle-property" | "package-json"
    version_key: str               # property key (gradle) or "version" (package.json)


APPS: dict[str, AppDef] = {
    "core-api": AppDef(
        name="core-api",
        version_file_rel="imstargg-backend/gradle.properties",
        version_kind="gradle-property",
        version_key="coreApiVersion",
    ),
    "core-batch": AppDef(
        name="core-batch",
        version_file_rel="imstargg-backend/gradle.properties",
        version_kind="gradle-property",
        version_key="coreBatchVersion",
    ),
    "core-worker": AppDef(
        name="core-worker",
        version_file_rel="imstargg-backend/gradle.properties",
        version_kind="gradle-property",
        version_key="coreWorkerVersion",
    ),
    "frontend": AppDef(
        name="frontend",
        version_file_rel="imstargg-frontend/package.json",
        version_kind="package-json",
        version_key="version",
    ),
}


# (path-prefix, [affected apps]) — first match wins.
# Empty list means "ignored" (e.g., core-admin, admin-frontend).
DIRECT_PATH_MAP: list[tuple[str, list[str]]] = [
    ("imstargg-backend/core/core-api/", ["core-api"]),
    ("imstargg-backend/core/core-batch/", ["core-batch"]),
    ("imstargg-backend/core/core-worker/", ["core-worker"]),
    ("imstargg-backend/core/core-admin/", []),
    ("imstargg-frontend/", ["frontend"]),
    ("imstargg-admin-frontend/", []),
]


# Library / shared backend modules — affect all bootable backend apps.
# core-admin is excluded from version management, so library changes do NOT bump it.
LIBRARY_PATHS: list[str] = [
    "imstargg-backend/infrastructure/",
    "imstargg-backend/support/",
    "imstargg-backend/client/",
    "imstargg-backend/core/core-enum/",
]
LIBRARY_AFFECTS: list[str] = ["core-api", "core-batch", "core-worker"]


def repo_root() -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        capture_output=True, text=True, check=True,
    )
    return Path(result.stdout.strip())


def version_file(app: AppDef) -> Path:
    return repo_root() / app.version_file_rel
