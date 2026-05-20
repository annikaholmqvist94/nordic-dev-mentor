# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S -G spring -u 1001 spring
USER spring
WORKDIR /app
COPY --from=builder /workspace/target/dev-mentor-*.jar app.jar
ENV SERVER_PORT=8090
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]
