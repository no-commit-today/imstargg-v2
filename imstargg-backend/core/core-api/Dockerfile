FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :core:core-api:bootJar -x test --no-daemon && \
    find core/core-api/build/libs -name "*.jar" -not -name "*plain*" -exec cp {} /app.jar \;

FROM eclipse-temurin:21-jre-jammy
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app
COPY --from=builder /app.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
