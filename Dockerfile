FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder
WORKDIR /workspace
COPY pom.xml .
COPY pjb-core/pom.xml pjb-core/pom.xml
COPY pjb-api/pom.xml pjb-api/pom.xml
COPY pjb-core/src pjb-core/src
COPY pjb-api/src pjb-api/src
RUN mvn -q -pl pjb-api -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && rm -rf /var/lib/apt/lists/* \
    && groupadd --system pjb && useradd --system --gid pjb --create-home --home-dir /home/pjb pjb \
    && mkdir -p /app/cds && chown -R pjb:pjb /app /home/pjb
ENV PJB_JVM_PROFILE=balanced
ENV JAVA_OPTS=""
COPY --from=builder /workspace/pjb-api/target/*.jar /app/app.jar
COPY pjb-api/src/main/resources/docker/pjb-runtime.sh /app/pjb-runtime.sh
RUN chmod +x /app/pjb-runtime.sh && chown pjb:pjb /app/app.jar /app/pjb-runtime.sh
USER pjb
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=5 CMD curl -fsS http://localhost:8080/livez | grep -q UP || exit 1
ENTRYPOINT ["/app/pjb-runtime.sh"]
