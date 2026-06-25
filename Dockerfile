# ── Stage 1: Builder ──────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Final (Distroless) ───────────────────────────────────────────────
FROM gcr.io/distroless/java21-debian12

WORKDIR /app

COPY --from=builder /app/target/commerce-service-*.jar app.jar

EXPOSE 8082

CMD ["app.jar"]