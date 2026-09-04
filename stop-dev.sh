#!/usr/bin/env bash
# Stops the local MedKeen dev stack started by start-dev.sh:
#   - Firebase emulators
#   - Ktor backend on :9000
#   - adb reverse port forwarding
#
# Usage:  DEVICE=ZA222S4PSG ./stop-dev.sh

MEDKEEN_DIR="$(cd "$(dirname "$0")" && pwd)"
DEVICE="${DEVICE:-ZA222S4PSG}"
JDK21="/home/botbox/.local/jdk/jdk-21"
ANDROID_HOME="${ANDROID_HOME:-/home/botbox/Android/Sdk}"
NODE_BIN="/home/botbox/.nvm/versions/node/v24.18.1/bin"

export JAVA_HOME="$JDK21"
export PATH="$JDK21/bin:$NODE_BIN:$ANDROID_HOME/platform-tools:$PATH"

cd "$MEDKEEN_DIR"

echo "==> Stopping Firebase emulators ..."
firebase emulators:stop || true

echo "==> Stopping backend on :9000 ..."
if command -v fuser >/dev/null 2>&1; then
  fuser -k 9000/tcp || true
else
  PID="$(lsof -ti tcp:9000 2>/dev/null || true)"
  [ -n "$PID" ] && kill $PID || true
fi

echo "==> Removing adb reverse for device $DEVICE ..."
adb -s "$DEVICE" reverse --remove tcp:9000 2>/dev/null || true
adb -s "$DEVICE" reverse --remove tcp:9090 2>/dev/null || true
adb -s "$DEVICE" reverse --remove tcp:9099 2>/dev/null || true

echo "==> Stopped."
