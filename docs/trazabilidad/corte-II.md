# Corte II — Microservicios, Bounded Contexts y GoF (35%)

> Estado: **implementado** en los tres servicios, con REST síncrono y AMQP asíncrono reales.

## 1. Bounded Contexts

| Contexto | Servicio | Lenguaje | Persistencia |
|---|---|---|---|
| Autoría y evaluación de reactivos | `question-service` :8083 | `Question`, `AnswerOption`, `Enunciado`, `Justificacion`, `QuestionId`, `EstadoPregunta` | PostgreSQL / H2 / memoria |
| Validación de forma (HU03) | `validation-service` :8082 | `ValidationPayload`, `QuestionFilter`, `ValidationPipeline` | sin estado |
| Ciclo de vida editorial | `lifecycle-service` :8084 | `QuestionLifecycle`, `LifecycleState` | mapa en memoria propio |

Regla aplicada: **ningún servicio importa clases de otro**. `question-service` habla con
`validation-service` por HTTP y con `lifecycle-service` por eventos; no comparte entidades ni
repositorios. El contrato del evento se duplica a propósito en emisor y receptor (§8.3, decisión
A17).

## 2. Comunicación síncrona — REST

```
POST /api/questions
  question-service ──► POST http://validation-service:8082/api/validation/questions
                    ◄── 200 {"valido":true}  → crea en BORRADOR → 201 Created
                    ◄── 400 {"valido":false} → ReglaNegocioException → 400 Bad Request
```

- El adaptador `ValidationServiceClient` implementa el puerto puro `ValidacionPreguntaPort`
  (decisión A11/A23) y se configura con timeouts (2 s conexión / 3 s lectura).
- Si el servicio está caído se lanza `ValidationServiceUnavailableException` → **503** documentado.
- Nombre DNS en Compose: `http://validation-service:8082`; en local: `http://localhost:8082`.

## 3. Comunicación asíncrona — AMQP

| Elemento | Valor |
|---|---|
| Exchange | `saberpro.questions.exchange` (topic, durable) |
| Routing key | `question.review.requested` |
| Cola | `saberpro.lifecycle.question-review` (durable) |
| DLX / DLQ | `saberpro.questions.dlx` / `saberpro.lifecycle.question-review.dlq` |

- Publicación **después** de persistir la transición (nunca antes).
- Mensaje JSON plano, sin cabecera `__TypeId__`, para no acoplar los paquetes Java de los dos
  servicios.
- Consumidor con `ack` automático, 3 reintentos con backoff 1 s → 2 s → 4 s y `default-requeue-rejected: false`
  para que el mensaje agotado termine en la DLQ.
- Idempotencia por `eventId`: el listener mantiene el conjunto de eventos procesados y descarta
  duplicados con log explícito.

Evidencia real (broker RabbitMQ 3.13 local, sin Docker):

```
E2E-01 POST /api/questions -> HTTP 201
E2E-06 POST /api/questions/{id}/evaluar -> HTTP 202
E2E-08 GET /api/lifecycle/questions/{id} -> HTTP 200
RabbitMQ queue: saberpro.lifecycle.question-review | consumers: 1 | messages: 0 | durable: True | dlx: saberpro.questions.dlx
Eventos procesados por el listener: 1
E2E-10 reenvio del mismo eventId: descartado por idempotencia (OK)
```

Archivos: `docs/evidencias/05-resumen-e2e.txt`, `05-listener-log.txt`,
`05-idempotencia-log.txt`, `05-rabbit-queue.json`, `05-rabbit-bindings.json`.

## 4. Patrones GoF evidenciados

| Patrón | Clase / uso | Por qué |
|---|---|---|
| **Builder** | `Question.builder()...build()` | Construir el agregado en pruebas y en el mapper sin telescoping de constructores; el constructor sigue privado |
| **State** | `lifecycle-service/.../state/`: `QuestionLifecycle`, `LifecycleState`, `PendingReviewState`, `InReviewState`, `ApprovedState`, `RejectedState` | El estado del ciclo de vida cambia de comportamiento según la fase; las transiciones inválidas se rechazan |
| **Adapter** | `JpaQuestionAdapter`, `InMemoryQuestionAdapter`, `RabbitMQPublisher`, `ValidationServiceClient` | Traducen el lenguaje del dominio a JPA, `HashMap`, AMQP y HTTP sin contaminar `domain` |

También se aplica **Repository** (puerto `PreguntaRepositoryPort`) y **DTO/Mapper**
(`QuestionRecord` ↔ `QuestionDomainMapper` ↔ `Question`).

## 5. JPA y modelo relacional

- `QuestionRecord` (`@Entity`) contiene `id`, `enunciado`, `justificacion`, `estado` y una
  colección `@ElementCollection` de `AnswerOptionRecord` (`@Embeddable`).
- `QuestionDomainMapper` es la única clase que conoce ambos mundos; no hay anotaciones JPA en
  `domain`.
- Perfiles: `postgres` (Compose) y `h2` (desarrollo/tests) con el **mismo** adaptador.

## 6. Evidencias ejecutadas

| Archivo | Contenido |
|---|---|
| `docs/evidencias/03-mvn-test-all.log` | `Tests run: 75` en `question-service` |
| `docs/evidencias/03-mvn-test-lifecycle.log` | `Tests run: 11` (listener, idempotencia, State, REST) |
| `docs/evidencias/05-*` | Flujo AMQP real con broker local |
| `docs/evidencias/01-*`, `02-*` | REST con ambos adaptadores |
| `docs/evidencias/logs/e2e-lifecycle.log` | Log del `@RabbitListener` procesando el evento |

## 7. Tag

```bash
git checkout corte-II
mvn -f lifecycle-service/pom.xml test
```
