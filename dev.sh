#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Starting Postgres + MinIO via Docker Compose..."
docker compose up -d

echo "==> Waiting for Postgres to be ready..."
for i in $(seq 1 30); do
    if docker compose exec -T postgres pg_isready -U medvault -d medvault >/dev/null 2>&1; then
        echo "    Postgres is ready."
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "    ERROR: Postgres did not become ready in time." >&2
        exit 1
    fi
    sleep 1
done

echo "==> Waiting for MinIO to be ready..."
for i in $(seq 1 30); do
    if curl -sf http://localhost:9000/minio/health/live >/dev/null 2>&1; then
        echo "    MinIO is ready."
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "    ERROR: MinIO did not become ready in time." >&2
        exit 1
    fi
    sleep 1
done

echo "==> Starting backend..."
cd backend
JAVA_HOME="${JAVA_HOME:-/home/botbox/.local/jdk/jdk-21}" ./gradlew run
