FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY backend/espreitar/mvnw backend/espreitar/pom.xml ./
COPY backend/espreitar/.mvn .mvn
RUN chmod +x mvnw
COPY backend/espreitar/src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /workspace/target/espreitar-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
