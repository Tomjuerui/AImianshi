#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BASE_URL=${BASE_URL:-"http://localhost:8080"}
RESUME_FILE="$ROOT_DIR/scripts/demo-resume.txt"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required." >&2
  exit 1
fi

if [ ! -f "$RESUME_FILE" ]; then
  echo "Resume file not found: $RESUME_FILE" >&2
  exit 1
fi

parse_json_data() {
  python -c "import json,sys; print(json.load(sys.stdin)['data'])"
}

echo "[demo] Uploading resume..."
resume_response=$(curl -s -F "file=@${RESUME_FILE}" -F "userId=1" "${BASE_URL}/api/resumes/upload")
resume_id=$(printf '%s' "$resume_response" | parse_json_data)

echo "[demo] Resume ID: $resume_id"

echo "[demo] Creating session..."
create_payload=$(printf '{"resumeId":%s,"durationMinutes":30}' "$resume_id")
session_response=$(curl -s -H "Content-Type: application/json" -d "$create_payload" "${BASE_URL}/api/interview/sessions")
session_id=$(printf '%s' "$session_response" | parse_json_data)

echo "[demo] Session ID: $session_id"

echo "[demo] Request next question..."
next_question=$(curl -s -X POST "${BASE_URL}/api/interview/sessions/${session_id}/next-question")
printf '%s
' "$next_question"

echo "[demo] Send candidate answer..."
turn_payload='{"role":"CANDIDATE","content":"我在项目中负责接口设计，重点优化了查询性能与缓存策略。"}'
turn_response=$(curl -s -H "Content-Type: application/json" -d "$turn_payload" "${BASE_URL}/api/interview/sessions/${session_id}/turns")
printf '%s
' "$turn_response"

echo "[demo] Advance stage..."
curl -s -X POST "${BASE_URL}/api/interview/sessions/${session_id}/stage/next" | cat

echo "[demo] End session..."
curl -s -X POST "${BASE_URL}/api/interview/sessions/${session_id}/end" | cat

echo "[demo] Generate report..."
curl -s -X POST "${BASE_URL}/api/interview/sessions/${session_id}/report" | cat

printf '\n[demo] Done.\n'
