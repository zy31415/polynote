# syntax=docker/dockerfile:1
FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat ./
COPY gradle gradle
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
