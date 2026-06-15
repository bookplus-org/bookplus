#!/usr/bin/env bash
# Adds Micrometer Tracing + Zipkin exporter to every service pom.xml
# Run once: bash scripts/add-tracing-deps.sh

SERVICES=(
  "book-plus-auth-service"
  "book-plus-catalog-service"
  "book-plus-inventory-service"
  "book-plus-cart-service"
  "book-plus-order-service"
  "book-plus-payment-service"
  "book-plus-notification-service"
  "book-plus-report-service"
  "book-plus-admin-bff"
)

TRACING_DEPS='
        <!-- Distributed Tracing — Micrometer + Zipkin -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-sender-okhttp3</artifactId>
        </dependency>'

MARKER="<!-- Actuator -->"

for svc in "${SERVICES[@]}"; do
  POM="$svc/pom.xml"
  if [ ! -f "$POM" ]; then
    echo "⚠️  Not found: $POM"
    continue
  fi
  if grep -q "micrometer-tracing" "$POM"; then
    echo "✓  Already has tracing: $svc"
    continue
  fi
  # Insert tracing deps before the Actuator dependency
  sed -i.bak "s|$MARKER|$TRACING_DEPS\n\n        $MARKER|" "$POM"
  rm -f "$POM.bak"
  echo "✅  Added tracing to $svc"
done
echo ""
echo "Done. Restart services to activate tracing."
