# ── Build stage ──
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Runtime stage ──
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

# JVM tuned for 8GB host (friend's cloud).
# G1GC for concurrent low-pause; 2GB max heap leaves room for DB + nginx in same VM.
ENV JAVA_OPTS="-XX:+UseG1GC \
  -Xms512m -Xmx2g \
  -XX:MaxMetaspaceSize=256m \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.jmx.enabled=false"

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
