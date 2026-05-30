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

## Secrets

Do not commit secrets to this directory. Values files may reference existing
Kubernetes Secrets, but secret material must stay outside git.
