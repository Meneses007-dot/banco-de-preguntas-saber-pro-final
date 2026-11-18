# Banco de Preguntas Saber PRO

## Entrega Final Integradora — DDD, Arquitectura Hexagonal y Docker

**Curso:** Ingeniería de Software II

**Programa:** Ingeniería de Sistemas — Facultad de Ingeniería Electrónica y Telecomunicaciones

**Universidad del Cauca — Popayán, 2026**

**Profesora:** Paola Andrea Bedoya Toro

**Repositorio:** `saberpro-banco-preguntas/`

---

## 1. Introducción

El examen **Saber PRO** evalúa competencias genéricas mediante preguntas de selección múltiple con
única respuesta. Un **banco de preguntas** institucional debe garantizar que cada reactivo tenga un
enunciado, una justificación de la clave, cuatro opciones y exactamente una opción correcta; además
debe controlar el tránsito editorial entre el borrador y la revisión por parte de un coordinador.

Esta entrega integra los tres cortes del semestre en un solo producto ejecutable:

1. **Corte I (25%)** — monolito limpio, principios SOLID y pipeline de validación **Pipes &
   Filters** (HU03).
2. **Corte II (35%)** — microservicios con *bounded contexts*, REST síncrono, eventos **AMQP** y
   patrones GoF (**Builder**, **State**, **Adapter**).
3. **Corte III (40%)** — **DDD + Arquitectura Hexagonal** con dominio puro e intercambiable, más
   **Dockerfiles** y **Docker Compose**.

El resultado son tres servicios independientes que cooperan sin compartir modelo de dominio:

<!-- tabla: Tabla 1. Servicios del sistema y responsabilidades. -->

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `validation-service` | 8082 | Validación de forma (Pipes & Filters) |
| `question-service` | 8083 | Núcleo hexagonal: agregado, invariantes, persistencia y publicación |
| `lifecycle-service` | 8084 | Consumidor idempotente y ciclo de vida editorial |

## 2. Objetivos

### 2.1 Objetivo general

Diseñar, implementar y desplegar un sistema distribuido para la gestión de preguntas Saber PRO,
aplicando Domain-Driven Design, Arquitectura Hexagonal y contenerización, con evidencia verificable
de sus invariantes, de la comunicación síncrona y asíncrona, y de la intercambiabilidad de sus
adaptadores de persistencia.

### 2.2 Objetivos específicos

1. Modelar el agregado `Question` con objetos de valor inmutables y ocho invariantes explícitas.
2. Aislar el dominio de todo framework y demostrarlo con reglas de arquitectura automatizadas.
3. Definir puertos de entrada y salida y al menos dos adaptadores intercambiables de persistencia.
4. Implementar el flujo de creación con validación síncrona y el flujo de envío a revisión con
   notificación asíncrona.
5. Garantizar idempotencia del consumidor y tolerancia a fallos del broker (reintentos y DLQ).
6. Contenerizar los tres servicios y orquestarlos con Docker Compose sobre una red bridge.
7. Producir evidencia reproducible de cada escenario E2E y documentar la trazabilidad semestral.

## 3. Marco conceptual

### 3.1 Domain-Driven Design

El **lenguaje ubicuo** del negocio se expresa en clases del dominio: `Question`, `AnswerOption`,
`Enunciado`, `Justificacion`, `QuestionId` y `EstadoPregunta`. El agregado `Question` es la única
puerta de entrada a su consistencia: no expone setters de estado y valida sus invariantes en la
transición.

### 3.2 Arquitectura Hexagonal

El dominio ocupa el centro; todo lo demás son adaptadores conectados por puertos. La regla de
dependencia se invierte: la infraestructura depende del dominio, nunca al contrario. Esto permite
cambiar JPA por memoria (o por cualquier tecnología) sin tocar una línea del negocio.

### 3.3 Pipes & Filters

La validación estructural se descompone en filtros independientes que comparten un contrato y se
ejecutan en secuencia, acumulando errores. Cada filtro tiene una única razón de cambio (SRP) y el
pipe permanece cerrado a modificación (OCP).

### 3.4 Mensajería asíncrona

El envío a revisión no debe bloquear al cliente ni acoplar `question-service` con
`lifecycle-service`. Se usa el patrón **publish/subscribe** sobre un exchange *topic* de RabbitMQ.
El evento se publica **después** de persistir la transición y el consumidor es idempotente por
`eventId`, lo que hace la entrega *at-least-once* segura.

### 3.5 Contenerización

Cada servicio se construye en dos etapas (build con Maven, runtime con JRE) y corre con un usuario
no privilegiado. Docker Compose declara la red, los volúmenes, los healthchecks y las dependencias
de arranque.

## 4. Arquitectura de la solución

### 4.1 Vista de contexto (C4 N1)

<!-- figura:docs/diagramas/c4-n1-contexto.png | Figura 1. Diagrama de contexto del sistema (C4 N1). -->

Los actores son el **docente** (crea y envía reactivos), el **coordinador académico** (consulta el
estado del ciclo de vida) y el **estudiante** (consumidor final del banco). Los sistemas externos
son el cliente de pruebas, RabbitMQ, la base de datos y la consola de administración del broker.

### 4.2 Vista de contenedores (C4 N2)

<!-- figura:docs/diagramas/c4-n2-contenedores.png | Figura 2. Contenedores y protocolos sobre la red saberpro-net (C4 N2). -->

<!-- tabla: Tabla 2. Contenedores, tecnologías y protocolos de comunicación. -->

| Contenedor | Tecnología | Protocolo de salida |
|---|---|---|
| `validation-service` | Java 17 / Spring Boot 3.4 | REST (recibe) |
| `question-service` | Java 17 / Spring Boot 3.4 | REST, JDBC, AMQP |
| `lifecycle-service` | Java 17 / Spring Boot 3.4 | AMQP (recibe), REST (consulta) |
| `rabbitmq` | RabbitMQ 3.13 Management | AMQP 0-9-1 |
| `postgres` | PostgreSQL 16 | JDBC |

### 4.3 Bounded contexts y aislamiento

<!-- tabla: Tabla 3. Bounded contexts y decisiones de aislamiento. -->

| Contexto | Servicio | Decisión clave |
|---|---|---|
| Autoría y evaluación | `question-service` | Dueño del agregado y de las invariantes |
| Validación de forma | `validation-service` | No conoce `Question`; solo `ValidationPayload` |
| Ciclo de vida editorial | `lifecycle-service` | Estado propio (patrón State) en memoria |

Ningún `pom.xml` declara dependencia hacia otro servicio. La comunicación es exclusivamente por
contratos de red: HTTP y mensajes JSON.

### 4.4 Decisiones de arquitectura destacadas

<!-- tabla: Tabla 4. Decisiones de arquitectura destacadas. -->

| # | Decisión | Justificación |
|---|---|---|
| D1 | El pipeline de validación se ejecuta **antes** de construir el agregado | Se evita crear objetos de valor con datos inválidos y se devuelve un reporte completo |
| D2 | Las invariantes INV-03/INV-04 se validan en la **transición**, no en la creación | Un borrador puede estar incompleto; la revisión exige completitud |
| D3 | `NotificacionPort` es obligatorio | Demuestra que el dominio ignora AMQP |
| D4 | Evento JSON sin `__TypeId__` | Evita acoplar los paquetes Java del emisor y del receptor |
| D5 | Idempotencia en el consumidor | La entrega AMQP es *at-least-once*; el duplicado no debe reprocesarse |
| D6 | Publicación tolerante a fallos | La transición ya está persistida; un broker caído no debe revertirla |

## 5. Modelo de dominio

### 5.1 Agregado y objetos de valor

<!-- figura:docs/diagramas/c4-n4-codigo.png | Figura 3. Diagrama de clases del dominio y de los puertos (C4 N4). -->

<!-- tabla: Tabla 5. Elementos del modelo de dominio y sus responsabilidades. -->

| Elemento | Tipo | Responsabilidad |
|---|---|---|
| `Question` | Aggregate root | Consistencia del reactivo y transición de estado |
| `QuestionId` | Value object | Envuelve un `UUID`; igualdad por valor |
| `Enunciado` | Value object | Texto no nulo ni vacío, normalizado con `trim` |
| `Justificacion` | Value object | Texto de al menos 15 caracteres |
| `AnswerOption` | Value object | Texto no vacío y bandera de clave correcta |
| `EstadoPregunta` | Enum | `BORRADOR`, `PENDIENTE_REVISION` |
| `ReglaNegocioException` | Excepción | Toda violación de invariante |

Los objetos de valor son `final`, sin setters y con igualdad por valor. `Question.getOpciones()`
devuelve una copia inmutable, de modo que el agregado controla sus cambios.

### 5.2 Invariantes

<!-- tabla: Tabla 6. Invariantes del agregado Question. -->

| ID | Regla | Implementación | Prueba |
|---|---|---|---|
| INV-01 | El enunciado no es nulo ni vacío | `Enunciado.of` | `EnunciadoTest` |
| INV-02 | La justificación tiene al menos 15 caracteres | `Justificacion.of` | `JustificacionTest` |
| INV-03 | Debe haber exactamente 4 opciones al evaluar | `Question.enviarARevision` | `QuestionTest` |
| INV-04 | Debe haber exactamente 1 clave correcta | `Question.enviarARevision` | `QuestionTest` |
| INV-05 | El texto de cada opción no es vacío | `AnswerOption.of` | `AnswerOptionTest` |
| INV-06 | El estado inicial es `BORRADOR` | `Question.crear` | `QuestionTest` |
| INV-07 | Una pregunta en revisión no se reevalúa | `Question.enviarARevision` | `QuestionTest` |
| INV-08 | La lista de opciones es inmutable | `Question.getOpciones` | `QuestionTest` |

### 5.3 Máquina de estados

<!-- figura:docs/diagramas/c4-n4-estados.png | Figura 4. Máquina de estados del agregado y del contexto de ciclo de vida. -->

La transición `BORRADOR → PENDIENTE_REVISION` ocurre una sola vez. Cualquier intento posterior
lanza `ReglaNegocioException` y el estado permanece inalterado. El contexto de ciclo de vida tiene
su propia máquina (`PENDIENTE_REVISION → EN_REVISION → APROBADA | RECHAZADA`) implementada con el
patrón **State**.

## 6. Puertos y adaptadores

### 6.1 Puertos de entrada (driving)

<!-- tabla: Tabla 7. Puertos de entrada y operaciones. -->

| Puerto | Método | Retorno |
|---|---|---|
| `CrearPreguntaUseCase` | `crear(CrearPreguntaCommand)` | `Question` (estado `BORRADOR`) |
| `EvaluarPreguntaUseCase` | `evaluar(QuestionId)` | `Question` (estado `PENDIENTE_REVISION`) |
| `ConsultarPreguntaUseCase` | `consultar(QuestionId)` | `Question` |

`CrearPreguntaCommand` y `OpcionCommand` son *records* puros, sin anotaciones de framework.

### 6.2 Puertos de salida (driven)

<!-- tabla: Tabla 8. Puertos de salida y adaptadores. -->

| Puerto | Método | Adaptadores |
|---|---|---|
| `PreguntaRepositoryPort` | `save`, `findById` | `JpaQuestionAdapter`, `InMemoryQuestionAdapter` |
| `NotificacionPort` | `notificarEnvioARevision` | `RabbitMQPublisher` |
| `ValidacionPreguntaPort` | `validar(CrearPreguntaCommand)` | `ValidationServiceClient` |

### 6.3 Capa de aplicación

`QuestionApplicationService` implementa los tres casos de uso y **no contiene reglas de negocio**:
traduce el comando a objetos de valor, delega la decisión al agregado y coordina los puertos. Su
construcción es por constructor, sin anotaciones.

### 6.4 Vista de componentes (C4 N3)

<!-- figura:docs/diagramas/c4-n3-componentes.png | Figura 5. Componentes del hexágono de question-service (C4 N3). -->

### 6.5 Mapeo objeto–relacional

`QuestionRecord` es la entidad JPA y `AnswerOptionRecord` su `@Embeddable`. La traducción ocurre
exclusivamente en `QuestionDomainMapper`; el dominio no importa `jakarta.persistence` ni conoce
columnas o tablas.

## 7. Servicios y contratos REST

### 7.1 `validation-service` (:8082)

<!-- tabla: Tabla 9. Contrato REST de validation-service. -->

| Método | Ruta | Éxito | Error |
|---|---|---|---|
| POST | `/api/validation/questions` | `200 {"valido":true,"errores":[]}` | `400 {"valido":false,"errores":[...]}` |
| GET | `/actuator/health` | `200 {"status":"UP"}` | — |

El pipeline ejecuta cuatro filtros: contenido, justificación, opciones y clave correcta.

### 7.2 `question-service` (:8083)

<!-- tabla: Tabla 10. Contrato REST de question-service. -->

| Método | Ruta | Códigos |
|---|---|---|
| POST | `/api/questions` | `201` / `400` / `503` |
| POST | `/api/questions/{id}/evaluar` *(alias `/enviar-a-revision`)* | `202` / `400` / `404` |
| GET | `/api/questions/{id}` | `200` / `404` |
| GET | `/actuator/health` | `200` |

Modelo de error uniforme:

```json
{ "error": "REGLA_NEGOCIO", "mensaje": "La pregunta debe tener exactamente una clave correcta." }
```

<!-- tabla: Tabla 11. Mapeo de errores HTTP. -->

| Código | Origen |
|---|---|
| `400` | `ReglaNegocioException` (invariante o rechazo estructural) |
| `404` | `QuestionNotFoundException` |
| `503` | `ValidationServiceUnavailableException` |
| `500` | error no controlado |

### 7.3 `lifecycle-service` (:8084)

<!-- tabla: Tabla 12. Contrato REST de lifecycle-service. -->

| Método | Ruta | Códigos |
|---|---|---|
| GET | `/api/lifecycle/questions/{id}` | `200` con el estado registrado / `404` |

## 8. Mensajería AMQP

### 8.1 Topología

<!-- tabla: Tabla 13. Topología AMQP declarada. -->

| Elemento | Nombre | Tipo |
|---|---|---|
| Exchange | `saberpro.questions.exchange` | topic, durable |
| Routing key | `question.review.requested` | — |
| Cola | `saberpro.lifecycle.question-review` | durable |
| DLX | `saberpro.questions.dlx` | fanout, durable |
| DLQ | `saberpro.lifecycle.question-review.dlq` | durable |

### 8.2 Contrato del evento (versión 1)

```json
{
  "eventId": "b7f1c2d4-0000-4000-8000-abcdef123456",
  "eventType": "QuestionReviewRequested",
  "version": 1,
  "occurredAt": "2026-11-13T15:04:05Z",
  "questionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "estado": "PENDIENTE_REVISION",
  "numeroOpciones": 4
}
```

Campos obligatorios: `eventId`, `eventType`, `occurredAt`, `questionId`, `estado`. El mensaje se
serializa como JSON plano y se envía como `Message` con `contentType: application/json`, sin
cabecera de tipo Java.

### 8.3 Publicación

`RabbitMQPublisher` construye el evento después de que la transición fue persistida. El
`RabbitTemplate` tiene un `RetryTemplate` con 3 intentos y backoff exponencial (1 s, 2 s, 4 s). Si
todos fallan, el adaptador registra el error y **no** revierte la transición: la consistencia es
eventual y el evento puede republicarse.

### 8.4 Consumo, reintentos e idempotencia

`QuestionReviewListener` declara `@RabbitListener` sobre la cola principal, deserializa el evento y
lo registra en `QuestionLifecycleRegistry`. La configuración del contenedor usa `ack` automático,
3 reintentos y `default-requeue-rejected: false`, de modo que un mensaje veneno termina en la DLQ.
Antes de procesar, verifica si el `eventId` ya existe; si existe, descarta el duplicado y lo
registra en el log.

### 8.5 Evidencia real del flujo asíncrono

Ejecución local con un broker RabbitMQ 3.13 real (sin Docker), tres JARs arrancados y el script
`docs/evidencias/ejecutar-e2e-amqp-local.sh`:

```text
E2E-01 POST /api/questions -> HTTP 201 (id=9b1d39f5-ee82-4a7a-9857-da8ee709224c)
E2E-06 POST /api/questions/{id}/evaluar -> HTTP 202
E2E-08 GET /api/lifecycle/questions/{id} -> HTTP 200
RabbitMQ queue: saberpro.lifecycle.question-review | consumers: 1 | messages: 0
  durable: True | dlx: saberpro.questions.dlx
Eventos procesados por el listener: 1
E2E-10 reenvio del mismo eventId: descartado por idempotencia (OK)
```

Log del consumidor:

```text
Evento 92314fb2-cf34-40c3-841a-491855afae57 procesado: pregunta 9b1d39f5-... registrada
en el contexto de ciclo de vida (estado=PENDIENTE_REVISION, opciones=4, ocurrido=...)
Evento 92314fb2-cf34-40c3-841a-491855afae57 ya procesado; se descarta el duplicado (idempotencia)
```

## 9. Contenerización y orquestación

### 9.1 Dockerfiles

Los tres servicios usan construcción multi-etapa:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd -r -u 1001 -m appuser
COPY --from=build /build/target/*.jar app.jar
USER appuser
EXPOSE 8083
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=12 \
  CMD curl -fsS http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
```

Se instala `curl` porque la imagen `-slim` no lo incluye y el healthcheck HTTP es más fiable que un
chequeo TCP (decisión A15). El usuario `appuser` (uid 1001) evita ejecutar como *root*.

### 9.2 Docker Compose

<!-- tabla: Tabla 14. Servicios de Docker Compose. -->

| Servicio | Imagen / build | Puertos | Dependencias |
|---|---|---|---|
| `rabbitmq` | `rabbitmq:3.13-management` | 5672, 15672 | — |
| `postgres` | `postgres:16-alpine` | 5432 | — |
| `validation-service` | build `./validation-service` | 8082 | — |
| `question-service` | build `./question-service` | 8083 | postgres, rabbitmq, validation |
| `lifecycle-service` | build `./lifecycle-service` | 8084 | rabbitmq |

Red: `saberpro-net` con `driver: bridge`. Volúmenes: `postgres_data`, `rabbitmq_data`. Los
healthchecks de Postgres (`pg_isready`) y RabbitMQ (`rabbitmq-diagnostics ping`) condicionan el
arranque de los servicios dependientes.

### 9.3 Puertos y credenciales

<!-- tabla: Tabla 15. Puertos y credenciales. -->

| Recurso | Puerto | Credenciales |
|---|---|---|
| `validation-service` | 8082 | — |
| `question-service` | 8083 | — |
| `lifecycle-service` | 8084 | — |
| RabbitMQ AMQP | 5672 | `saberpro` / `saberpro` |
| RabbitMQ Management | 15672 | `saberpro` / `saberpro` |
| PostgreSQL *(opcional)* | 5432 | `saberpro` / `saberpro` |

### 9.4 Comandos canónicos

```bash
docker compose up -d --build     # construir y levantar
docker compose ps                # estado y salud
docker compose logs -f           # logs agregados
docker compose down              # detener
docker compose down -v           # detener y borrar volúmenes
```

### 9.5 Limitación de ejecución en el entorno de desarrollo

En la máquina donde se preparó la entrega, el usuario no tiene acceso al socket de Docker
(`/var/run/docker.sock: permission denied`) y `sudo` exige contraseña; tampoco hay Docker rootless
ni Podman. Por ello:

- **Ejecutado realmente:** `docker compose config --quiet` (exit 0), compilación y empaquetado de
  los tres JARs, arranque local de los tres servicios, flujo REST y AMQP completo con broker real,
  suites de prueba y reglas de arquitectura.
- **Pendiente de ejecución (script listo):** `docker compose up -d --build`, `docker compose ps`,
  `docker images`, `docker network inspect` y el flujo E2E sobre contenedores. El script
  `docs/evidencias/e2e-docker.sh` genera los artefactos `06-*`, `07-*` y `08-*` cuando se ejecute en
  una máquina con Docker.

Esta limitación se declara explícitamente para no presentar como ejecutado algo que no lo fue.

## 10. Flujo integrador end-to-end

### 10.1 Secuencia

1. El cliente envía `POST /api/questions`.
2. `question-service` llama por REST a `validation-service`.
3. Si la validación estructural es correcta, el agregado se crea en `BORRADOR` y se persiste.
4. El cliente envía `POST /api/questions/{id}/evaluar`.
5. El agregado valida INV-03/INV-04/INV-07, pasa a `PENDIENTE_REVISION` y se persiste.
6. `RabbitMQPublisher` publica `question.review.requested`.
7. `lifecycle-service` consume, registra el estado y descarta duplicados por `eventId`.
8. El cliente consulta `GET /api/lifecycle/questions/{id}` y obtiene el estado procesado.

### 10.2 Resultados de los escenarios

<!-- tabla: Tabla 16. Resultados de los escenarios E2E. -->

| ID | Escenario | Esperado | Resultado real | Evidencia |
|---|---|---|---|---|
| E2E-01 | Creación válida | 201 + `BORRADOR` | ✅ 201 | `04-resumen-local.txt` (01/02-crear) |
| E2E-02 | Enunciado vacío | 400 + INV-01 | ✅ 400 | `01-curl-enunciado-vacio.json` |
| E2E-03 | Justificación de 14 caracteres | 400 + INV-02 | ✅ 400 | `01-curl-justificacion-corta.json` |
| E2E-04 | Tres opciones | 400 | ✅ 400 | `01-curl-tres-opciones.json` |
| E2E-05 | Dos claves correctas | 400 + INV-04 | ✅ 400 | `01-curl-dos-claves.json` |
| E2E-06 | Evaluación válida | 202 + `PENDIENTE_REVISION` | ✅ 202 | `05-evaluar.json` |
| E2E-07 | Evento consumido | consumer y log del listener | ✅ 1 consumer, 1 evento | `05-rabbit-queue.json`, `05-listener-log.txt` |
| E2E-08 | Estado en lifecycle | 200 con estado | ✅ 200 | `05-lifecycle-get.json` |
| E2E-09 | Id inexistente | 404 | ✅ 404 | `01-curl-inexistente.json` |
| E2E-10 | Idempotencia | procesado una sola vez | ✅ duplicado descartado | `05-idempotencia-log.txt` |
| E2E-11 | Intercambiabilidad | mismos resultados con memoria | ✅ 201/400/202/404 idénticos | `04-resumen-local.txt` (bloques 01 y 02) |

La colección Postman `docs/colecciones/saberpro-e2e.json` reproduce los 11 escenarios con
aserciones sobre códigos y cuerpos.

### 10.3 Evidencia HTTP real (fragmento)

```text
01-crear             POST http://localhost:8083/api/questions -> HTTP 201
01-enunciado-vacio   POST http://localhost:8083/api/questions -> HTTP 400
01-justificacion-corta POST http://localhost:8083/api/questions -> HTTP 400
01-tres-opciones     POST http://localhost:8083/api/questions -> HTTP 400
01-dos-claves        POST http://localhost:8083/api/questions -> HTTP 400
01-evaluar           POST http://localhost:8083/api/questions/{id}/evaluar -> HTTP 202
01-reevaluar         POST http://localhost:8083/api/questions/{id}/evaluar -> HTTP 400
01-consultar         GET  http://localhost:8083/api/questions/{id} -> HTTP 200
01-inexistente       GET  http://localhost:8083/api/questions/{uuid} -> HTTP 404
```

## 11. Intercambiabilidad

### 11.1 Protocolo

**Paso 1 — adaptador JPA activo.** Perfil `h2` en local y `postgres` en Compose. El log de arranque
reporta `Adaptador de persistencia activo: JPA(h2)`. La suite de contrato y el contrato REST se
ejecutan contra H2: `Tests run: 17`.

**Paso 2 — cambio solo por configuración.** Se activa `QUESTION_REPOSITORY_ADAPTER=memory` y el
perfil `memory` (que excluye `DataSource`, Hibernate y los repositorios Spring Data). El log reporta
`Adaptador de persistencia activo: MEMORIA(HashMap)`. No se modifica ninguna línea de `domain` ni de
`application`.

**Paso 3 — comportamiento idéntico.** Se repite la misma suite y la misma secuencia de peticiones.
`Tests run: 17` en ambos casos y la misma secuencia de códigos HTTP.

### 11.2 Integridad del dominio

```text
sha256sum -c docs/evidencias/domain.sha256
... 17 archivos: La suma coincide
```

El archivo `docs/evidencias/domain.sha256` se generó **antes** del cambio de adaptador. La
verificación posterior es idéntica y `git diff --exit-code` sobre `domain/` no produce salida.

### 11.3 Tabla comparativa

<!-- tabla: Tabla 17. Comparativa de adaptadores de persistencia. -->

| Aspecto | `JpaQuestionAdapter` | `InMemoryQuestionAdapter` | ¿Idéntico? |
|---|---|---|---|
| Puerto implementado | `PreguntaRepositoryPort` | `PreguntaRepositoryPort` | ✅ |
| `POST /api/questions` | 201 + `BORRADOR` | 201 + `BORRADOR` | ✅ |
| INV-01 / INV-02 | 400 + mismo mensaje | 400 + mismo mensaje | ✅ |
| INV-03 / INV-04 | 400 + mismo mensaje | 400 + mismo mensaje | ✅ |
| `POST .../evaluar` | 202 + `PENDIENTE_REVISION` | 202 + `PENDIENTE_REVISION` | ✅ |
| Evento RabbitMQ + listener | procesado | procesado | ✅ |
| Pruebas de contrato | 17 verdes | 17 verdes | ✅ |
| Líneas cambiadas en `domain` | — | **0** | ✅ |

## 12. Pruebas

### 12.1 Resumen de ejecución

<!-- tabla: Tabla 18. Resumen de pruebas ejecutadas. -->

| Servicio / suite | Pruebas | Resultado | Archivo |
|---|---|---|---|
| `question-service` (suite completa) | 75 | ✅ 0 fallos | `03-mvn-test-all.log` |
| Contrato e integración JPA (H2) | 17 | ✅ | `03-mvn-test-jpa.log` |
| Contrato e integración memoria | 17 | ✅ | `03-mvn-test-memory.log` |
| `validation-service` | 13 | ✅ | `03-mvn-test-validation.log` |
| `lifecycle-service` | 11 | ✅ | `03-mvn-test-lifecycle.log` |
| ArchUnit | 4 reglas | ✅ | `03-archunit.log` |

### 12.2 Cobertura de invariantes (T-01…T-17)

<!-- tabla: Tabla 19. Cobertura de invariantes (T-01…T-17). -->

| Test | Invariante | Escenario |
|---|---|---|
| T-01/02/03 | INV-01 | `null`, `""`, `"   "` y valor válido |
| T-04/05/06 | INV-02 | 14 caracteres, 15 exactos y `null` |
| T-07/08 | INV-05 | opción vacía y opción válida inmutable |
| T-09 | INV-06 | `Question.crear` inicia en `BORRADOR` |
| T-10/11 | INV-03 | 3 y 5 opciones al evaluar |
| T-12/13 | INV-04 | 0 y 2 claves correctas |
| T-14 | INV-03+04 | 4 opciones y 1 clave → `PENDIENTE_REVISION` |
| T-15 | INV-08 | `getOpciones().add(...)` lanza `UnsupportedOperationException` |
| T-16 | — | igualdad por valor de VO e identidad por `id` |
| T-17 | INV-07 | doble evaluación lanza `ReglaNegocioException` |

### 12.3 Pruebas de contrato e integración

`PreguntaRepositoryPortContractTest` es una clase abstracta con cinco pruebas
(`save→findById`, id inexistente, actualización de estado, preservación de opciones y de la clave,
guardado repetido). Se ejecuta mediante dos subclases:

- `InMemoryQuestionRepositoryContractTest`
- `JpaQuestionRepositoryContractTest` (`@DataJpaTest` + H2)

El contrato REST se ejecuta mediante `QuestionRestControllerContractTest` y sus subclases
`QuestionRestControllerH2Test` y `QuestionRestControllerMemoryTest`, que verifican 201, 400, 202 y
404 con el mismo conjunto de aserciones.

### 12.4 Reglas de arquitectura

```java
ArchRule dominio_puro = noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat().resideInAnyPackage(
        "org.springframework..", "jakarta.persistence..", "javax.persistence..",
        "org.hibernate..", "com.fasterxml.jackson..", "java.sql..",
        "..infrastructure..", "..application..");

ArchRule application_sin_infrastructure = noClasses()
    .that().resideInAPackage("..application..")
    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
```

Además se verifica que todo `*Port` y `*UseCase` sea interfaz y que todo adaptador viva en
`infrastructure`. Las cuatro reglas se ejecutan dentro de pruebas JUnit 5 y el reporte de Surefire
las contabiliza (`Tests run: 4`).

### 12.5 Pureza del dominio

```text
$ grep -RInE "org\.springframework|jakarta\.persistence|javax\.persistence|..." \
    question-service/.../domain
OK: DOMINIO PURO
```

Los únicos *imports* externos del dominio son `java.util`, `java.time` y `java.util.UUID`.

## 13. Trazabilidad semestral

<!-- tabla: Tabla 20. Matriz de trazabilidad semestral. -->

| Corte | Enfoque | Hitos | Peso | Evidencia actual | Tag |
|---|---|---|---|---|---|
| **I** | Monolito limpio & SOLID | Pipeline HU03 (Pipes & Filters), capas limpias, extensión por plugins | 25% | `validation-service/pipeline/`, `docs/trazabilidad/corte-I.md` | `corte-I` |
| **II** | Microservicios & GoF | Bounded contexts, REST, AMQP, Builder/State/Adapter, JPA | 35% | Los 3 servicios, `state/`, `Question.builder()` | `corte-II` |
| **III** | DDD, Hexagonal & Docker | Dominio puro, puertos/adaptadores, intercambiabilidad, Docker | 40% | `domain/**`, `domain.sha256`, Dockerfiles, Compose, C4 | `corte-III` |

Detalle completo en `docs/trazabilidad/matriz-semestral.md`.

## 14. Decisiones y ambigüedades

<!-- tabla: Tabla 21. Decisiones y ambigüedades registradas. -->

| # | Ambigüedad | Decisión |
|---|---|---|
| A1 | Nombres del Taller 12 vs Taller 13 | Se adoptan los del Taller 13 (`Question`, `AnswerOption`) y se documenta la equivalencia |
| A2 | Paquete base | `co.edu.unicauca.saberpro` |
| A3 | H2 vs PostgreSQL | Adaptador agnóstico: `postgres` en Compose, `h2` en desarrollo y pruebas |
| A5 | `NotificacionPort` opcional | Obligatorio; lo implementa `RabbitMQPublisher` |
| A6 | Semántica de evaluar | Validar INV-03/04/07, persistir, notificar y responder 202 |
| A7 | Tipo de identificador | `QuestionId` sobre `UUID` |
| A8 | ¿INV-03/04 al crear? | Solo en la transición; la forma la valida `validation-service` |
| A9 | Conteo de 15 caracteres | Se rechaza *blank* y se normaliza con `trim` |
| A11 | Validación sin acoplar el dominio | Puerto `ValidacionPreguntaPort` + `ValidationServiceClient` |
| A12 | Código para regla de negocio | 400 |
| A14 | Imagen `openjdk:17-jdk-slim` *deprecated* | Se usa el tag exigido; fallback documentado a `eclipse-temurin` |
| A15 | `HEALTHCHECK` en imagen slim | Se instala `curl` |
| A17 | Sin módulo compartido de eventos | DTO duplicado por servicio + contrato JSON versionado |
| A18 | Estado de lifecycle | Mapa propio del contexto + patrón State |
| A21 | Falta de excepción 404 en el árbol | `QuestionNotFoundException` |
| A22 | Consulta del agregado | Puerto `ConsultarPreguntaUseCase` |
| A23 | Retorno del puerto de validación | `ValidationResult` |
| A24 | Colección de opciones en JPA | `AnswerOptionRecord` (`@Embeddable`) |
| A25 | Estado en lifecycle | Paquete `state/` + `RabbitMQListenerConfig` |
| A27 | Docker no disponible en el entorno | Evidencia Docker entregada como script reproducible y declarada pendiente |
| A28 | Backoff de publicación | `spring-retry` con 3 intentos, 1 s/2 s/4 s |
| A29/A30 | ArchUnit bajo JDK 27 | Reglas evaluadas con JUnit 5 para que Surefire las contabilice |
| A31 | Perfil global vs `@DataJpaTest` | Ejecuciones de evidencia acotadas con `-Dtest` |

## 15. Limitaciones y trabajo futuro

1. **Ejecución Docker pendiente.** No fue posible ejecutar `docker compose up` en el entorno de
   desarrollo. El script `docs/evidencias/e2e-docker.sh` deja el protocolo completo listo.
2. **Persistencia del ciclo de vida en memoria.** Un reinicio de `lifecycle-service` pierde el
   registro. La evolución natural es una base propia del contexto (por ejemplo, MongoDB o
   PostgreSQL con esquema separado), nunca la base de `question-service`.
3. **Sin autenticación.** Los endpoints están abiertos; en producción se requiere OAuth2/JWT y
   control de roles (docente, coordinador).
4. **Publicación tolerante a fallos.** Si el broker está caído, el evento se pierde y solo queda el
   log. Se propone un *outbox* transaccional para garantizar entrega.
5. **Pruebas de contrato con Testcontainers.** No se ejecutaron por la limitación de Docker; están
   previstas para PostgreSQL y RabbitMQ reales.

## 16. Conclusiones

1. El dominio implementado es **independiente de frameworks**: las reglas de arquitectura y el
   `grep` de pureza lo demuestran, y el cambio de adaptador no requirió una sola modificación.
2. La separación en **bounded contexts** permitió que cada servicio evolucione con su propio
   modelo; la comunicación por REST y AMQP mantiene el bajo acoplamiento.
3. El agregado `Question` concentra las invariantes INV-01…INV-08 y rechaza estados inválidos, lo
   que evita datos inconsistentes incluso si un cliente omite la validación de forma.
4. El pipeline **Pipes & Filters** conserva el hito del Corte I y separa la validación estructural
   del negocio.
5. La mensajería asíncrona con **idempotencia** y **DLQ** hace el flujo de revisión tolerante a
   duplicados y a fallos del consumidor.
6. La contenerización quedó completamente especificada y validada de forma estática; su ejecución
   efectiva queda pendiente por una restricción del entorno, documentada con transparencia.

## 17. Referencias

[1] E. Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Boston, MA,
USA: Addison-Wesley, 2003.

[2] A. Cockburn, *Hexagonal Architecture (Ports and Adapters)*, 2005. [En línea]. Disponible en:
https://alistair.cockburn.us/hexagonal-architecture/

[3] R. C. Martin, *Clean Architecture: A Craftsman's Guide to Software Structure and Design*.
Boston, MA, USA: Prentice Hall, 2017.

[4] M. Fowler, *Pipes and Filters*, 2015. [En línea]. Disponible en:
https://www.enterpriseintegrationpatterns.com/patterns/messaging/PipesAndFilters.html

[5] G. Hohpe y B. Woolf, *Enterprise Integration Patterns*. Boston, MA, USA: Addison-Wesley, 2003.

[6] E. Gamma, R. Helm, R. Johnson y J. Vlissides, *Design Patterns: Elements of Reusable
Object-Oriented Software*. Boston, MA, USA: Addison-Wesley, 1994.

[7] Pivotal Software, *Spring Boot Reference Documentation*, 2024. [En línea]. Disponible en:
https://docs.spring.io/spring-boot/docs/current/reference/html/

[8] RabbitMQ, *AMQP 0-9-1 Model Explained*, 2024. [En línea]. Disponible en:
https://www.rabbitmq.com/tutorials/amqp-concepts.html

[9] Docker Inc., *Docker Compose file reference*, 2024. [En línea]. Disponible en:
https://docs.docker.com/compose/compose-file/

[10] S. Brown, *The C4 model for visualising software architecture*, 2023. [En línea]. Disponible
en: https://c4model.com/

## 18. Anexos

### Anexo A — Estructura del repositorio

```text
saberpro-banco-preguntas/
├── docker-compose.yml
├── README.md
├── AGENTS.md
├── .gitignore
├── docs/
│   ├── diagramas/
│   ├── evidencias/
│   ├── trazabilidad/
│   ├── colecciones/
│   └── informe/
├── question-service/
├── validation-service/
└── lifecycle-service/
```

### Anexo B — Comandos de verificación

```bash
# Compilar y probar cada servicio
mvn -f question-service/pom.xml   clean test
mvn -f validation-service/pom.xml clean test
mvn -f lifecycle-service/pom.xml  clean test

# Pureza del dominio
grep -RInE "org\.springframework|jakarta\.persistence|javax\.persistence|..." \
  question-service/src/main/java/co/edu/unicauca/saberpro/question/domain

# Integridad del dominio
sha256sum -c docs/evidencias/domain.sha256
git diff --exit-code -- question-service/src/main/java/co/edu/unicauca/saberpro/question/domain

# Compose (requiere Docker)
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

### Anexo C — Evidencias entregadas

<!-- tabla: Tabla 22. Evidencias entregadas. -->

| Evidencia | Archivo |
|---|---|
| HTTP 201/400/202/404 con JPA y memoria | `docs/evidencias/04-resumen-local.txt` |
| Cuerpos de respuesta por escenario | `docs/evidencias/0{1,2,5}-*.json` |
| Flujo AMQP real e idempotencia | `docs/evidencias/05-resumen-e2e.txt` |
| Suites de prueba | `docs/evidencias/03-mvn-test-*.log` |
| ArchUnit | `docs/evidencias/03-archunit.log` |
| Pureza | `docs/evidencias/03-dominio-puro.log` |
| Integridad del dominio | `docs/evidencias/domain.sha256`, `03-domain-integridad.txt` |
| Diagramas C4 | `docs/diagramas/` |
| Colección Postman | `docs/colecciones/saberpro-e2e.json` |
| Script de evidencias Docker | `docs/evidencias/e2e-docker.sh` |
