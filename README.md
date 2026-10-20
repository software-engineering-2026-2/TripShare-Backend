# TripShare-Backend

Backend do sistema TripShare, desenvolvido durante a disciplina de Engenharia de Software.

O projeto é uma aplicação **Spring Boot 4.1.1** (Java 17) construída com **Maven**, com persistência em
**PostgreSQL**. Todo o código-fonte fica no diretório [`trip.share/`](./trip.share).

## Pré-requisitos

- Java 17+
- Maven 3.10+ (ou o Maven Wrapper `./mvnw`, já incluído no projeto)
- Docker e Docker Compose (para execução em container)

## Como executar

### Localmente

```bash
cd trip.share
./mvnw spring-boot:run
```

> O projeto inclui o `spring-boot-docker-compose`: ao iniciar, o Spring procura um arquivo de compose no
> diretório de trabalho e executa `docker compose up` sozinho. Como o `docker-compose.yaml` também define o
> serviço `spring` (que ocupa a porta 8080), isso sobe uma segunda instância da aplicação e gera conflito
> de porta. Para rodar apenas o app na sua máquina, desabilite o suporte a compose:

```bash
cd trip.share
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.docker.compose.enabled=false
```

### Com Docker

```bash
cd trip.share
docker compose up --build
```

Sobem dois serviços:

| Serviço    | Descrição                                                                | Porta  |
|------------|--------------------------------------------------------------------------|--------|
| `spring`   | API (build a partir do `Dockerfile` do projeto)                          | `8080` |
| `postgres` | PostgreSQL 16, banco `mydatabase` (user/senha `postgres`), volume `pgdata` | `5432` |

Para encerrar:

```bash
docker compose down
```

A API sobe em `http://localhost:8080`.

## Estrutura do projeto

```
.
├── README.md               # documentação geral do repositório
├── .gitignore
└── trip.share/             # projeto Spring Boot (Maven)
    ├── pom.xml             # dependências e build (Spring Boot 4.1.1, Java 17)
    ├── mvnw / mvnw.cmd     # Maven Wrapper
    ├── .mvn/wrapper/       # configuração do wrapper
    ├── src/main/java/ufcg/software/engineering/trip/share/
    │   └── Application.java    # ponto de entrada (@SpringBootApplication)
    ├── src/main/resources/
    │   ├── application.properties
    │   ├── static/             # recursos estáticos
    │   └── templates/          # templates de view
    ├── src/test/java/ufcg/software/engineering/trip/share/
    │   └── ApplicationTests.java
    ├── Dockerfile          # build multi-stage (maven:3.9-eclipse-temurin-17 -> eclipse-temurin:17-jre)
    ├── docker-compose.yaml # serviços spring (8080) e postgres (5432)
    ├── .dockerignore
    └── HELP.md             # referência gerada pelo Spring Initializr
```

## API

Ainda não há endpoints implementados. Os controllers devem ser criados dentro de
`trip.share/src/main/java/ufcg/software/engineering/trip/share/` (por exemplo, em um subpacote `controller`).
Por padrão a aplicação responde em `http://localhost:8080`.
