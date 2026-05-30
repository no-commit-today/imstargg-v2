#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: update-image-tag.sh <app> <repository> <tag>

Updates the image repository/tag in the prod values.yaml for the given app.

Arguments:
  app         core-api | core-batch | core-worker
  repository  full image repository (e.g. <registry>/imstargg/core-api)
  tag         image tag
USAGE
}

if [ "$#" -ne 3 ]; then
  usage
  exit 64
fi

app="$1"
repository="$2"
tag="$3"

if ! command -v yq >/dev/null 2>&1; then
  echo "yq not found in PATH" >&2
  exit 69
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
prod_dir="$(cd -- "${script_dir}/../apps/prod" && pwd)"

case "$app" in
  core-batch)
    values_file="${prod_dir}/core-batch/values.yaml"
    image_path=".cronjob.image"
    ;;
  core-api | core-worker)
    values_file="${prod_dir}/${app}/values.yaml"
    image_path=".deployment.image"
    ;;
  *)
    echo "Unsupported app: ${app}" >&2
    usage
    exit 64
    ;;
esac

if [ ! -f "$values_file" ]; then
  echo "values.yaml not found: ${values_file}" >&2
  exit 66
fi

yq e "${image_path}.repository = \"${repository}\"" -i "$values_file"
yq e "${image_path}.tag = \"${tag}\"" -i "$values_file"
