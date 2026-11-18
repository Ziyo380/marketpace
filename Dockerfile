
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew

COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app

COPY --from=builder /workspace/build/libs/marketplace-backend.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
