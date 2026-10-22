# Estágio 1: build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Baixa as dependências primeiro (aproveita cache do Docker)
COPY ./trip.share/pom.xml .
COPY --chmod=755 ./trip.share/mvnw ./mvnw
COPY ./trip.share/.mvn ./.mvn
RUN ./mvnw dependency:go-offline -B

# Copia o código e gera o JAR
COPY ./trip.share/src ./src
RUN ./mvnw clean verify -B

# Mesmo motor e dependências de migração da aplicação, em arquitetura nativa.
FROM build AS migrate
ENV FLYWAY_IGNORE_MIGRATION_PATTERNS=""
ENTRYPOINT ["./mvnw", "-o", "-B"]
CMD ["flyway:migrate"]

# Estágio 2: runtime
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
