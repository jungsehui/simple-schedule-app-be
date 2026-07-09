# notification 서버 런타임 이미지 (jar는 CI에서 ./gradlew :notification:bootJar 로 빌드)
# 빌드: docker build -f notification/Dockerfile .   (컨텍스트는 반드시 저장소 루트)
#
# ⚠ FCM 서비스 계정 JSON은 이미지에 넣지 않는다.
#   compose에서 /app/config/fcm-service-account.json 으로 마운트하고
#   FCM_KEY_JSON 환경변수로 경로를 주입한다.
#   (FcmConfig가 ClassPathResource 대신 Spring Resource 추상화를 쓰도록 변경 필요 — deploy/README.md 참고)
FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app
COPY notification/build/libs/*-SNAPSHOT.jar app.jar

USER spring:spring
EXPOSE 8081

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
