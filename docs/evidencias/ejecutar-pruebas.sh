#!/usr/bin/env bash
# Produces the real Maven/ArchUnit/purity evidence files under docs/evidencias/.
#
# Note: the shared contract test class is executed through two subclasses, so the full suite
# already covers BOTH adapters. The two adapter-scoped runs below exist to make each adapter
# explicit in a separate log; a global -Dspring.profiles.active is not applied because
# @DataJpaTest requires a DataSource and would fail under the memory profile.
#
# Usage: bash docs/evidencias/ejecutar-pruebas.sh
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVID="$ROOT/docs/evidencias"
mkdir -p "$EVID/logs"

cd "$ROOT"

echo "== 1/7 question-service: suite completa (ambos adaptadores) =="
mvn -f question-service/pom.xml clean test > "$EVID/03-mvn-test-all.log" 2>&1
echo "exit=$?" | tee -a "$EVID/03-mvn-test-all.log"

echo "== 2/7 question-service: contrato e integracion con JpaQuestionAdapter (H2) =="
mvn -f question-service/pom.xml test \
  -Dtest='QuestionRestControllerH2Test,JpaQuestionRepositoryContractTest' \
  > "$EVID/03-mvn-test-jpa.log" 2>&1
echo "exit=$?" | tee -a "$EVID/03-mvn-test-jpa.log"

echo "== 3/7 question-service: contrato e integracion con InMemoryQuestionAdapter =="
mvn -f question-service/pom.xml test \
  -Dtest='QuestionRestControllerMemoryTest,InMemoryQuestionRepositoryContractTest' \
  > "$EVID/03-mvn-test-memory.log" 2>&1
echo "exit=$?" | tee -a "$EVID/03-mvn-test-memory.log"

echo "== 4/7 validation-service =="
mvn -f validation-service/pom.xml clean test > "$EVID/03-mvn-test-validation.log" 2>&1
echo "exit=$?" | tee -a "$EVID/03-mvn-test-validation.log"

echo "== 5/7 lifecycle-service =="
mvn -f lifecycle-service/pom.xml clean test > "$EVID/03-mvn-test-lifecycle.log" 2>&1
echo "exit=$?" | tee -a "$EVID/03-mvn-test-lifecycle.log"

echo "== 6/7 ArchUnit =="
mvn -f question-service/pom.xml test -Dtest=ArquitecturaHexagonalTest \
  > "$EVID/03-archunit.log" 2>&1
echo "exit=$?" | tee -a "$EVID/03-archunit.log"

echo "== 7/7 pureza del dominio =="
{
  echo '$ grep -RInE "org\.springframework|jakarta\.persistence|javax\.persistence|org\.hibernate|java\.sql|com\.fasterxml\.jackson|amqp|RestController" question-service/src/main/java/co/edu/unicauca/saberpro/question/domain'
  if grep -RInE "org\.springframework|jakarta\.persistence|javax\.persistence|org\.hibernate|java\.sql|com\.fasterxml\.jackson|amqp|RestController" \
      question-service/src/main/java/co/edu/unicauca/saberpro/question/domain; then
    echo "FALLO: acoplamiento detectado en el dominio"
  else
    echo "OK: DOMINIO PURO (sin Spring, JPA, Jackson, AMQP, java.sql ni anotaciones REST)"
  fi
  echo
  echo '$ grep -RIn "import " question-service/src/main/java/co/edu/unicauca/saberpro/question/domain | grep -v "co.edu.unicauca" | grep -v "java.util"'
  grep -RIn "import " question-service/src/main/java/co/edu/unicauca/saberpro/question/domain \
    | grep -v "co.edu.unicauca" | grep -v "java.util" || echo "OK: solo imports del propio dominio y java.util"
} > "$EVID/03-dominio-puro.log" 2>&1

sha256sum -c "$EVID/domain.sha256" > "$EVID/03-domain-integridad.txt" 2>&1
echo "exit=$?" >> "$EVID/03-domain-integridad.txt"

echo
echo "== Resumen =="
for log in "$EVID"/03-mvn-test-*.log "$EVID/03-archunit.log"; do
  printf '%-45s %s\n' "$(basename "$log")" \
    "$(grep -E '^\[(INFO|ERROR)\] Tests run: [0-9]+, Failures' "$log" | tail -1)"
done
