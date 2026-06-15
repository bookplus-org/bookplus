#!/usr/bin/env bash
# ============================================================================
# Ejecuta los tests + cobertura JaCoCo de todos los servicios Java, cada uno en
# un contenedor Maven desechable, reutilizando la caché de dependencias (.m2)
# en el volumen "bookplus-m2". Al final imprime un resumen PASS/FAIL.
#
# Uso (Git Bash en Windows):   bash run-coverage.sh
# Reporte por servicio:        book-plus-<svc>/target/site/jacoco/index.html
# ============================================================================
set -u

ROOT="C:/proyecto-book-plus"
SERVICES="api-gateway auth-service cart-service catalog-service inventory-service notification-service order-service payment-service"

PASS=""
FAIL=""

for s in $SERVICES; do
  echo ""
  echo "============================================================"
  echo "  mvn test :: $s"
  echo "============================================================"
  if MSYS_NO_PATHCONV=1 docker run --rm \
        -v "$ROOT/book-plus-$s:/app" \
        -v bookplus-m2:/root/.m2 \
        -w /app \
        maven:3.9-eclipse-temurin-21 \
        mvn -B test; then
    PASS="$PASS $s"
  else
    FAIL="$FAIL $s"
  fi
done

echo ""
echo "============================================================"
echo "  RESUMEN"
echo "============================================================"
echo "  PASS:${PASS:- (ninguno)}"
echo "  FAIL:${FAIL:- (ninguno)}"
echo ""
echo "Si hay servicios en FAIL, pásame su salida y alineo los tests."
