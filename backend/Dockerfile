# =====================================================================================
# Multi-stage build:
#   Stage 1 (build)   - full JDK + Maven, compiles the code into a jar   (~800 MB, thrown away)
#   Stage 2 (runtime) - only a JRE + the jar                              (~200 MB, shipped)
# Same idea as building Angular in a node image and serving dist/ from nginx.
# =====================================================================================

# ---------- Stage 1: build ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom.xml first and download dependencies. Docker caches this layer, so as long as
# pom.xml doesn't change, rebuilds skip the download (like COPY package*.json + npm ci).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests are run separately (see docs) to keep `docker compose up --build` fast.
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run as root inside a container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage: let the JVM size its heap from the container memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
