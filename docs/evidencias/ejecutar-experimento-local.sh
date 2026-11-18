#!/usr/bin/env bash
# =============================================================================
# Local evidence without Docker: launches validation-service and question-service
# with both persistence profiles (jpa/h2 and memory), runs the E2E scenarios with
# curl and stores every response under docs/evidencias/.
#
# Usage:  bash docs/evidencias/ejecutar-experimento-local.sh
# Requirement: JDK 17+ and the three JARs already packaged (mvn package).
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVID="$ROOT/docs/evidencias"
PAYLOADS="$EVID/payloads"
LOGS="$EVID/logs"
mkdir -p "$LOGS"

VALIDATION_JAR="$ROOT/validation-service/target/validation-service-1.0.0.jar"
QUESTION_JAR="$ROOT/question-service/target/question-service-1.0.0.jar"

PIDS=()
cleanup() {
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT

wait_for() {
  local url="$1" name="$2"
  for _ in $(seq 1 90); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "OK   $name listo ($url)"
      return 0
    fi
    sleep 1
  done
  echo "ERROR $name no arrancó ($url)" >&2
  return 1
}

status_of() {
  curl -s -o "$2" -w "%{http_code}" "$1"
}

capture() {
  local label="$1" method="$2" url="$3" body="$4" out="$5"
  local code
  if [ -n "$body" ]; then
    code=$(curl -s -o "$out" -w "%{http_code}" -X "$method" "$url" \
      -H "Content-Type: application/json" --data-binary "@$body")
  else
    code=$(curl -s -o "$out" -w "%{http_code}" -X "$method" "$url")
  fi
  printf '%-38s %-6s %s -> HTTP %s\n' "$label" "$method" "$url" "$code" | tee -a "$EVID/04-resumen-local.txt"
}

: > "$EVID/04-resumen-local.txt"
echo "== Evidencia local (sin Docker) ==" | tee -a "$EVID/04-resumen-local.txt"
date -Iseconds | tee -a "$EVID/04-resumen-local.txt"

# ---------------------------------------------------------------- validation
java -jar "$VALIDATION_JAR" --server.port=8082 > "$LOGS/validation-service.log" 2>&1 &
PIDS+=($!)
wait_for http://localhost:8082/actuator/health validation-service

capture "validacion-valida" POST http://localhost:8082/api/validation/questions \
  "$PAYLOADS/valid.json" "$EVID/00-validacion-valida.json"
capture "validacion-tres-opciones" POST http://localhost:8082/api/validation/questions \
  "$PAYLOADS/three-options.json" "$EVID/00-validacion-tres-opciones.json"

run_profile() {
  local profile="$1" tag="$2"
  local log="$LOGS/question-service-$tag.log"

  java -jar "$QUESTION_JAR" \
    --spring.profiles.active="$profile" \
    --server.port=8083 \
    --spring.rabbitmq.dynamic=false \
    > "$log" 2>&1 &
  local pid=$!
  PIDS+=("$pid")
  wait_for http://localhost:8083/actuator/health "question-service ($profile)"
  grep -m1 "Adaptador de persistencia activo" "$log" | tee -a "$EVID/04-resumen-local.txt"

  local base="http://localhost:8083/api/questions"

  capture "$tag-crear" POST "$base" "$PAYLOADS/valid.json" "$EVID/$tag-curl-crear.json"
  local id
  id=$(grep -o '"id":"[^"]*"' "$EVID/$tag-curl-crear.json" | head -1 | cut -d'"' -f4)
  echo "id generado: $id" | tee -a "$EVID/04-resumen-local.txt"

  capture "$tag-enunciado-vacio" POST "$base" "$PAYLOADS/empty-statement.json" \
    "$EVID/$tag-curl-enunciado-vacio.json"
  capture "$tag-justificacion-corta" POST "$base" "$PAYLOADS/short-justification.json" \
    "$EVID/$tag-curl-justificacion-corta.json"
  capture "$tag-tres-opciones" POST "$base" "$PAYLOADS/three-options.json" \
    "$EVID/$tag-curl-tres-opciones.json"
  capture "$tag-dos-claves" POST "$base" "$PAYLOADS/two-keys.json" \
    "$EVID/$tag-curl-dos-claves.json"
  capture "$tag-evaluar" POST "$base/$id/evaluar" "" "$EVID/$tag-curl-evaluar.json"
  capture "$tag-reevaluar" POST "$base/$id/evaluar" "" "$EVID/$tag-curl-reevaluar.json"
  capture "$tag-consultar" GET "$base/$id" "" "$EVID/$tag-curl-get.json"
  capture "$tag-inexistente" GET "$base/00000000-0000-4000-8000-000000000000" "" \
    "$EVID/$tag-curl-inexistente.json"

  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
}

run_profile h2 01
run_profile memory 02

echo | tee -a "$EVID/04-resumen-local.txt"
echo "Evidencias escritas en docs/evidencias/. Los escenarios con RabbitMQ (E2E-07/08/10)" \
  "requieren Docker Compose: ver docs/evidencias/e2e-docker.sh" | tee -a "$EVID/04-resumen-local.txt"
