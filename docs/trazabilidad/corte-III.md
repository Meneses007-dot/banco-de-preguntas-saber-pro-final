# Corte III — DDD, Arquitectura Hexagonal y Docker (40%)

> Estado: **implementado y verificado en local**; la ejecución sobre contenedores queda pendiente
> por la limitación de acceso al socket de Docker documentada en `AGENTS.md` §15-A27 y en el
> informe técnico (§9).

## 1. El hexágono

```
              ADAPTADORES DE ENTRADA                    ADAPTADORES DE SALIDA
        ┌───────────────────────────┐            ┌────────────────────────────────┐
        │ QuestionRestController    │            │ JpaQuestionAdapter             │
        │ GlobalExceptionHandler    │            │ InMemoryQuestionAdapter        │
        └────────────┬──────────────┘            │ RabbitMQPublisher              │
                     │                            │ ValidationServiceClient        │
        ┌────────────▼──────────────┐            └───────────────▲────────────────┘
        │ PUERTOS INBOUND           │                            │
        │ CrearPreguntaUseCase      │      ┌─────────────────────┴──────────────┐
        │ EvaluarPreguntaUseCase    │◄─────┤ PUERTOS OUTBOUND                   │
        │ ConsultarPreguntaUseCase  │      │ PreguntaRepositoryPort             │
        └────────────┬──────────────┘      │ NotificacionPort                   │
                     │                     │ ValidacionPreguntaPort             │
        ┌────────────▼──────────────┐      └────────────────────────────────────┘
        │ QuestionApplicationService│
        └────────────┬──────────────┘
        ┌────────────▼──────────────────────────────────────────────────────────┐
        │ DOMINIO PURO: Question · AnswerOption · Enunciado · Justificacion ·   │
        │ QuestionId · EstadoPregunta · ReglaNegocioException                   │
        └───────────────────────────────────────────────────────────────────────┘
```

Regla de dependencia verificada con ArchUnit: `domain` no conoce `org.springframework`,
`jakarta.persistence`, `org.hibernate`, `com.fasterxml.jackson`, `java.sql`, AMQP ni
`infrastructure`/`application`; `application` no conoce `infrastructure`.

## 2. Intercambiabilidad (protocolo §11)

### Paso 1 — `JpaQuestionAdapter` activo

```bash
mvn -f question-service/pom.xml test -Dtest='QuestionRestControllerH2Test,JpaQuestionRepositoryContractTest'
# Tests run: 17, Failures: 0, Errors: 0
```

Log de arranque real:

```
Adaptador de persistencia activo: JPA(h2)
```

Evidencias: `docs/evidencias/03-mvn-test-jpa.log`, `01-curl-*.json`, `04-resumen-local.txt`
(bloque `01-*`).

### Paso 2 — cambio de adaptador **solo por configuración**

```bash
QUESTION_REPOSITORY_ADAPTER=memory QUESTION_PROFILE=memory
```

No se modificó ninguna línea de `domain` ni de `application`:

```bash
find question-service/src/main/java/co/edu/unicauca/saberpro/question/domain -name '*.java' \
  | sort | xargs sha256sum > docs/evidencias/domain.sha256
sha256sum -c docs/evidencias/domain.sha256      # 17/17 OK
git diff --exit-code -- question-service/src/main/java/co/edu/unicauca/saberpro/question/domain  # 0
```

Log de arranque real:

```
Adaptador de persistencia activo: MEMORIA(HashMap)
```

Evidencias: `docs/evidencias/domain.sha256`, `03-domain-integridad.txt`, `02-curl-*.json`,
`04-resumen-local.txt` (bloque `02-*`).

### Paso 3 — comportamiento idéntico

Misma suite de contrato, dos subclases:

```bash
mvn -f question-service/pom.xml test -Dtest='QuestionRestControllerMemoryTest,InMemoryQuestionRepositoryContractTest'
# Tests run: 17, Failures: 0, Errors: 0
```

| Aspecto | `JpaQuestionAdapter` | `InMemoryQuestionAdapter` | ¿Idéntico? |
|---|---|---|---|
| Puerto implementado | `PreguntaRepositoryPort` | `PreguntaRepositoryPort` | ✅ |
| `POST /api/questions` | 201 + `BORRADOR` | 201 + `BORRADOR` | ✅ |
| INV-01 (enunciado vacío) | 400 + mismo mensaje | 400 + mismo mensaje | ✅ |
| INV-02 (justificación corta) | 400 + mismo mensaje | 400 + mismo mensaje | ✅ |
| INV-03/INV-04 (3 opciones / 2 claves) | 400 + mismo mensaje | 400 + mismo mensaje | ✅ |
| `POST .../evaluar` | 202 + `PENDIENTE_REVISION` | 202 + `PENDIENTE_REVISION` | ✅ |
| Reevaluación (INV-07) | 400 | 400 | ✅ |
| `GET .../{id}` inexistente | 404 | 404 | ✅ |
| Evento RabbitMQ + listener | procesado | procesado | ✅ |
| Líneas cambiadas en `domain` | — | **0** | ✅ |
| Tests del contrato | 17 ok | 17 ok | ✅ |

Comparación real de códigos HTTP (`docs/evidencias/04-resumen-local.txt`): los bloques `01-*`
(JPA/h2) y `02-*` (memoria) contienen exactamente la misma secuencia
`201 · 400 · 400 · 400 · 400 · 202 · 400 · 200 · 404`; solo cambian los UUID generados.

En contenedores, el script `docs/evidencias/e2e-docker.sh` repite el protocolo y genera
`08-e2e11-comparativa.txt` (pendiente de ejecución por la limitación A27).

## 3. Contenerización

| Servicio | Dockerfile | Particularidades |
|---|---|---|
| `question-service` | multi-stage `maven:3.9-eclipse-temurin-17` → `openjdk:17-jdk-slim` | usuario `appuser` (uid 1001), `EXPOSE 8083`, `HEALTHCHECK` con `curl /actuator/health` |
| `validation-service` | idem | `EXPOSE 8082`, healthcheck |
| `lifecycle-service` | idem | `EXPOSE 8084`, healthcheck |

`docker-compose.yml`: 5 servicios (`rabbitmq`, `postgres`, `validation-service`,
`question-service`, `lifecycle-service`), red **`saberpro-net` driver bridge**, volúmenes
`postgres_data` y `rabbitmq_data`, healthchecks y `depends_on` con `condition: service_healthy`.
Puertos públicos: **8082, 8083, 8084, 5672, 15672** (+ 5432 opcional).

Validación estática ejecutada:

```bash
docker compose config --quiet   # exit 0
```

> `docker compose up -d --build`, `docker compose ps`, `docker images` y `docker network inspect`
> no pudieron ejecutarse en la máquina de desarrollo (socket sin permisos). El script
> `docs/evidencias/e2e-docker.sh` reproduce todo el protocolo cuando Docker esté disponible.

## 4. Diagramas C4

| Nivel | Archivo fuente | Imagen |
|---|---|---|
| N1 | `docs/diagramas/c4-n1-contexto.puml` | `.svg` / `.png` |
| N2 | `docs/diagramas/c4-n2-contenedores.puml` | `.svg` / `.png` |
| N3 | `docs/diagramas/c4-n3-componentes.puml` | `.svg` / `.png` |
| N4 | `docs/diagramas/c4-n4-codigo.mmd` + `c4-n4-estados.mmd` | `.svg` / `.png` |

Regeneración: `bash docs/diagramas/render.sh` (Kroki: PlantUML y Mermaid).

## 5. Evidencias del corte

| Archivo | Contenido |
|---|---|
| `03-mvn-test-all.log` | 75 pruebas verdes (incluye ArchUnit) |
| `03-mvn-test-jpa.log` / `03-mvn-test-memory.log` | 17 + 17 pruebas de contrato e integración |
| `03-archunit.log` | 4 reglas de arquitectura en verde |
| `03-dominio-puro.log` | `grep` de pureza sin coincidencias |
| `03-domain-integridad.txt` | `sha256sum -c` con 17 archivos OK |
| `04-resumen-local.txt` | 201/400/202/404 con ambos adaptadores |
| `05-resumen-e2e.txt` | 202 + consumo AMQP + idempotencia |

## 6. Tag

```bash
git checkout corte-III
sha256sum -c docs/evidencias/domain.sha256
mvn -f question-service/pom.xml test
```
