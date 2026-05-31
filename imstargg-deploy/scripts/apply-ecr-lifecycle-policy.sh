#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: apply-ecr-lifecycle-policy.sh [--preview] [profile]

Applies the shared ECR lifecycle policy (ecr/lifecycle-policy.json) to the
imstargg core repositories.

Options:
  --preview     Evaluate the policy without applying it. Uses the ECR lifecycle
                policy preview API to show which images would be expired.
  -h, --help    Show this help.

Arguments:
  profile       AWS CLI profile to use (default: nocommittoday)

Repositories:
  imstargg/core-api, imstargg/core-batch, imstargg/core-worker (ap-northeast-2)
USAGE
}

preview=false
profile="nocommittoday"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --preview)
      preview=true
      shift
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage
      exit 64
      ;;
    *)
      profile="$1"
      shift
      ;;
  esac
done

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI not found in PATH" >&2
  exit 69
fi

region="ap-northeast-2"
repos=(imstargg/core-api imstargg/core-batch imstargg/core-worker)

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
policy_file="${script_dir}/../ecr/lifecycle-policy.json"

if [ ! -f "$policy_file" ]; then
  echo "lifecycle policy not found: ${policy_file}" >&2
  exit 66
fi

policy_text="file://${policy_file}"

if [ "$preview" = true ]; then
  for repo in "${repos[@]}"; do
    echo "=== preview: ${repo} ==="
    aws ecr start-lifecycle-policy-preview \
      --repository-name "$repo" \
      --lifecycle-policy-text "$policy_text" \
      --region "$region" \
      --profile "$profile" >/dev/null

    # The preview is evaluated asynchronously; poll until it completes.
    attempts=0
    status="$(aws ecr get-lifecycle-policy-preview \
      --repository-name "$repo" --region "$region" --profile "$profile" \
      --query 'status' --output text)"
    while [ "$status" = "IN_PROGRESS" ] && [ "$attempts" -lt 30 ]; do
      sleep 2
      attempts=$((attempts + 1))
      status="$(aws ecr get-lifecycle-policy-preview \
        --repository-name "$repo" --region "$region" --profile "$profile" \
        --query 'status' --output text)"
    done

    aws ecr get-lifecycle-policy-preview \
      --repository-name "$repo" --region "$region" --profile "$profile" \
      --query '{status: status, expiringImageTotalCount: summary.expiringImageTotalCount, expiring: previewResults[].{appliedRule: appliedRulePriority, tags: imageTags, pushedAt: imagePushedAt}}' \
      --output json
  done
  exit 0
fi

for repo in "${repos[@]}"; do
  echo "=== apply: ${repo} ==="
  aws ecr put-lifecycle-policy \
    --repository-name "$repo" \
    --lifecycle-policy-text "$policy_text" \
    --region "$region" \
    --profile "$profile" \
    --output json
done
