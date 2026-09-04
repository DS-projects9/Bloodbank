#!/usr/bin/env bash
# Starts the MedKeen local stack and fully detaches the backend so it
# keeps running after this script (and the calling shell) exits.
set -u
MEDKEEN_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$MEDKEEN_DIR/.devlogs"
mkdir -p "$LOG_DIR"

export JAVA_HOME=/home/botbox/.local/jdk/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"

# 1) Postgres + MinIO (if not already up)
( cd "$MEDKEEN_DIR" && docker compose up -d ) >/dev/null 2>&1

# 2) Wait briefly for MinIO
for i in $(seq 1 30); do
  curl -sf -m 3 http://localhost:9000/minio/health/live >/dev/null 2>&1 && break
  sleep 1
done

# 3) Launch backend fully detached (new session, no inherited fds)
cd "$MEDKEEN_DIR/backend"
setsid bash -c "./gradlew run > \"$LOG_DIR/backend.log\" 2>&1" < /dev/null > /dev/null 2>&1 &
disown

echo "Backend launching. Logs: $LOG_DIR/backend.log"
