FROM eclipse-temurin:21-jre-jammy
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
WORKDIR /app
COPY app.jar app.jar
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
