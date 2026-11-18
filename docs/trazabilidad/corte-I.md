# Corte I — Monolito limpio, SOLID y Pipes & Filters (25%)

> Estado: **conservado y reubicado** dentro de `validation-service`, tal como exige §12.4 de la
> entrega final. La versión completa del taller de origen (microkernel + plugins por reflexión)
> vive en el repositorio del curso; aquí se conserva el hito evaluable del corte: el **pipeline de
> validación HU03** y la separación limpia de capas.

## 1. Objetivo del corte

| Hito | Cómo se cumple hoy | Evidencia |
|---|---|---|
| Refactorización SOLID y capas limpias | Capa `api` (transporte) separada de `pipeline` (reglas de forma); sin lógica en el controlador | `validation-service/.../api/ValidationRestController.java` |
| Pipeline de validación **HU03** (Pipes & Filters) | `ValidationPipeline` ejecuta secuencialmente 4 filtros y acumula errores | `pipeline/base/ValidationPipeline.java`, `pipeline/filters/*` |
| Soporte de plugins (Microkernel) | Reconstrucción documentada: en el taller de origen los filtros se descubrían por reflexión desde `plugins.properties`. En la entrega final el mismo contrato (`QuestionFilter`) se inyecta como lista de beans, lo que mantiene el principio de extensión sin introducir un cuarto servicio | `AGENTS.md` §15-A19; `ValidationServiceApplication#validationPipeline` |

## 2. Diseño del pipeline

```
POST /api/validation/questions
        │
        ▼
ValidationRestController ──► ValidationPipeline
                               ├── ContentValidationFilter        (enunciado presente)
                               ├── JustificationValidationFilter  (longitud mínima)
                               ├── OptionsValidationFilter        (cantidad y duplicados)
                               └── CorrectAnswerValidationFilter  (una sola clave)
        │
        ▼
200 {"valido":true,"errores":[]}   |   400 {"valido":false,"errores":[...]}
```

Cada filtro implementa el mismo contrato funcional (`QuestionFilter.validate(ValidationPayload)`)
y devuelve una lista **inmutable** de mensajes. El pipe no corta en el primer error: acumula todo
para que el cliente reciba el reporte completo (decisión de usabilidad documentada en el Javadoc).

### 2.1 Principios SOLID aplicados

| Principio | Dónde se observa |
|---|---|
| **S** — responsabilidad única | Cada filtro valida **una** regla de forma; el pipe solo orquesta |
| **O** — abierto/cerrado | Un nuevo filtro se agrega implementando `QuestionFilter` y registrándolo en el `@Bean`; no se modifica el pipe |
| **L** — sustitución de Liskov | Cualquier `QuestionFilter` es intercambiable dentro de `List<QuestionFilter>` |
| **I** — segregación de interfaces | `QuestionFilter` expone un único método; no obliga a implementar validaciones que no aplican |
| **D** — inversión de dependencias | El controlador depende de `ValidationPipeline`, no de los filtros concretos |

## 3. Evidencias ejecutadas

| Archivo | Contenido |
|---|---|
| `docs/evidencias/00-validacion-valida.json` | Respuesta `200` con `{"valido":true,"errores":[]}` |
| `docs/evidencias/00-validacion-tres-opciones.json` | Respuesta `400` con el error de cantidad de opciones |
| `docs/evidencias/03-mvn-test-validation.log` | `Tests run: 13, Failures: 0, Errors: 0` |
| `docs/evidencias/logs/validation-service.log` | Arranque real y peticiones atendidas |

## 4. Diferencia deliberada con el dominio

El pipeline **no duplica** las invariantes de negocio: verifica forma (campos, longitudes,
cantidad). La decisión final sobre INV-03/INV-04 la toma el agregado `Question` en
`question-service`, que es el único dueño del lenguaje del negocio. Esto evita el antipatrón de
"dos modelos de dominio" y es la razón por la que `validation-service` no importa el paquete
`question.domain`.

## 5. Tag

```bash
git checkout corte-I
mvn -f validation-service/pom.xml test
```
