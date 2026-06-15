# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar solo el pom primero para cachear dependencias
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

# Instalar Maven
RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copiar fuentes y compilar
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B -q

# ── Stage 2: Runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S bookplus && adduser -S bookplus -G bookplus
USER bookplus

# Copiar JAR desde builder
COPY --from=builder /app/target/*.jar app.jar

# Configuración JVM optimizada para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
