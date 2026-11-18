#!/usr/bin/env bash
# =============================================================================
# FULL E2E EVIDENCE OVER CONTAINERS (Taller 13 §9, §10, §11).
#
# This script is the canonical evidence generator of the final delivery. It
# requires a Docker Engine accessible by the current user. On the lab machine
# used to build this repository the socket was not accessible
# (/var/run/docker.sock: permission denied) and `sudo` required a password, so
# the script is delivered ready to run but its output is PENDING.
#
# Usage:
#   bash docs/evidencias/e2e-docker.sh
#
# Artifacts written to docs/evidencias/:
#   06-compose-ps.txt             docker compose ps of the 5 containers
#   06-compose-images.txt         image sizes
#   06-network-inspect.txt        saberpro-net bridge inspection
#   06-compose-up.log             build + up log
#   06-compose-logs.txt           logs of the 5 containers
#   06-<escenario>.json           HTTP bodies of E2E-01..E2E-10 (adapter JPA)
#   06-resumen-docker.txt         HTTP codes with the JPA adapter
#   07-resumen-memory.txt         same scenarios with the in-memory adapter
#   07-adapter-log-jpa.txt        startup log "Adaptador de persistencia activo: JPA"
#   07-adapter-log-memory.txt     startup log "Adaptador de persistencia activo: MEMORIA"
#   08-e2e11-comparativa.txt      diff of both runs (E2E-11)
# =============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVID="$ROOT/docs/evidencias"
PAYLOADS="$EVID/payloads"
COMPOSE=(docker compose -f "$ROOT/docker-compose.yml" --project-directory "$ROOT")

QUESTION_URL="http://localhost:8083"
VALIDATION_URL="http://localhost:8082"
LIFECYCLE_URL="http://localhost:8084"
RABBIT_MGMT="http://localhost:15672/api"
RABBIT_USER="saberpro"
RABBIT_PASS="saberpro"
MISSING_ID="00000000-0000-4000-8000-000000000000"

log() { echo "$@" | tee -a "$EVID/06-resumen-docker.txt"; }

preflight() {
  if ! docker info >/dev/null 2>&1; then
    cat >&2 <<'EOF'
ERROR: Docker Engine no es accesible por el usuario actual.
       Verifique:  docker info
       Si el error es "permission denied", agregue el usuario al grupo docker
       (sudo usermod -aG docker "$USER" && newgrp docker) o ejecute el script
       con privilegios. Este script NO se ejecuto en la maquina de desarrollo
       de la entrega: ver docs/informe/informe-tecnico.md, seccion de limitaciones.
EOF
    exit 2
  fi
  docker compose version >/dev/null 2>&1 || { echo "ERROR: falta docker compose v2" >&2; exit 2; }
}

wait_for() {
  local url="$1" name="$2"
  for _ in $(seq 1 120); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "OK   $name listo" | tee -a "$EVID/06-resumen-docker.txt"
      return 0
    fi
    sleep 2
  done
  echo "ERROR $name no respondio en 240s" | tee -a "$EVID/06-resumen-docker.txt" >&2
  return 1
}

capture_http() {
  local tag="$1" method="$2" url="$3" body="${4:-}"
  local code
  if [ -n "$body" ]; then
    code=$(curl -s -o "$EVID/$tag.json" -w "%{http_code}" -X "$method" \
      -H "Content-Type: application/json" --data-binary "@$body" "$url")
  else
    code=$(curl -s -o "$EVID/$tag.json" -w "%{http_code}" -X "$method" "$url")
  fi
  echo "$code"
}

run_e2e_suite() {
  local prefix="$1" summary="$2"
  : > "$EVID/$summary"
  echo "== Escenarios E2E sobre contenedores ($prefix) ==" | tee -a "$EVID/$summary"
  date -Iseconds | tee -a "$EVID/$summary"

  local code
  code=$(capture_http "${prefix}-01-crear" POST "$QUESTION_URL/api/questions" "$PAYLOADS/valid.json")
  local question_id
  question_id=$(grep -o '"id":"[^"]*"' "$EVID/${prefix}-01-crear.json" | head -1 | cut -d'"' -f4)
  log "E2E-01 crear valido                    -> HTTP $code (id=$question_id)"

  code=$(capture_http "${prefix}-02-enunciado-vacio" POST "$QUESTION_URL/api/questions" "$PAYLOADS/empty-statement.json")
  log "E2E-02 enunciado vacio                 -> HTTP $code"
  code=$(capture_http "${prefix}-03-justificacion-corta" POST "$QUESTION_URL/api/questions" "$PAYLOADS/short-justification.json")
  log "E2E-03 justificacion corta             -> HTTP $code"
  code=$(capture_http "${prefix}-04-tres-opciones" POST "$QUESTION_URL/api/questions" "$PAYLOADS/three-options.json")
  log "E2E-04 tres opciones                   -> HTTP $code"
  code=$(capture_http "${prefix}-05-dos-claves" POST "$QUESTION_URL/api/questions" "$PAYLOADS/two-keys.json")
  log "E2E-05 dos claves correctas            -> HTTP $code"

  code=$(capture_http "${prefix}-06-evaluar" POST "$QUESTION_URL/api/questions/$question_id/evaluar")
  log "E2E-06 evaluar                         -> HTTP $code"

  local lifecycle_code=000
  for _ in $(seq 1 20); do
    lifecycle_code=$(curl -s -o "$EVID/${prefix}-08-lifecycle.json" -w "%{http_code}" \
      "$LIFECYCLE_URL/api/lifecycle/questions/$question_id")
    [ "$lifecycle_code" = "200" ] && break
    sleep 1
  done
  log "E2E-08 GET lifecycle                   -> HTTP $lifecycle_code"

  code=$(capture_http "${prefix}-09-inexistente" GET "$QUESTION_URL/api/questions/$MISSING_ID")
  log "E2E-09 id inexistente                  -> HTTP $code"

  local event_id
  event_id=$(grep -o '"eventId":"[^"]*"' "$EVID/${prefix}-08-lifecycle.json" | cut -d'"' -f4)
  if [ -n "$event_id" ]; then
    python3 - "$event_id" "$question_id" > "$EVID/${prefix}-10-republish-body.json" <<'PY'
import json, sys
print(json.dumps({
    "properties": {},
    "routing_key": "question.review.requested",
    "payload": json.dumps({
        "eventId": sys.argv[1],
        "eventType": "QuestionReviewRequested",
        "version": 1,
        "occurredAt": "2026-11-13T15:04:05Z",
        "questionId": sys.argv[2],
        "estado": "PENDIENTE_REVISION",
        "numeroOpciones": 4,
    }),
    "payload_encoding": "string",
}))
PY
    curl -s -u "$RABBIT_USER:$RABBIT_PASS" -H "Content-Type: application/json" \
      -X POST "$RABBIT_MGMT/exchanges/%2F/saberpro.questions.exchange/publish" \
      --data-binary "@$EVID/${prefix}-10-republish-body.json" \
      -o "$EVID/${prefix}-10-republish-response.json"
    sleep 2
    ${COMPOSE[@]} logs --no-color lifecycle-service > "$EVID/${prefix}-10-lifecycle.log" 2>&1
    if grep -q "ya procesado" "$EVID/${prefix}-10-lifecycle.log"; then
      log "E2E-10 reenvio del mismo eventId       -> descartado por idempotencia (OK)"
    else
      log "E2E-10 reenvio del mismo eventId       -> NO se encontro la evidencia de idempotencia"
    fi
  fi
}

capture_rabbit() {
  curl -s -u "$RABBIT_USER:$RABBIT_PASS" "$RABBIT_MGMT/exchanges/%2F/saberpro.questions.exchange" \
    -o "$EVID/06-rabbit-exchange.json"
  curl -s -u "$RABBIT_USER:$RABBIT_PASS" "$RABBIT_MGMT/queues/%2F/saberpro.lifecycle.question-review" \
    -o "$EVID/06-rabbit-queue.json"
  curl -s -u "$RABBIT_USER:$RABBIT_PASS" "$RABBIT_MGMT/queues/%2F/saberpro.lifecycle.question-review/bindings" \
    -o "$EVID/06-rabbit-bindings.json"
  python3 - "$EVID/06-rabbit-queue.json" <<'PY' | tee -a "$EVID/06-resumen-docker.txt"
import json, sys
queue = json.load(open(sys.argv[1]))
print("RabbitMQ queue:", queue.get("name"),
      "| consumers:", queue.get("consumers"),
      "| messages:", queue.get("messages"),
      "| durable:", queue.get("durable"),
      "| dlx:", (queue.get("arguments") or {}).get("x-dead-letter-exchange"))
PY
}

# --------------------------------------------------------------------------- #
preflight
mkdir -p "$EVID"
: > "$EVID/06-resumen-docker.txt"

echo "== 1/6 docker compose up -d --build =="
"${COMPOSE[@]}" up -d --build > "$EVID/06-compose-up.log" 2>&1
up_exit=$?
echo "exit=$up_exit" >> "$EVID/06-compose-up.log"
[ "$up_exit" -ne 0 ] && { tail -40 "$EVID/06-compose-up.log" >&2; exit "$up_exit"; }

wait_for "$VALIDATION_URL/actuator/health" "validation-service:8082"
wait_for "$LIFECYCLE_URL/actuator/health" "lifecycle-service:8084"
wait_for "$QUESTION_URL/actuator/health" "question-service:8083"

echo "== 2/6 evidencia de contenedores, imagenes y red =="
"${COMPOSE[@]}" ps > "$EVID/06-compose-ps.txt" 2>&1
docker image ls --format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}' \
  | grep -Ei 'saberpro|rabbitmq|postgres' > "$EVID/06-compose-images.txt" || true
docker network inspect saberpro-banco-preguntas_saberpro-net > "$EVID/06-network-inspect.txt" 2>&1 \
  || docker network inspect saberpro-net > "$EVID/06-network-inspect.txt" 2>&1

echo "== 3/6 topologia RabbitMQ =="
capture_rabbit

echo "== 4/6 escenarios E2E con el adaptador JPA (perfil postgres) =="
"${COMPOSE[@]}" logs --no-color question-service > "$EVID/07-adapter-log-jpa.txt" 2>&1
grep "Adaptador de persistencia activo" "$EVID/07-adapter-log-jpa.txt" | tail -1 \
  | tee -a "$EVID/06-resumen-docker.txt" || true
run_e2e_suite "06" "06-resumen-docker.txt"

"${COMPOSE[@]}" logs --no-color > "$EVID/06-compose-logs.txt" 2>&1

echo "== 5/6 E2E-11: conmutacion al adaptador de memoria (solo env) =="
QUESTION_REPOSITORY_ADAPTER=memory QUESTION_PROFILE=memory \
  "${COMPOSE[@]}" up -d --build question-service > "$EVID/07-compose-up-memory.log" 2>&1
wait_for "$QUESTION_URL/actuator/health" "question-service (memoria)"
"${COMPOSE[@]}" logs --no-color question-service > "$EVID/07-adapter-log-memory.txt" 2>&1
grep "Adaptador de persistencia activo" "$EVID/07-adapter-log-memory.txt" | tail -1 \
  | tee -a "$EVID/06-resumen-docker.txt" || true
run_e2e_suite "07" "07-resumen-memory.txt"

echo "== 6/6 comparativa E2E-11 (solo deben cambiar los UUID) =="
{
  echo "== Comparativa de codigos HTTP: JPA vs MEMORIA =="
  echo "--- JPA ---"
  grep -E '^E2E-' "$EVID/06-resumen-docker.txt" | sed -E 's/\(id=[^)]*\)//'
  echo "--- MEMORIA ---"
  grep -E '^E2E-' "$EVID/07-resumen-memory.txt" | sed -E 's/\(id=[^)]*\)//'
  echo
  if diff <(grep -E '^E2E-' "$EVID/06-resumen-docker.txt" | sed -E 's/\(id=[^)]*\)//') \
          <(grep -E '^E2E-' "$EVID/07-resumen-memory.txt" | sed -E 's/\(id=[^)]*\)//') >/dev/null; then
    echo "RESULTADO: comportamiento IDENTICO entre JpaQuestionAdapter e InMemoryQuestionAdapter."
    echo "El dominio NO se modifico: sha256sum -c docs/evidencias/domain.sha256"
  else
    echo "RESULTADO: DIFERENCIAS detectadas; revisar el acoplamiento del adaptador."
  fi
} | tee "$EVID/08-e2e11-comparativa.txt"

echo "== Restaurando el adaptador JPA por defecto =="
"${COMPOSE[@]}" up -d --build question-service >> "$EVID/07-compose-up-memory.log" 2>&1

echo "Evidencias Docker en docs/evidencias/06-* y 07-*."
