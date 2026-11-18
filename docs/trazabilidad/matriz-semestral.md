# Matriz de trazabilidad semestral

Tabla normativa de la Entrega Final (§12.4), con la ubicación exacta de cada evidencia dentro de
este repositorio y el tag de Git que la congela.

| Corte | Enfoque arquitectónico | Hitos técnicos | Ponderación | Dónde se evidencia hoy | Tag |
|---|---|---|---|---|---|
| **I** | Monolito limpio & SOLID | Refactorización SOLID, capas MVC limpias, pipeline de validación **HU03 (Pipes & Filters)**, soporte de plugins (**Microkernel**) | **25%** | `validation-service/.../pipeline/` (4 filtros + `ValidationPipeline`), `docs/trazabilidad/corte-I.md`, `docs/evidencias/00-validacion-*.json` | `corte-I` |
| **II** | Microservicios & GoF | Bounded Contexts autónomos, REST síncrono, eventos AMQP RabbitMQ, GoF (**Builder, State, Adapter**), JPA | **35%** | Los 3 servicios, `Question.builder()`, `lifecycle-service/.../state/`, `JpaQuestionAdapter`/`InMemoryQuestionAdapter`/`RabbitMQPublisher`/`ValidationServiceClient`, `QuestionRecord` + `QuestionDomainMapper`, `docs/trazabilidad/corte-II.md` | `corte-II` |
| **III** | DDD, Hexagonal & Docker | Dominio puro Java, puertos/adaptadores, intercambiabilidad InMemory vs JPA, Dockerfile y Docker Compose | **40%** | `question-service/.../domain/**`, `docs/evidencias/03-domain-integridad.txt`, los 3 `Dockerfile`, `docker-compose.yml`, `docs/diagramas/`, `docs/trazabilidad/corte-III.md` | `corte-III` |

## 1. Trazabilidad horizontal (objetivo → artefacto)

| Objetivo integrado (§2) | Artefacto verificable | Prueba / evidencia |
|---|---|---|
| Modelo DDD puro con invariantes encapsuladas | `domain/model/{Question,AnswerOption,Enunciado,Justificacion,QuestionId,EstadoPregunta}.java` | `QuestionTest`, `AnswerOptionTest`, `EnunciadoTest`, `JustificacionTest` |
| Puertos y adaptadores intercambiables | `domain/ports/**`, `infrastructure/{jpa,memory,messaging,client}` | `PreguntaRepositoryPortContractTest` + 2 subclases; `QuestionRestControllerH2Test` / `MemoryTest` |
| Pureza arquitectónica | reglas ArchUnit | `ArquitecturaHexagonalTest` → `03-archunit.log`; `grep` → `03-dominio-puro.log` |
| Validación de forma heredada del Corte I | `validation-service/.../pipeline/filters/` | `ValidationPipelineTest`, `ValidationRestControllerTest` |
| Flujo síncrono REST | `QuestionRestController` + `ValidationServiceClient` | `04-resumen-local.txt` (201/400/404), colección Postman |
| Flujo asíncrono AMQP | `RabbitMQPublisher`, `RabbitMQConfig`, `QuestionReviewListener` | `05-resumen-e2e.txt`, `05-listener-log.txt`, `05-rabbit-queue.json` |
| Idempotencia | `QuestionReviewListener` por `eventId` | `05-idempotencia-log.txt`, `QuestionReviewListenerTest` |
| Ciclo de vida con patrón State | `lifecycle-service/.../state/` | `QuestionLifecycleTest`, `QuestionLifecycleRegistry` |
| Contenerización | 3 `Dockerfile` + `docker-compose.yml` | `e2e-docker.sh` (pendiente de ejecución por limitación A27) |
| Informe y C4 | `docs/diagramas/`, `docs/informe/` | N1–N4 en `.puml/.mmd` + `.svg/.png` |

## 2. Trazabilidad de invariantes

| Invariante | Dónde se implementa | Dónde se prueba |
|---|---|---|
| INV-01 enunciado no nulo/vacío | `Enunciado.of`, `Question.crear` | `EnunciadoTest`, `QuestionTest`, E2E-02 |
| INV-02 justificación ≥ 15 caracteres | `Justificacion.of` | `JustificacionTest` (14/15/`null`), E2E-03 |
| INV-03 exactamente 4 opciones | `Question.enviarARevision` | `QuestionTest` (3 y 5 opciones), E2E-04 |
| INV-04 exactamente 1 clave | `Question.enviarARevision` | `QuestionTest` (0 y 2 claves), E2E-05 |
| INV-05 texto de opción no vacío | `AnswerOption.of` | `AnswerOptionTest` |
| INV-06 estado inicial `BORRADOR` | `Question.crear` | `QuestionTest`, E2E-01 |
| INV-07 no reevaluar | `Question.enviarARevision` | `QuestionTest`, E2E-06b |
| INV-08 lista de opciones inmutable | `Question.getOpciones` | `QuestionTest` (`UnsupportedOperationException`) |

## 3. Trazabilidad de commits

El repositorio se versiona con **Conventional Commits**. Cada tag congela el estado verificable
del corte:

```bash
git log --oneline --decorate
git show --stat corte-I
git show --stat corte-II
git show --stat corte-III
```

| Tag | Contenido congelado |
|---|---|
| `corte-I` | `validation-service` con el pipeline Pipes & Filters y su API REST (HU03). |
| `corte-II` | Los 3 microservicios, REST síncrono, topología AMQP, listener idempotente, patrón State y Builder del agregado. |
| `corte-III` | Dominio puro aislado, contrato de repositorio doble, ArchUnit, Dockerfiles, Compose, C4 y evidencias. |
