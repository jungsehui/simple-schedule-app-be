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

ENV JAVA_OPTS="-XX:+UseSerialGC \
  -Xms128m -Xmx256m \
  -Xss512k \
  -XX:MaxMetaspaceSize=80m \
  -XX:ReservedCodeCacheSize=32m \
  -XX:CompressedClassSpaceSize=16m \
  -XX:TieredStopAtLevel=1 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.jmx.enabled=false"

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
