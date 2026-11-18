# Banco de Preguntas Saber PRO

Entrega final integradora de **Ingeniería de Software II** (FIET – Universidad del Cauca):
tres microservicios construidos con **DDD + Arquitectura Hexagonal**, comunicados de forma
**síncrona por REST** y **asíncrona por AMQP**, contenedorizados y orquestados con **Docker
Compose**.

| Curso | Ingeniería de Software II |
|---|---|
| Corte | III – DDD, Arquitectura Hexagonal y Docker |
| Repositorio | `saberpro-banco-preguntas/` |
| Documentación | [`docs/informe/informe-tecnico.md`](docs/informe/informe-tecnico.md) · [`docs/diagramas/`](docs/diagramas) · [`docs/evidencias/`](docs/evidencias) |

---

## 1. Arquitectura en una vista

```
                        +---------------------------+
   Postman / Thunder ->|  question-service  :8083  |  hexagono DDD
                        |  dominio puro + puertos   |
                        +----+----------+-----------+
              REST/JSON |              | AMQP
                        v              v
        +----------------------+   +------------------------+
        | validation-service   |   | RabbitMQ 5672 / 15672  |
        | :8082 Pipes&Filters  |   +-----------+------------+
        +----------------------+               | AMQP
                                               v
                                 +---------------------------+
                                 | lifecycle-service  :8084  |
                                 | listener idempotente+State|
                                 +---------------------------+
```

- **`question-service` (8083)** – núcleo hexagonal. El paquete `domain` no conoce Spring, JPA,
  Jackson ni AMQP. Contiene el agregado `Question`, los objetos de valor, las invariantes
  INV-01…INV-08, 3 puertos de entrada y 3 de salida.
- **`validation-service` (8082)** – validación estructural con **Pipes & Filters** (herencia del
  Corte I, HU03). No duplica reglas de negocio: solo verifica forma.
- **`lifecycle-service` (8084)** – consumidor **idempotente por `eventId`**, con patrón **State**
  sobre su propio bounded context y persistencia en memoria (nunca la BD de `question-service`).
- **PostgreSQL 16** – perfil `postgres` (Docker Compose). **H2** – perfil `h2` (desarrollo y tests).

Diagramas C4 completos (fuente + SVG/PNG) en [`docs/diagramas/`](docs/diagramas):

| Nivel | Fuente | Imagen |
|---|---|---|
| N1 Contexto | `c4-n1-contexto.puml` | `c4-n1-contexto.png` |
| N2 Contenedores | `c4-n2-contenedores.puml` | `c4-n2-contenedores.png` |
| N3 Componentes (hexágono) | `c4-n3-componentes.puml` | `c4-n3-componentes.png` |
| N4 Código (clases + puertos) | `c4-n4-codigo.mmd` | `c4-n4-codigo.png` |
| N4 Máquina de estados | `c4-n4-estados.mmd` | `c4-n4-estados.png` |

---

## 2. Estructura del repositorio

```
saberpro-banco-preguntas/
├── docker-compose.yml          # 5 servicios, red saberpro-net (bridge)
├── README.md
├── AGENTS.md
├── .gitignore
├── docs/
│   ├── diagramas/              # C4 N1-N4 (.puml/.mmd + .svg/.png) + render.sh
│   ├── evidencias/             # salidas reales de tests, HTTP, AMQP y Docker
│   ├── trazabilidad/           # corte-I.md, corte-II.md, corte-III.md
│   ├── colecciones/            # saberpro-e2e.json (Postman, 11 escenarios)
│   └── informe/                # informe-tecnico.md + Formato-Documento.docx
├── question-service/           # :8083  (Dockerfile, .dockerignore, pom.xml, src)
├── validation-service/         # :8082
└── lifecycle-service/          # :8084
```

No existe `pom.xml` raíz: cada servicio es un proyecto Maven independiente (exigencia literal de
la estructura oficial del Taller 13).

---

## 3. Puertos, credenciales y variables

| Servicio | Puerto host | Puerto contenedor | Credenciales / notas |
|---|---|---|---|
| `validation-service` | **8082** | 8082 | sin autenticación |
| `question-service` | **8083** | 8083 | sin autenticación |
| `lifecycle-service` | **8084** | 8084 | sin autenticación |
| RabbitMQ AMQP | **5672** | 5672 | `saberpro` / `saberpro` |
| RabbitMQ Management UI | **15672** | 15672 | `saberpro` / `saberpro` → <http://localhost:15672> |
| PostgreSQL | 5432 *(opcional, inspección)* | 5432 | `saberpro` / `saberpro` · BD `saberpro` |

Variables de entorno relevantes de `question-service`:

| Variable | Valores | Efecto |
|---|---|---|
| `QUESTION_REPOSITORY_ADAPTER` | `jpa` \| `memory` | Selecciona `JpaQuestionAdapter` o `InMemoryQuestionAdapter` |
| `QUESTION_PROFILE` / `SPRING_PROFILES_ACTIVE` | `postgres` \| `h2` \| `memory` | Perfil de persistencia |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` en Compose | Host del broker |
| `VALIDATION_SERVICE_URL` | `http://validation-service:8082` | URL del servicio de validación |
| `RABBIT_HEALTH_ENABLED` | `true` \| `false` | Activa el health indicator de RabbitMQ |

---

## 4. Ejecución con Docker Compose (camino oficial)

Requisitos: Docker Engine 24+ con Compose v2.

```bash
cd saberpro-banco-preguntas

# 1. Construir y levantar los 5 contenedores
docker compose up -d --build

# 2. Verificar estado y salud
docker compose ps

# 3. Logs
docker compose logs -f question-service

# 4. Detener (conserva volumenes)
docker compose down

# 5. Detener y borrar volumenes
docker compose down -v
```

Resultado esperado de `docker compose ps`: `saberpro-validation`, `saberpro-question`,
`saberpro-lifecycle`, `saberpro-rabbitmq` y `saberpro-postgres` en estado `Up`/`healthy`, sobre la
red `saberpro-banco-preguntas_saberpro-net` (driver `bridge`).

> En la máquina donde se preparó esta entrega el socket de Docker no era accesible
> (`permission denied`) y `sudo` exigía contraseña; por eso los artefactos `06-*`/`07-*` de
> `docs/evidencias` se entregan como **script reproducible pendiente de ejecución**
> (`docs/evidencias/e2e-docker.sh`). El resto de la evidencia (REST, AMQP, tests, ArchUnit,
> intercambiabilidad) fue ejecutada realmente en local y está en `docs/evidencias`.

---

## 5. Ejecución local sin Docker (desarrollo)

```bash
# JARs
mvn -f validation-service/pom.xml -q clean package -DskipTests
mvn -f question-service/pom.xml   -q clean package -DskipTests
mvn -f lifecycle-service/pom.xml  -q clean package -DskipTests

# 1) validation-service
java -jar validation-service/target/validation-service-1.0.0.jar --server.port=8082

# 2) lifecycle-service (broker local)
java -jar lifecycle-service/target/lifecycle-service-1.0.0.jar --server.port=8084 \
  --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest

# 3) question-service (H2 + adaptador JPA)
java -jar question-service/target/question-service-1.0.0.jar --server.port=8083 \
  --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest

# 3-bis) question-service con adaptador de memoria (sin base de datos)
java -jar question-service/target/question-service-1.0.0.jar --server.port=8083 \
  --spring.profiles.active=memory --spring.rabbitmq.username=guest --spring.rabbitmq.password=guest
```

Scripts de evidencia listos para ejecutar:

| Script | Qué produce | Estado en esta entrega |
|---|---|---|
| `docs/evidencias/ejecutar-experimento-local.sh` | HTTP 201/400/202/404 con adaptador JPA(h2) y MEMORIA | **Ejecutado** (`04-resumen-local.txt`, `01-*`, `02-*`) |
| `docs/evidencias/ejecutar-e2e-amqp-local.sh` | 202 + consumo real + idempotencia (broker local) | **Ejecutado** (`05-*`) |
| `docs/evidencias/ejecutar-pruebas.sh` | suites Maven, ArchUnit, pureza, hashes | **Ejecutado** (`03-*`) |
| `docs/evidencias/e2e-docker.sh` | E2E completo sobre contenedores + E2E-11 | **Pendiente (requiere Docker)** |

---

## 6. API y ejemplos `curl`

### 6.1 `validation-service`

```bash
curl -i -X POST http://localhost:8082/api/validation/questions \
  -H 'Content-Type: application/json' \
  -d '{
    "enunciado": "¿Cuál es la conclusión del autor del texto?",
    "justificacion": "La clave se sustenta en la tesis central del tercer párrafo.",
    "opciones": [
      {"texto":"La conclusión del autor","correcta":true},
      {"texto":"Una cita textual","correcta":false},
      {"texto":"Un ejemplo del texto","correcta":false},
      {"texto":"Un dato histórico","correcta":false}
    ]
  }'
# 200 {"valido":true,"errores":[]}
```

### 6.2 `question-service`

```bash
# Crear (201 Created / 400 Bad Request)
curl -i -X POST http://localhost:8083/api/questions \
  -H 'Content-Type: application/json' \
  --data-binary @docs/evidencias/payloads/valid.json

# Enviar a revisión (202 Accepted / 400 / 404)
curl -i -X POST http://localhost:8083/api/questions/{id}/evaluar

# Consultar (200 / 404)
curl -i http://localhost:8083/api/questions/{id}

# Salud
curl -s http://localhost:8083/actuator/health
```

### 6.3 `lifecycle-service`

```bash
curl -i http://localhost:8084/api/lifecycle/questions/{id}
# 200 {"questionId":"...","estado":"PENDIENTE_REVISION","eventId":"...","occurredAt":"...","processedAt":"..."}
# 404 si el evento aún no fue consumido
```

Contrato de error uniforme (`question-service`):

```json
{ "error": "REGLA_NEGOCIO", "mensaje": "La justificación debe tener al menos 15 caracteres." }
```

| Código | Cuándo |
|---|---|
| `400` | violación de invariante o rechazo del `validation-service` |
| `404` | `QuestionNotFoundException` (`NO_ENCONTRADO`) |
| `503` | `validation-service` caído (`VALIDACION_NO_DISPONIBLE`) |
| `201` / `202` | creación / transición a `PENDIENTE_REVISION` |

---

## 7. Cambiar de adaptador de persistencia (intercambiabilidad)

El dominio **no se toca**. Solo cambia una variable de entorno:

```bash
# Adaptador JPA (por defecto) con PostgreSQL
docker compose up -d --build question-service

# Adaptador en memoria (HashMap), sin base de datos
QUESTION_REPOSITORY_ADAPTER=memory QUESTION_PROFILE=memory \
  docker compose up -d --build question-service
```

En local:

```bash
QUESTION_REPOSITORY_ADAPTER=memory mvn -f question-service/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=memory
# o
java -jar question-service/target/question-service-1.0.0.jar \
  --spring.profiles.active=memory
```

Verificación de integridad del dominio:

```bash
sha256sum -c docs/evidencias/domain.sha256
git diff --exit-code -- question-service/src/main/java/co/edu/unicauca/saberpro/question/domain
```

Ambos comandos deben terminar con código 0: ninguna línea de `domain` cambia entre adaptadores.

---

## 8. Pruebas

```bash
mvn -f question-service/pom.xml   clean test
mvn -f validation-service/pom.xml clean test
mvn -f lifecycle-service/pom.xml  clean test
```

| Servicio | Contenido |
|---|---|
| `question-service` | dominio (INV-01…INV-08), aplicación con stubs, **contrato de repositorio** ejecutado contra `InMemoryQuestionAdapter` y `JpaQuestionAdapter` (H2), **contrato REST** contra ambos perfiles, `GlobalExceptionHandler`, `RabbitMQPublisher`, **ArchUnit** (dominio puro y aplicación sin infraestructura) |
| `validation-service` | pipeline Pipes & Filters (4 filtros) + controlador REST |
| `lifecycle-service` | listener idempotente, patrón State y controlador de consulta |

Resultados reales de la última ejecución: [`docs/evidencias/03-mvn-test-*.log`](docs/evidencias),
`03-archunit.log`, `03-dominio-puro.log` y `03-domain-integridad.txt`.

---

## 9. Colección E2E (Postman / Thunder Client)

[`docs/colecciones/saberpro-e2e.json`](docs/colecciones/saberpro-e2e.json) contiene los 11
escenarios con aserciones sobre `201`, `400`, `202`, `404`, topología RabbitMQ e idempotencia.

1. Importar en Postman → *Import* → archivo.
2. Verificar variables `{{questionHost}}` (8083), `{{validationHost}}` (8082),
   `{{lifecycleHost}}` (8084) y credenciales del broker.
3. *Run collection* en orden: `E2E-01` guarda `{{questionId}}`, `E2E-08` guarda `{{eventId}}` y
   `E2E-10` reenvía el mismo evento para demostrar idempotencia.

Escenarios: E2E-01 creación válida · E2E-02 enunciado vacío · E2E-03 justificación corta ·
E2E-04 tres opciones · E2E-05 dos claves · E2E-06 evaluación (202) · E2E-07 evento consumido ·
E2E-08 estado en lifecycle · E2E-09 id inexistente · E2E-10 idempotencia · E2E-11 intercambiabilidad.

---

## 10. Mensajería

| Elemento | Nombre |
|---|---|
| Exchange | `saberpro.questions.exchange` (topic, durable) |
| Routing key | `question.review.requested` |
| Cola | `saberpro.lifecycle.question-review` (durable) |
| DLX / DLQ | `saberpro.questions.dlx` (fanout) / `saberpro.lifecycle.question-review.dlq` |

Evento publicado (JSON versionado, sin `__TypeId__` para no acoplar paquetes entre servicios):

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

Reintentos del consumidor: 3 intentos con backoff 1s → 2s → 4s; al agotarse, el mensaje va a la
DLQ (`default-requeue-rejected: false`). Idempotencia: `eventId` ya procesado ⇒ se descarta y se
registra en el log.

---

## 11. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Bind for 0.0.0.0:8083 failed: port is already allocated` | puerto ocupado | `docker compose down` o cambiar el mapeo del puerto |
| `Connection refused` al publicar el evento | RabbitMQ aún no está `healthy` | `docker compose ps`; esperar el healthcheck; revisar `docker compose logs rabbitmq` |
| `question-service` arranca pero `POST` devuelve `503` | `validation-service` no respondió a tiempo | verificar `VALIDATION_SERVICE_URL` y `docker compose logs validation-service` |
| `UnknownHostException: validation-service` en local | se ejecuta fuera de la red Docker | exportar `VALIDATION_SERVICE_URL=http://localhost:8082` |
| `docker compose` falla al construir | sin acceso al registro de imágenes | `docker login` o usar el fallback `eclipse-temurin:17-jdk-jammy` documentado en `AGENTS.md` §15 (A14) |
| El adaptador no cambia | falta el perfil `memory` | usar **ambas** variables: `QUESTION_REPOSITORY_ADAPTER=memory` y `QUESTION_PROFILE=memory` |
| `permission denied` en `/var/run/docker.sock` | usuario fuera del grupo `docker` | `sudo usermod -aG docker $USER && newgrp docker` |

---

## 12. Documentación y evidencias

| Documento | Ruta |
|---|---|
| Informe técnico consolidado (Markdown) | `docs/informe/informe-tecnico.md` |
| Informe en plantilla oficial | `docs/informe/Formato-Documento.docx` |
| Trazabilidad Corte I / II / III | `docs/trazabilidad/` |
| Diagramas C4 N1–N4 | `docs/diagramas/` |
| Evidencias HTTP y AMQP reales | `docs/evidencias/04-resumen-local.txt`, `05-resumen-e2e.txt` |
| Evidencias de pruebas y ArchUnit | `docs/evidencias/03-*` |
| Integridad del dominio | `docs/evidencias/domain.sha256` |
| Script de evidencias Docker | `docs/evidencias/e2e-docker.sh` |
| Colección Postman | `docs/colecciones/saberpro-e2e.json` |

### Regenerar el informe `.docx`

El documento se construye desde `informe-tecnico.md` con `python-docx` y una doble pasada por
LibreOffice para que la tabla de contenido lleve números de página reales:

```bash
PYTHON=/ruta/al/venv/bin/python bash docs/informe/construir-docx.sh
```

Requiere `python-docx`, LibreOffice (`soffice`) y `poppler-utils` (`pdftotext`, `pdfinfo`).
Antes de entregar, reemplace los nombres y códigos de los integrantes (portada, portadilla y
`informe-tecnico.md`) y regenere el documento.
