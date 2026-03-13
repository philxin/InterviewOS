#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env"
JAVA_HOME_VALUE="${JAVA_HOME:-/opt/java/openjdk}"
PROFILE_VALUE=""
SKIP_ENV="false"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      ENV_FILE="$2"
      shift 2
      ;;
    --java-home)
      JAVA_HOME_VALUE="$2"
      shift 2
      ;;
    --profile)
      PROFILE_VALUE="$2"
      shift 2
      ;;
    --skip-env)
      SKIP_ENV="true"
      shift
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Usage: ./scripts/start-dev.sh [--env-file <path>] [--java-home <path>] [--profile <profile>] [--skip-env] [--dry-run]" >&2
      exit 1
      ;;
  esac
done

if [[ "$SKIP_ENV" != "true" ]]; then
  # shellcheck source=/dev/null
  source "$PROJECT_ROOT/scripts/load-env.sh" "$ENV_FILE"
fi

if [[ ! -d "$JAVA_HOME_VALUE" ]]; then
  echo "JAVA_HOME not found: $JAVA_HOME_VALUE" >&2
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_VALUE"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ -n "$PROFILE_VALUE" ]]; then
  export SPRING_PROFILES_ACTIVE="$PROFILE_VALUE"
fi

echo "JAVA_HOME=$JAVA_HOME"
echo "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-}"
echo "Starting Spring Boot from $PROJECT_ROOT"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "Dry run enabled, startup command was not executed."
  exit 0
fi

cd "$PROJECT_ROOT"
./mvnw spring-boot:run
