# Tasks

## 1. Modelar e consultar lugares persistidos

- [x] 1.1 Mapear a entidade `Place` para a tabela `places`, incluindo seus campos escalares e o relacionamento proprietário `ManyToOne` lazy com `Destination`; verificar que Hibernate valida o mapeamento existente sem migration.
- [x] 1.2 Criar consulta paginada no repositório para lugares do destino da viagem, filtrando `removed_at IS NULL` e ordenando por `name` e `id`; verificar resultados, filtro e limites de página com teste de persistência PostgreSQL.

## 2. Completar a API e documentar o contrato

- [x] 2.1 Implementar no service a resolução da viagem, autenticação/propriedade e consulta de lugares com o contrato de card da spec; verificar viagem inexistente, usuário suspenso, não proprietário e acesso correto por testes de API.
- [x] 2.2 Completar `GET /trips/{id}/places` com `page=0`, `size=20`, máximo de 100, DTO explícito de card e metadados de paginação; verificar JSON, página vazia, ordenação e parâmetros inválidos por testes de API.
- [x] 2.3 Documentar a rota e seus campos no `README.md`; verificar a documentação contra o contrato aprovado e executar `./mvnw clean verify` a partir de `trip.share/`.

## 3. Validar a integração com PostgreSQL

- [x] 3.1 Executar `./mvnw verify -Pdatabase-it` a partir de `trip.share/` e confirmar que migrations, validação JPA e consulta paginada passam contra PostgreSQL 16.
