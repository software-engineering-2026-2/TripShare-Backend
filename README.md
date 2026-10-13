# TripShare-Backend

Backend do sistema TripShare, desenvolvido durante a disciplina de Engenharia
de Software.

## Pré-requisitos

- Go 1.25+
- Docker e Docker Compose (para execução em container)

## Como executar

### Localmente

```bash
go mod download
go run ./cmd/api
```

### Com Docker

```bash
docker compose up --build
```

Para encerrar:

```bash
docker compose down
```

A API sobe em `http://localhost:8080`.

## Estrutura do projeto

```
.
├── cmd/api/              # ponto de entrada (main.go) — inicializa e sobe o servidor Gin
├── internal/
│   ├── handler/          # handlers HTTP do Gin (controllers)
│   ├── model/            # entidades de domínio e structs
│   ├── repository/       # acesso a dados (persistência)
│   └── service/          # regras de negócio
├── tests/                # testes de integração/e2e
├── Dockerfile            # build multi-stage (golang:1.25-alpine -> alpine:3.20)
├── docker-compose.yaml   # orquestra o serviço da API
├── .dockerignore
├── go.mod / go.sum       # dependências do projeto (Gin)
└── README.md
```

## Endpoints

| Método | Rota      | Descrição               | Resposta              |
|--------|-----------|-------------------------|-----------------------|
| GET    | `/health` | Verifica a saúde da API | `200 {"status":"ok"}` |
