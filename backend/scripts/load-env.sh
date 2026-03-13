#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-"$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE" >&2
  exit 1
fi

normalize_value() {
  local value="$1"
  if [[ ${#value} -ge 2 ]]; then
    if [[ "${value:0:1}" == '"' && "${value: -1}" == '"' ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; then
      value="${value:1:${#value}-2}"
    fi
  fi
  printf '%s' "$value"
}

while IFS= read -r line || [[ -n "$line" ]]; do
  trimmed="${line#"${line%%[![:space:]]*}"}"
  if [[ -z "$trimmed" || "${trimmed:0:1}" == "#" ]]; then
    continue
  fi

  if [[ "$trimmed" != *=* ]]; then
    echo "Invalid .env line: $line" >&2
    exit 1
  fi

  key="${trimmed%%=*}"
  value="${trimmed#*=}"
  key="$(printf '%s' "$key" | xargs)"
  value="$(normalize_value "$value")"

  export "$key=$value"
  echo "Loaded $key"
done < "$ENV_FILE"

echo "Environment loaded from $ENV_FILE"
echo "Tip: use 'source ./scripts/load-env.sh' to keep variables in the current shell."
