FROM eclipse-temurin:17-jdk-alpine
COPY docker_run.sh run.sh
COPY build/libs/*.jar app.jar
ENTRYPOINT ["./run.sh"]
