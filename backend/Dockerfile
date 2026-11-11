FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 nbbang
COPY --from=build /workspace/build/libs/*.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
