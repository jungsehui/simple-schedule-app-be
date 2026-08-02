# SSA 단일 JVM 런타임 이미지 (course+notification 통합, jar는 CI에서 ./gradlew :app:bootJar 로 빌드)
# 빌드: docker build -f app/Dockerfile .   (컨텍스트는 반드시 저장소 루트)
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app
COPY app/build/libs/*-SNAPSHOT.jar app.jar

USER spring:spring
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
