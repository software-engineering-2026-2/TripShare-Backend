# Estágio 1: build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Baixa as dependências primeiro (aproveita cache do Docker)
COPY pom.xml .
COPY --chmod=755 mvnw ./mvnw
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline -B

# Copia o código e gera o JAR
COPY src ./src
RUN ./mvnw clean verify -B

# Estágio 2: runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
