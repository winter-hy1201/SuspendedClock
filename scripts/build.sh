#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$task_root"
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Library/Android/sdk" ]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with platform 36 and build-tools 36.0.0}"
bash scripts/check-core.sh
./gradlew --console=plain "$@" :app:assembleDebug :app:lintDebug
mkdir -p releases
cp app/build/outputs/apk/debug/app-debug.apk releases/offline-clock-0.2.1-debug.apk
python3 scripts/check-apk.py releases/offline-clock-0.2.1-debug.apk --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt"
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify releases/offline-clock-0.2.1-debug.apk
shasum -a 256 releases/offline-clock-0.2.1-debug.apk > releases/SHA256SUMS
