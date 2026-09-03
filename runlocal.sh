#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

./gradlew bootBuildImage

# --force-recreate: "rapat:1" is a static tag, so compose won't notice a
# rebuilt image otherwise and would keep the previous container running.
docker compose up --force-recreate
