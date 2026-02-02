#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BACKEND_PORT=8080

cd "$ROOT_DIR"

"$ROOT_DIR/mvnw" spring-boot:run -Dspring-boot.run.profiles=dev &
BACKEND_PID=$!

cleanup() {
  if kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID"
  fi
}
trap cleanup EXIT

echo "[dev] Waiting for backend on port ${BACKEND_PORT}..."
for _ in {1..60}; do
  if curl -s "http://localhost:${BACKEND_PORT}" >/dev/null 2>&1; then
    echo "[dev] Backend is up."
    break
  fi
  sleep 1
done

cd "$ROOT_DIR/frontend"
if [ ! -d "node_modules" ]; then
  npm install
fi

npm run dev
