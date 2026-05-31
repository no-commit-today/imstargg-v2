# imstargg-deploy

GitOps-owned deployment state for imstargg.

## Directory Convention

Deployment values live under:

```text
apps/<env>/<app>/values.yaml
```

The initial environment is `prod`:

```text
apps/prod/core-api/values.yaml
apps/prod/core-worker/values.yaml
apps/prod/core-batch/values.yaml
```

## Image Tags

CI writes immutable image tags in this format:

```text
<app-version>-<git-short-sha>
```

Example:

```text
0.2.0-abcdef1
```

## Version Sources

| App | Source of truth |
|---|---|
| core-api | `imstargg-backend/gradle.properties` `coreApiVersion` |
| core-batch | `imstargg-backend/gradle.properties` `coreBatchVersion` |
| core-worker | `imstargg-backend/gradle.properties` `coreWorkerVersion` |
| imstargg-frontend | `imstargg-frontend/package.json` `version` |

Use the helper script to read the current version:

```bash
./imstargg-deploy/scripts/get-app-version.sh core-api
```

## ECR Lifecycle Policy

ECR repositories (`imstargg/core-api`, `imstargg/core-batch`, `imstargg/core-worker`)
share a single lifecycle policy defined in `ecr/lifecycle-policy.json`:

| Rule | Selection | Action |
|---|---|---|
| 1 | untagged images older than 1 day | expire |
| 2 | tagged images beyond the most recent 10 | expire |

Rule 2 is **count-based, not age-based**: the currently deployed tag is not always
the newest image (e.g. core-batch runs as a low-frequency CronJob), so an age rule
could delete an in-use image. Keeping the last 10 tagged images protects the
deployed revision.

Apply the policy (default profile `nocommittoday`, region `ap-northeast-2`):

```bash
# Dry run — show which images would be expired, without applying
./imstargg-deploy/scripts/apply-ecr-lifecycle-policy.sh --preview

# Apply to all three repositories
./imstargg-deploy/scripts/apply-ecr-lifecycle-policy.sh

# Override the AWS CLI profile
./imstargg-deploy/scripts/apply-ecr-lifecycle-policy.sh my-profile
```

Edit `ecr/lifecycle-policy.json` and re-run the script to change the policy.

> **Multi-arch caveat:** images are currently built for `linux/arm64` only. If
> multi-platform builds are added later, the platform-specific images under a
> manifest list appear as *untagged* — the "expire untagged after 1 day" rule
> would then break manifest lists. Revisit the untagged rule before that switch.

## Secrets

Do not commit secrets to this directory. Values files may reference existing
Kubernetes Secrets, but secret material must stay outside git.
