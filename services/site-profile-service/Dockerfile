# Multi-stage build (master spec §25): build with the Maven wrapper, run on a
# minimal JRE as a non-root user, no secrets baked in, health endpoint exposed.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ensap && adduser -S ensap -G ensap
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER ensap
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -q -O - http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
