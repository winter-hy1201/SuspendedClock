#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "$0")/.." && pwd)"
task_tmp="$(mktemp -d)"
trap 'rm -rf "$task_tmp"' EXIT
javac -d "$task_tmp" \
  "$task_root/app/src/main/java/local/suspendedclock/Countdown.java" \
  "$task_root/app/src/main/java/local/suspendedclock/TimeText.java" \
  "$task_root/app/src/main/java/local/suspendedclock/MinuteCue.java" \
  "$task_root/tests/CoreChecks.java"
java -cp "$task_tmp" local.suspendedclock.CoreChecks
