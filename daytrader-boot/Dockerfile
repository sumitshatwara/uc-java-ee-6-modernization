# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
ARG MAVEN_MIRROR_URL
RUN if [ -n "$MAVEN_MIRROR_URL" ]; then \
      mkdir -p /root/.m2 && \
      printf '<settings><mirrors><mirror><id>build-mirror</id><mirrorOf>central</mirrorOf><url>%s</url></mirror></mirrors></settings>' "$MAVEN_MIRROR_URL" > /root/.m2/settings.xml; \
    fi
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 daytrader \
    && useradd --uid 10001 --gid daytrader --create-home daytrader
WORKDIR /app
COPY --from=build --chown=daytrader:daytrader /build/target/daytrader-boot.jar ./daytrader-boot.jar
ENV TZ=UTC \
    JAVA_TOOL_OPTIONS="-Duser.timezone=UTC"
USER daytrader
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent --show-error http://localhost:8080/daytrader/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/daytrader-boot.jar"]
