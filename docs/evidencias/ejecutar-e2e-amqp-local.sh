#!/usr/bin/env bash
# =============================================================================
# Local asynchronous E2E evidence (without Docker, with a real RabbitMQ broker).
# It proves E2E-06 (202 Accepted), E2E-07 (event consumed), E2E-08 (lifecycle
# lookup) and E2E-10 (idempotency by eventId).
#
# Prerequisites: RabbitMQ 3.13 on localhost:5672/15672 with guest/guest enabled
#               (laboratory machine), the three JARs packaged and Python 3.
# Usage: bash docs/evidencias/ejecutar-e2e-amqp-local.sh
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVID="$ROOT/docs/evidencias"
PAYLOADS="$EVID/payloads"
LOGS="$EVID/logs"
mkdir -p "$LOGS"

VALIDATION_JAR="$ROOT/validation-service/target/validation-service-1.0.0.jar"
QUESTION_JAR="$ROOT/question-service/target/question-service-1.0.0.jar"
LIFECYCLE_JAR="$ROOT/lifecycle-service/target/lifecycle-service-1.0.0.jar"

RABBIT_USER="${RABBIT_USER:-guest}"
RABBIT_PASS="${RABBIT_PASS:-guest}"
RABBIT_HOST="${RABBIT_HOST:-localhost}"
MGMT="http://${RABBIT_HOST}:15672/api"
BROKER_ARGS=(--spring.rabbitmq.host="$RABBIT_HOST" --spring.rabbitmq.port="${RABBIT_PORT:-5672}"
             --spring.rabbitmq.username="$RABBIT_USER" --spring.rabbitmq.password="$RABBIT_PASS")

PIDS=()
cleanup() {
  for pid in "${PIDS[@]:-}"; do kill "$pid" 2>/dev/null || true; done
  wait 2>/dev/null || true
}
trap cleanup EXIT

wait_for() {
  local url="$1" name="$2"
  for _ in $(seq 1 90); do
    curl -sf "$url" >/dev/null 2>&1 && { echo "OK   $name listo"; return 0; }
    sleep 1
  done
  echo "ERROR $name no arrancó" >&2
  return 1
}

publish_event() {
  local body_file="$1"
  curl -s -u "$RABBIT_USER:$RABBIT_PASS" \
    -H "Content-Type: application/json" \
    -X POST "$MGMT/exchanges/%2F/saberpro.questions.exchange/publish" \
    --data-binary "@$body_file"
}

: > "$EVID/05-resumen-e2e.txt"
echo "== E2E asincrono local con RabbitMQ (broker real, sin Docker) ==" | tee -a "$EVID/05-resumen-e2e.txt"
date -Iseconds | tee -a "$EVID/05-resumen-e2e.txt"

# ------------------------------------------------------------- validation
java -jar "$VALIDATION_JAR" --server.port=8082 > "$LOGS/e2e-validation.log" 2>&1 &
PIDS+=($!)
wait_for http://localhost:8082/actuator/health validation-service

# -------------------------------------------------------------- lifecycle
java -jar "$LIFECYCLE_JAR" --server.port=8084 "${BROKER_ARGS[@]}" \
  > "$LOGS/e2e-lifecycle.log" 2>&1 &
PIDS+=($!)
wait_for http://localhost:8084/actuator/health lifecycle-service

# --------------------------------------------------------------- question
java -jar "$QUESTION_JAR" --server.port=8083 --spring.rabbitmq.dynamic=true "${BROKER_ARGS[@]}" \
  > "$LOGS/e2e-question.log" 2>&1 &
PIDS+=($!)
wait_for http://localhost:8083/actuator/health question-service

# E2E-01 + E2E-06
create_code=$(curl -s -o "$EVID/05-crear.json" -w "%{http_code}" \
  -X POST http://localhost:8083/api/questions -H "Content-Type: application/json" \
  --data-binary "@$PAYLOADS/valid.json")
question_id=$(grep -o '"id":"[^"]*"' "$EVID/05-crear.json" | head -1 | cut -d'"' -f4)
echo "E2E-01 POST /api/questions -> HTTP $create_code (id=$question_id)" | tee -a "$EVID/05-resumen-e2e.txt"

evaluate_code=$(curl -s -o "$EVID/05-evaluar.json" -w "%{http_code}" \
  -X POST "http://localhost:8083/api/questions/$question_id/evaluar")
echo "E2E-06 POST /api/questions/{id}/evaluar -> HTTP $evaluate_code" | tee -a "$EVID/05-resumen-e2e.txt"

# E2E-08: wait until the listener registered the state in its own context
lifecycle_code=000
for _ in $(seq 1 20); do
  lifecycle_code=$(curl -s -o "$EVID/05-lifecycle-get.json" -w "%{http_code}" \
    "http://localhost:8084/api/lifecycle/questions/$question_id")
  [ "$lifecycle_code" = "200" ] && break
  sleep 1
done
echo "E2E-08 GET /api/lifecycle/questions/{id} -> HTTP $lifecycle_code" | tee -a "$EVID/05-resumen-e2e.txt"
cat "$EVID/05-lifecycle-get.json" | tee -a "$EVID/05-resumen-e2e.txt" >/dev/null
echo | tee -a "$EVID/05-resumen-e2e.txt"

# E2E-07: broker topology and consumer evidence through the management API
curl -s -u "$RABBIT_USER:$RABBIT_PASS" "$MGMT/exchanges/%2F/saberpro.questions.exchange" \
  -o "$EVID/05-rabbit-exchange.json"
curl -s -u "$RABBIT_USER:$RABBIT_PASS" "$MGMT/queues/%2F/saberpro.lifecycle.question-review" \
  -o "$EVID/05-rabbit-queue.json"
curl -s -u "$RABBIT_USER:$RABBIT_PASS" \
  "$MGMT/queues/%2F/saberpro.lifecycle.question-review/bindings" \
  -o "$EVID/05-rabbit-bindings.json"

python3 - "$EVID/05-rabbit-queue.json" <<'PY' | tee -a "$EVID/05-resumen-e2e.txt"
import json, sys
queue = json.load(open(sys.argv[1]))
print("RabbitMQ queue:", queue.get("name"),
      "| consumers:", queue.get("consumers"),
      "| messages:", queue.get("messages"),
      "| durable:", queue.get("durable"),
      "| dlx:", (queue.get("arguments") or {}).get("x-dead-letter-exchange"))
PY
grep -c "Evento .* procesado" "$LOGS/e2e-lifecycle.log" \
  | sed 's/^/Eventos procesados por el listener: /' | tee -a "$EVID/05-resumen-e2e.txt"
grep "Evento .* procesado" "$LOGS/e2e-lifecycle.log" | tail -2 > "$EVID/05-listener-log.txt" || true

# E2E-10: republish the exact same eventId and check idempotency
python3 - "$EVID/05-lifecycle-get.json" > /tmp/republish.json <<'PY'
import json, sys
event = json.load(open(sys.argv[1]))
payload = json.dumps({
    "eventId": event["eventId"],
    "eventType": "QuestionReviewRequested",
    "version": 1,
    "occurredAt": event["occurredAt"],
    "questionId": event["questionId"],
    "estado": event["estado"],
    "numeroOpciones": 4,
})
print(json.dumps({
    "properties": {},
    "routing_key": "question.review.requested",
    "payload": payload,
    "payload_encoding": "string",
}))
PY
publish_event /tmp/republish.json > "$EVID/05-republish-response.json"
sleep 2
if grep -q "ya procesado" "$LOGS/e2e-lifecycle.log"; then
  echo "E2E-10 reenvio del mismo eventId: descartado por idempotencia (OK)" \
    | tee -a "$EVID/05-resumen-e2e.txt"
  grep "ya procesado" "$LOGS/e2e-lifecycle.log" | tail -1 > "$EVID/05-idempotencia-log.txt"
else
  echo "E2E-10 reenvio del mismo eventId: NO se encontro el log de idempotencia" \
    | tee -a "$EVID/05-resumen-e2e.txt"
fi

echo "Evidencias en docs/evidencias/05-*" | tee -a "$EVID/05-resumen-e2e.txt"
