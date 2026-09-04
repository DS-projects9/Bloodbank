#!/usr/bin/env bash
# Starts the full local MedKeen dev stack:
#   - Firebase emulators (Auth 9099, Firestore 9090, Storage 9199)
#   - Ktor backend API on :9000 (talks to the emulators)
#   - adb reverse so the physical device reaches them via 127.0.0.1
#
# Usage:  DEVICE=ZA222S4PSG ./start-dev.sh

MEDKEEN_DIR="$(cd "$(dirname "$0")" && pwd)"
DEVICE="${DEVICE:-ZA222S4PSG}"
JDK21="/home/botbox/.local/jdk/jdk-21"
ANDROID_HOME="${ANDROID_HOME:-/home/botbox/Android/Sdk}"
NODE_BIN="/home/botbox/.nvm/versions/node/v24.18.1/bin"
LOG_DIR="/tmp/medkeen_dev"

export JAVA_HOME="$JDK21"
export PATH="$JDK21/bin:$NODE_BIN:$ANDROID_HOME/platform-tools:$PATH"

mkdir -p "$LOG_DIR"

wait_for_port() {
  local port="$1"
  local i=0
  echo -n "Waiting for localhost:$port ..."
  while ! bash -c "cat < /dev/null > /dev/tcp/127.0.0.1/$port" 2>/dev/null; do
    sleep 2
    i=$((i + 2))
    if [ "$i" -ge 120 ]; then echo " timeout!"; return 1; fi
  done
  echo " ready"
}

cd "$MEDKEEN_DIR"

echo "==> Starting Firebase emulators (auth:9099 firestore:9090 storage:9199) ..."
nohup firebase emulators:start --project medkeen-11c68 \
  --only auth,firestore,storage --non-interactive \
  > "$LOG_DIR/firebase.log" 2>&1 &
echo "    log: $LOG_DIR/firebase.log"

wait_for_port 9090
wait_for_port 9099

echo "==> Starting backend API on :9000 ..."
cd "$MEDKEEN_DIR/backend"
export PORT=9000
export GOOGLE_CLOUD_PROJECT=medkeen-11c68
export FIRESTORE_EMULATOR_HOST=localhost:9090
export FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
export FIREBASE_STORAGE_EMULATOR_HOST=localhost:9199
nohup ./gradlew run > "$LOG_DIR/backend.log" 2>&1 &
echo "    log: $LOG_DIR/backend.log"

wait_for_port 9000

echo "==> Setting up adb reverse for device $DEVICE ..."
adb -s "$DEVICE" reverse tcp:9000 tcp:9000 || true
adb -s "$DEVICE" reverse tcp:9090 tcp:9090 || true
adb -s "$DEVICE" reverse tcp:9099 tcp:9099 || true

echo "==> Done. Local dev stack is running."
