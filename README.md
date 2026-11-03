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

Coloque o JSON da conta de serviço do Firebase em `.secrets/firebase.json` e a Service API Key da API de locais do Foursquare (texto puro) em `.secrets/foursquare.txt`, na raiz do repositório.
O caminho local padrão considera a execução dentro de `trip.share/`:

```bash
cd trip.share
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.docker.compose.file=../docker-compose.yaml"
```

O `spring-boot-docker-compose` inicia somente PostgreSQL e configura a conexão da aplicação local
automaticamente. O serviço `spring` pertence ao perfil Compose `app`, que fica desativado nesse fluxo,
evitando duas aplicações na porta 8080. Não ative esse perfil ao executar a aplicação localmente.

### Com Docker

```bash
# Na raiz do repositório:
docker compose --profile app up --build
```

A credencial do Firebase é o JSON de uma conta de serviço do projeto usado pelo frontend; a do Foursquare é a Service API Key salva em `.secrets/foursquare.txt` (`FOURSQUARE_KEY_PATH`).
Os arquivos em `.secrets/` são somente para leitura e nunca devem ser versionados. A aplicação exige a credencial do Firebase para iniciar e a do Foursquare ao consultar locais externos; os comandos de banco e
os testes comuns não precisam delas. `FIREBASE_KEY_PATH` configura a
JVM local; `FIREBASE_CREDENTIALS_FILE` configura o caminho no host para Docker. Ambas são opcionais
quando o arquivo está em `.secrets/firebase.json` na raiz: localmente é usado o caminho padrão,
e o Compose monta o arquivo em `/run/secrets/firebase.json` e informa esse caminho automaticamente.

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
`trip.share/.env.example`; copie para `.env` na raiz do repositório quando necessário.

**A V7 faz um reset de desenvolvimento:** remove todos os usuários anteriores ao vínculo Firebase,
suas viagens e dados dependentes, além de senhas, sessões e tokens de recuperação locais. Catálogos e
histórico Flyway permanecem. Antes da primeira aplicação da V7, salve um dump em `.old/`:

```bash
mkdir -p .old
docker compose exec -T postgres sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > .old/before_firebase_v7.sql
```

Execute esse backup na raiz do repositório, com PostgreSQL iniciado e antes de iniciar a aplicação
ou executar `database.sh migrate`. Os seeds usam UIDs fictícios e não habilitam login real.

Consulte a [modelagem, diagrama e matriz RF-01–RF-32](docs/database.md) e o
[dicionário de dados](docs/database-dictionary.md). As entidades `Auth` e `User` atendem ao fluxo de autenticação.

## Estrutura do projeto

```
.
├── README.md               # documentação geral do repositório
├── .gitignore
├── Dockerfile              # build, migrador Flyway e runtime Java 17
├── docker-compose.yaml     # postgres, migrate e spring
├── .dockerignore
└── trip.share/             # projeto Spring Boot (Maven)
    ├── pom.xml             # dependências e build (Spring Boot 4.1.1, Java 17)
    ├── mvnw / mvnw.cmd     # Maven Wrapper
    ├── .mvn/wrapper/       # configuração do wrapper
    ├── src/main/java/ufcg/software/engineering/trip/share/
    │   ├── Application.java    # ponto de entrada (@SpringBootApplication)
    │   └── config/, controller/, dto/, handlers/, models/, repository/, service/
    ├── src/main/resources/
    │   ├── application.properties
    │   ├── static/             # recursos estáticos
    │   └── templates/          # templates de view
    ├── src/test/java/ufcg/software/engineering/trip/share/
    │   └── ApplicationTests.java
    └── HELP.md             # referência gerada pelo Spring Initializr
```

## API

O frontend cadastra/autentica a conta no Firebase e obtém o **ID token**. A API local responde em
`http://localhost:8080`; em ambiente publicado, envie os tokens por HTTPS. O UID Firebase é uma string
opaca, armazenada em `auth.uuid`; os IDs internos de `auth` e `users` são UUIDs distintos.

Todos os endpoints exigem `Authorization: Bearer <firebase-id-token>`. O backend verifica o token.
No cadastro e no login, também exige igualdade exata entre o UID verificado e `uuid` no corpo. Não há
senha nem emissão de token no backend.

### Endpoints disponíveis

| Método | Caminho | Autenticação | Descrição |
|--------|---------|--------------|-----------|
| `POST` | `/auth/register` | Firebase ID token | Cria o perfil local associado à conta Firebase. Retorna `201`. |
| `POST` | `/auth/login` | Firebase ID token | Busca o perfil existente associado ao UID Firebase. Retorna `200`. |
| `GET` | `/users/{id}` | Firebase ID token | Retorna informações públicas do perfil identificado por `id`. |
| `PATCH` | `/users/me` | Firebase ID token | Atualiza campos do perfil do usuário autenticado. |
| `POST` | `/trips` | Firebase ID token | Cria uma viagem vinculada ao usuário autenticado. Retorna `201`. |
| `PATCH` | `/trips/{id}` | Firebase ID token | Atualiza campos da viagem do usuário autenticado (título, destino, datas ou duração). Retorna `200`. |

### Cadastro de perfil

`POST /auth/register`, após a criação da conta no Firebase:

```json
{
  "uuid": "uid-retornado-pelo-firebase",
  "username": "viajante",
  "name": "Nome do usuário",
  "phone": "+5583999999999",
  "bio": "Gosto de viajar",
  "hometown": "Campina Grande"
}
```

`uuid` (até 128 caracteres), `username` (até 50 caracteres) e `name` (até 120) são obrigatórios.
Telefone é opcional e,
quando enviado, deve seguir E.164. `bio` e `hometown` são opcionais. O email vem do token e deve ser
válido, com até 254 caracteres. Papéis e status enviados pelo cliente não alteram os padrões `USER` e `ACTIVE`.

Retorna `201` com o corpo de resposta de usuário abaixo. UID, email ou username já cadastrado retorna
`409`; email e username são únicos sem diferenciar maiúsculas de minúsculas. `auth` e perfil são criados
na mesma transação.

### Login

`POST /auth/login`:

```json
{"uuid":"uid-retornado-pelo-firebase"}
```

Retorna `200` com o perfil existente no formato de resposta abaixo. Perfil ausente retorna `404`; o
frontend deve realizar o cadastro separado. Perfil suspenso retorna `403`. O login não cadastra nem
altera dados automaticamente.

Cadastro e login retornam o mesmo formato:

```json
{
  "id": "UUID interno do usuário",
  "authId": "UUID interno da identidade",
  "uuid": "uid-retornado-pelo-firebase",
  "username": "viajante",
  "name": "Nome do usuário",
  "email": "usuario@example.com",
  "phone": "+5583999999999",
  "bio": "Gosto de viajar",
  "hometown": "Campina Grande",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-10-24T12:00:00Z",
  "updatedAt": "2026-10-24T12:00:00Z"
}
```

### Consulta pública de perfil

`GET /users/{id}` recebe o UUID interno do usuário (`id`) no parâmetro `id` e retorna `username`, `name`, `bio` e
`hometown`. Perfil inexistente retorna `404`; perfil suspenso retorna `403` (`User is suspended`).
Este endpoint exige `Authorization: Bearer <firebase-id-token>`.

Exemplo de resposta (`200`):

```json
{
  "username": "viajante",
  "name": "Nome do usuário",
  "bio": "Gosto de viajar",
  "hometown": "Campina Grande"
}
```

### Atualização do próprio perfil

`PATCH /users/me` atualiza parcialmente o perfil associado à identidade do Firebase autenticada. Envie
somente os campos que deseja alterar; `name`, `phone`, `bio` e `hometown` são opcionais:

```json
{
  "name": "Nome atualizado",
  "phone": "+5583999999999",
  "bio": "Gosto de viajar",
  "hometown": "Campina Grande"
}
```

`name` aceita até 120 caracteres e não pode ser vazio quando informado; `phone` é opcional e, quando
informado, deve seguir E.164 (até 32 caracteres). `bio` aceita até 500 caracteres e `hometown` até 120.
O endpoint retorna `200` com o perfil atualizado no mesmo formato da resposta de cadastro e login:

```json
{
  "id": "UUID interno do usuário",
  "authId": "UUID interno da identidade",
  "uuid": "uid-retornado-pelo-firebase",
  "username": "viajante",
  "name": "Nome atualizado",
  "email": "usuario@example.com",
  "phone": "+5583999999999",
  "bio": "Gosto de viajar",
  "hometown": "Campina Grande",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-10-24T12:00:00Z",
  "updatedAt": "2026-10-27T12:00:00Z"
}
```

### Criação de viagem

`POST /trips` cria uma viagem vinculada ao perfil associado à identidade do Firebase autenticada.

Com intervalo de datas definido (`startsOn` e `endsOn` calculam `durationDays = endsOn - startsOn + 1`):

```json
{
  "title": "Férias no Rio",
  "destinationId": "UUID do destino cadastrado",
  "startsOn": "2027-01-10",
  "endsOn": "2027-01-14"
}
```

Ou informando apenas a duração em dias (`durationDays > 0`, sem `startsOn` e `endsOn`):

```json
{
  "title": "Roteiro de 5 dias no Rio",
  "destinationId": "UUID do destino cadastrado",
  "durationDays": 5
}
```

`title` (não vazio; espaços nas extremidades são removidos com `trim()`) e `destinationId` (UUID existente em `destinations`) são obrigatórios. A viagem deve informar um intervalo válido (`startsOn` e `endsOn` com `endsOn >= startsOn`) ou uma duração positiva (`durationDays > 0`).

Exemplo de resposta (`201`):

```json
{
  "id": "UUID interno da viagem",
  "userId": "UUID interno do usuário proprietário",
  "destinationId": "UUID do destino",
  "destinationName": "Rio de Janeiro",
  "destinationRegion": "Rio de Janeiro",
  "destinationCountryCode": "BR",
  "title": "Férias no Rio",
  "startsOn": "2027-01-10",
  "endsOn": "2027-01-14",
  "durationDays": 5,
  "createdAt": "2026-10-28T12:00:00Z",
  "updatedAt": "2026-10-28T12:00:00Z"
}
```

Quando criada apenas com `durationDays`, os campos `startsOn` e `endsOn` retornam `null`. Datas invertidas (`endsOn < startsOn`), duração inválida/ausente ou datas incompletas retornam `400`; destino ou perfil do usuário inexistente retorna `404`; usuário suspenso retorna `403`.

Na mesma transação, a API cria um registro vazio em `trip_days` para cada dia da viagem, numerado de `1` a `durationDays`. O dia `1` corresponde a `startsOn` e o último a `endsOn`; viagens criadas apenas com `durationDays` possuem os dias numerados, mas sem datas. A restrição `uq_trip_days_number` impede dias duplicados na mesma viagem. Esses dias ficam disponíveis para o gerenciamento de atividades por dia.

### Edição de viagem

O endpoint `PATCH /trips/{id}` permite ao proprietário atualizar parcialmente sua viagem: título, destino, datas ou duração em dias. Todos os campos são opcionais:

```json
{
  "title": "Novo título da viagem",
  "destinationId": "UUID de um destino cadastrado",
  "startsOn": "2027-01-12",
  "endsOn": "2027-01-18",
  "durationDays": 7
}
```

Regras e comportamentos:
- **Propriedade da viagem:** Apenas o proprietário pode editar a viagem. Tentativas de outro usuário retornam `403` (`You do not own this trip`). Viagem inexistente retorna `404` (`Trip not found`).
- **Destino:** Se informado, `destinationId` deve existir na tabela `destinations`; se inexistente, retorna `404` (`Destination not found`).
- **Título:** Deve ter até 120 caracteres e conter ao menos um caractere visível (espaços nas bordas são removidos com `trim()`).
- **Datas e Duração:**
  - Quando datas são informadas, `endsOn` não pode ser anterior a `startsOn` (`400 End date cannot be before start date`).
  - Se datas forem alteradas, a duração é recalculada automaticamente (`endsOn - startsOn + 1`). Se `durationDays` for enviado em conjunto com datas, deve ser compatível com o intervalo.
  - Para viagens que já possuem datas, atualizar apenas `durationDays` ajusta a data final a partir do início existente.
  - Para viagens sem datas fixas (criadas apenas com duração), atualizar `durationDays` atualiza a quantidade de dias da viagem diretamente. Se datas forem passadas para essa viagem, ambas (`startsOn` e `endsOn`) devem ser informadas.
  - **Expansão de período:** Quando o período da viagem é expandido, o sistema cria automaticamente os novos dias vazios correspondentes em `trip_days`.
  - **Redução de período e conflitos:** Ao reduzir o período da viagem, caso existam atividades salvas em dias que ficariam de fora do novo intervalo, a alteração é rejeitada com status `409 Conflict`, retornando os dias conflitantes e o total de atividades a serem realocadas ou removidas (`Cannot shorten trip: activities exist on days outside the new period`). Se os dias excedentes estiverem vazios (sem atividades), eles são removidos com segurança na mesma transação.

Exemplo de resposta de conflito (`409`):

```json
{
  "status": 409,
  "message": "Cannot shorten trip: activities exist on days outside the new period",
  "conflicts": [
    {
      "dayNumber": 4,
      "activityCount": 2
    },
    {
      "dayNumber": 5,
      "activityCount": 1
    }
  ]
}
```

Exemplo de resposta (`200`):

```json
{
  "id": "UUID interno da viagem",
  "userId": "UUID interno do usuário proprietário",
  "destinationId": "UUID do destino",
  "destinationName": "Rio de Janeiro",
  "destinationRegion": "Rio de Janeiro",
  "destinationCountryCode": "BR",
  "title": "Novo título da viagem",
  "startsOn": "2027-01-12",
  "endsOn": "2027-01-18",
  "durationDays": 7,
  "createdAt": "2026-10-28T12:00:00Z",
  "updatedAt": "2026-10-28T12:30:00Z"
}
```

Erros esperados retornam `{"status":400,"message":"Invalid request body"}`, com status e mensagem
correspondentes: `400` para corpo inválido, `401` para token ausente/inválido/expirado ou UID divergente,
`403` para suspensão, `404` para perfil ausente e `409` para duplicidade. Indisponibilidade de Firebase
ou banco retorna `503`; outras violações de integridade retornam `500`, sem expor detalhes internos.

O filtro usa `FirebaseAuth.verifyIdToken`, que valida assinatura, expiração e projeto emissor. Essa
verificação não consulta revogação remota a cada chamada. Renovação de token, recuperação de senha e
logout são gerenciados pelo Firebase no frontend. O backend não armazena tokens ou senhas.
