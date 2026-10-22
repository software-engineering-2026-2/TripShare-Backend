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

Ative Email/Password no Firebase e configure as credenciais do mesmo projeto:

```bash
export FIREBASE_KEY_PATH='file:/caminho/absoluto/firebase-service-account.json'
export FIREBASE_WEB_API_KEY='sua-web-api-key'
export APP_AUTH_SESSION_TTL='PT8H' # opcional
cd trip.share
./mvnw spring-boot:run
```

O Maven executa a aplicação a partir da raiz do repositório, onde fica o Compose. Na IDE,
use essa raiz como working directory, ou informe `SPRING_DOCKER_COMPOSE_FILE` com caminho absoluto.
A credencial deve ficar fora de `src/main/resources`; senhas são verificadas somente pelo Firebase.

O `spring-boot-docker-compose` inicia somente PostgreSQL e configura a conexão da aplicação local
automaticamente. O serviço `spring` pertence ao perfil Compose `app`, que fica desativado nesse fluxo,
evitando duas aplicações na porta 8080. Não ative esse perfil ao executar a aplicação localmente.

### Com Docker

Na raiz, copie `trip.share/.env.example` para `.env`. Configure `FIREBASE_WEB_API_KEY` e
`FIREBASE_CREDENTIALS_FILE` com o caminho absoluto da credencial no host. O JSON é montado somente
para leitura no serviço `spring`, sem entrar no contexto de build, imagem ou JAR.

```bash
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
`trip.share/.env.example`; copie para `.env` na raiz quando necessário. Compose lê `.env`;
para Java local, exporte as variáveis no shell ou configure-as na IDE.

Consulte a [modelagem, diagrama e matriz RF-01–RF-32](docs/database.md) e o
[dicionário de dados](docs/database-dictionary.md). Usuários e sessões possuem entidades JPA e endpoints
de autenticação; endpoints de viagens e interações serão adicionados nas próximas histórias.

## Estrutura do projeto

```
.
├── README.md               # documentação geral do repositório
├── .gitignore
├── Dockerfile              # build e testes em Java 17
├── docker-compose.yaml     # postgres, migrate e spring
├── .dockerignore
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
    └── HELP.md             # referência gerada pelo Spring Initializr
```

## API

A API responde em `http://localhost:8080`. Documentação: `/swagger-ui/index.html`,
`/v3/api-docs` e `/v3/api-docs.yaml`.

### Login e logout

O cadastro deve existir com `users.firebase_uid` correspondente ao UID Firebase. `users.id` é o
UUID local usado nos relacionamentos; o UID Firebase é uma identidade externa, não um UUID.
O cadastro deve salvar esse UID após validar a identidade. O login não cria usuários nem associa contas pelo e-mail.

`POST /auth/login` é público e aceita exatamente uma destas formas:

```json
{"method":"PASSWORD","email":"usuario@example.com","password":"senha"}
```

```json
{"method":"FIREBASE","idToken":"id-token-obtido-pelo-sdk-firebase"}
```

Resposta `200` (valores ilustrativos):

```json
{
  "accessToken": "token-opaco-da-sessao",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-22T20:00:00Z",
  "user": {
    "id": "10000000-0000-0000-0000-000000000001",
    "username": "alex",
    "name": "Alex",
    "email": "usuario@example.com",
    "role": "USER"
  }
}
```

Envie `Authorization: Bearer <accessToken>` nas rotas protegidas. Um ID token Firebase serve somente
para login. O banco guarda apenas SHA-256 do token de sessão. Não há refresh: após 8 horas
(configurável), execute o login novamente.

`POST /auth/logout`, sem corpo, exige esse header e retorna `204`. Revoga imediatamente a sessão
atual, preservando as demais. O cliente descarta o token local e, se usa o SDK Firebase, encerra
também a autenticação no SDK quando quiser sair completamente. O backend não revoga globalmente
os tokens Firebase de outros dispositivos. Bloqueios Firebase são verificados no login;
o status local `SUSPENDED` é conferido também em cada acesso protegido.

As demais rotas exigem sessão, exceto a documentação. Respostas de autenticação usam
`Cache-Control: no-store`. Erros usam `application/problem+json`, com `errorCode`:

| Status | Código | Significado |
|---|---|---|
| 400 | `INVALID_REQUEST` | Corpo inválido, campos ausentes ou credenciais combinadas |
| 401 | `INVALID_CREDENTIALS` | Mensagem única: `Credenciais inválidas.`; inclui conta suspensa ou cadastro/UID ausente |
| 401 | `INVALID_SESSION` | Sessão ausente, expirada ou revogada; executar login novamente |
| 403 | `ACCESS_DENIED` | Usuário sem permissão |
| 503 | `AUTH_PROVIDER_UNAVAILABLE` | Falha de comunicação/configuração do provedor |
| 503 | `DATABASE_UNAVAILABLE` | Banco indisponível ao validar sessão |

Seeds de demonstração não criam contas Firebase nem vínculos automaticamente. Para teste manual,
use uma conta Firebase de desenvolvimento com cadastro local e UID correspondente.
Cadastro, recuperação de senha e renovação automática não fazem parte desta implementação.
