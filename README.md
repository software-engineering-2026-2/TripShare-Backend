# TripShare-Backend

Backend do sistema TripShare, desenvolvido durante a disciplina de Engenharia de Software.

O projeto é uma aplicação **Spring Boot 4.1.1** (Java 17) construída com **Maven**, com persistência em
**PostgreSQL**. Todo o código-fonte fica no diretório [`trip.share/`](./trip.share).

## Pré-requisitos

- Java 17+
- Maven Wrapper `./mvnw`, já incluído no projeto (Maven 3.10.0)
- Docker e Docker Compose em execução (para PostgreSQL local ou execução completa em containers)

## Como executar

### Localmente

```bash
cd trip.share
./mvnw spring-boot:run
```

O `spring-boot-docker-compose` inicia somente PostgreSQL e configura a conexão da aplicação local
automaticamente. O serviço `spring` pertence ao perfil Compose `app`, que fica desativado nesse fluxo,
evitando duas aplicações na porta 8080. Não ative esse perfil ao executar a aplicação localmente.

### Com Docker

```bash
cd trip.share
docker compose --profile app up --build
```

Sobem o banco, o migrador e a aplicação; o migrador termina antes de a aplicação iniciar:

| Serviço    | Descrição                                                                | Porta  |
|------------|--------------------------------------------------------------------------|--------|
| `spring`   | API (build a partir do `Dockerfile` do projeto)                          | `8080` |
| `postgres` | PostgreSQL 16, banco `mydatabase` (user/senha `postgres`), volume `pgdata` | `5432` |
| `migrate`  | Flyway valida o histórico e aplica migrations pendentes                 | —      |

Para encerrar:

```bash
docker compose --profile app down
```

A API sobe em `http://localhost:8080`.

## Build e testes

```bash
cd trip.share
./mvnw clean verify
```

O Dockerfile usa o mesmo Maven Wrapper e executa os testes antes de gerar a imagem final com Java 17.
No Windows, substitua `./mvnw` por `mvnw.cmd`.

A persistência usa Spring Data JPA com PostgreSQL. O teste de inicialização ativa o perfil Spring `test`,
com H2 em memória e integração automática com Compose desabilitada. Ele verifica a conexão e a
inicialização do JPA sem depender de Docker; não substitui testes de integração com PostgreSQL.
H2 tem escopo `test` e não é incluído no JAR da aplicação.

Para validar o esquema e migrations em PostgreSQL 16 real (Docker obrigatório):

```bash
./mvnw verify -Pdatabase-it
```

## Banco de dados e migrations

Dentro de `trip.share/`, use:

```bash
./scripts/database.sh start          # inicia e atualiza somente o banco
./scripts/database.sh status         # mostra versões aplicadas e pendentes
./scripts/database.sh restart        # reinicia e verifica migrations novamente
./scripts/database.sh migrate        # aplica novas versões sem reiniciar
./scripts/database.sh start --demo   # inclui dados fictícios, apenas em desenvolvimento
```

Flyway é o responsável pelo esquema; Hibernate apenas valida. Os scripts preservam o volume e
recusam baseline automática em banco não vazio sem histórico. Reiniciar diretamente pelo Docker
Desktop não substitui os comandos acima. Variáveis e credenciais de desenvolvimento estão em
`trip.share/.env.example`; copie para `.env` quando necessário.

Consulte a [modelagem, diagrama e matriz RF-01–RF-32](docs/database.md) e o
[dicionário de dados](docs/database-dictionary.md). Ainda não há entidades JPA ou endpoints de domínio.

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
    ├── Dockerfile          # build com Maven Wrapper em JDK 17 -> runtime JRE 17
    ├── docker-compose.yaml # postgres (5432) e spring (8080, perfil Compose app)
    ├── .dockerignore
    └── HELP.md             # referência gerada pelo Spring Initializr
```

## API

Ainda não há endpoints implementados. Os controllers devem ser criados dentro de
`trip.share/src/main/java/ufcg/software/engineering/trip/share/` (por exemplo, em um subpacote `controller`).
Por padrão a aplicação responde em `http://localhost:8080`.
