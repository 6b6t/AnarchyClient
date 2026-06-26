#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
gradle_user_home=""

cleanup() {
  if [[ -n "$gradle_user_home" ]]; then
    rm -rf "$gradle_user_home"
  fi
}
trap cleanup EXIT

new_gradle_cache() {
  cleanup
  gradle_user_home="$(mktemp -d)"
}

tasks=("$@")
if [[ ${#tasks[@]} -eq 0 ]]; then
  tasks=(build)
fi

cd "$repo_root"

new_gradle_cache
echo "Updating dependency verification metadata with a clean Gradle cache..."
GRADLE_USER_HOME="$gradle_user_home" ./gradlew --no-daemon --console=plain --write-verification-metadata sha256 "${tasks[@]}"

new_gradle_cache
echo "Verifying dependency metadata with a second clean Gradle cache..."
GRADLE_USER_HOME="$gradle_user_home" ./gradlew --no-daemon --console=plain "${tasks[@]}"

echo "Dependency verification metadata is up to date."
