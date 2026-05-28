#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: get-app-version.sh <app>

Supported apps:
  core-api
  core-batch
  core-worker
  imstargg-frontend
USAGE
}

if [ "$#" -ne 1 ]; then
  usage
  exit 64
fi

app="$1"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "${script_dir}/../.." && pwd)"

read_gradle_property() {
  local properties_file="$1"
  local property_name="$2"

  if [ ! -f "$properties_file" ]; then
    echo "gradle.properties not found: ${properties_file}" >&2
    exit 66
  fi

  awk -F '=' -v key="$property_name" '
    /^[[:space:]]*(#|$)/ {
      next
    }

    {
      name = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)

      if (name == key) {
        value = substr($0, index($0, "=") + 1)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        print value
        found = 1
        exit
      }
    }

    END {
      if (!found) {
        exit 1
      }
    }
  ' "$properties_file"
}

read_package_json_version() {
  local package_json="$1"

  if [ ! -f "$package_json" ]; then
    echo "package.json not found: ${package_json}" >&2
    exit 66
  fi

  if command -v node >/dev/null 2>&1; then
    node -e '
      const fs = require("fs");
      const packageJsonPath = process.argv[1];
      const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, "utf8"));

      if (typeof packageJson.version !== "string" || packageJson.version.length === 0) {
        process.exit(1);
      }

      process.stdout.write(`${packageJson.version}\n`);
    ' "$package_json"
    return
  fi

  awk '
    /^[[:space:]]*"version"[[:space:]]*:/ {
      value = $0
      sub(/^[[:space:]]*"version"[[:space:]]*:[[:space:]]*"/, "", value)
      sub(/".*$/, "", value)
      print value
      found = 1
      exit
    }

    END {
      if (!found) {
        exit 1
      }
    }
  ' "$package_json"
}

case "$app" in
  core-api)
    version="$(read_gradle_property "${repo_dir}/imstargg-backend/gradle.properties" "coreApiVersion")" || {
      echo "Version property not found: coreApiVersion" >&2
      exit 66
    }
    ;;
  core-batch)
    version="$(read_gradle_property "${repo_dir}/imstargg-backend/gradle.properties" "coreBatchVersion")" || {
      echo "Version property not found: coreBatchVersion" >&2
      exit 66
    }
    ;;
  core-worker)
    version="$(read_gradle_property "${repo_dir}/imstargg-backend/gradle.properties" "coreWorkerVersion")" || {
      echo "Version property not found: coreWorkerVersion" >&2
      exit 66
    }
    ;;
  imstargg-frontend)
    version="$(read_package_json_version "${repo_dir}/imstargg-frontend/package.json")" || {
      echo "Package version not found: imstargg-frontend/package.json" >&2
      exit 66
    }
    ;;
  *)
    echo "Unsupported app: ${app}" >&2
    usage
    exit 64
    ;;
esac

if [ -z "$version" ]; then
  echo "Version is empty for app: ${app}" >&2
  exit 66
fi

printf '%s\n' "$version"
