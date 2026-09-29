# syntax=docker/dockerfile:1
# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk@sha256:abb3826b404269a005829b63e2e7bd48a7be32115ab7ba9fa0d8cba834360eef AS build
WORKDIR /app

# Copy Maven wrapper and pom first (better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copy source and build
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre@sha256:e4f018a55645ad204892e44eb35437518d7e108ba2a2dce305024ab371d24876
WORKDIR /app

# eclipse-temurin images run as root by default - add a dedicated non-root
# user. /app/uploads is a mounted volume (see docker-compose.yml), so it
# needs to be owned by that user too, otherwise writes to it fail once we
# drop privileges below.
RUN groupadd --system spring && useradd --system --gid spring spring \
    && mkdir -p /app/uploads && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring
EXPOSE 8080

# No Actuator dependency in this project, so we probe an existing public
# endpoint (see SecurityConfig: /v3/api-docs is permitAll) instead of
# /actuator/health. Swap this for /actuator/health if Actuator gets added.
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD wget -q --spider http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
