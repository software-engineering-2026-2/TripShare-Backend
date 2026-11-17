# Referência das rotas

Este diretório documenta as cinco operações de viagens implementadas em `TripController`. Comece pela página da operação para integrar, testar ou modificar uma rota. As regras descritas correspondem ao código e às migrações atuais, incluindo a V8, que permite viagens sem datas fixas.

| Método | Caminho | Finalidade | Documentação | `operationId` |
| --- | --- | --- | --- | --- |
| `POST` | `/trips` | Criar uma viagem e seus dias vazios. | [Criar viagem](trips/create-trip.md) | `createTrip` |
| `PATCH` | `/trips/{id}` | Editar viagem e resolver conflitos ao reduzir sua duração. | [Editar viagem](trips/update-trip.md) | `updateTrip` |
| `GET` | `/trips/{id}/delete-preview` | Consultar os roteiros afetados pela exclusão. | [Consultar prévia de exclusão](trips/preview-trip-deletion.md) | `previewTripDeletion` |
| `DELETE` | `/trips/{id}` | Excluir viagem e dependentes mediante confirmação. | [Excluir viagem](trips/delete-trip.md) | `deleteTrip` |
| `GET` | `/trips/{id}/places` | Listar lugares do catálogo local do destino da viagem. | [Listar lugares](trips/list-trip-places.md) | `getTripPlaces` |

## Como consultar

- [Convenções](conventions.md): autenticação, formatos, paginação, erros compartilhados e preparação dos exemplos.
- [OpenAPI](../openapi.yaml): contrato estruturado de entradas, saídas e exemplos dessas cinco operações. Os `operationId` identificam as operações no contrato; não precisam coincidir com o nome do método Java.
- [Autenticação](../auth.md) e [README do projeto](../../README.md#api): fluxo Firebase e contratos de autenticação e perfis, fora do escopo deste catálogo de viagens.
- [Guia do banco](../database.md) e [dicionário](../database-dictionary.md): contexto da persistência. Para mudanças posteriores ao período coberto por esses documentos, consulte as [migrações](../../trip.share/src/main/resources/db/migration), em especial a V8 para datas e duração.

Os exemplos usam identificadores ilustrativos. Substitua-os por registros do seu ambiente e envie um ID token real do Firebase; os tokens simulados dos testes não autenticam na aplicação normal.

## Manutenção

Antes de alterar uma operação, leia sua página, o contrato OpenAPI e os testes vinculados. Atualize os três no mesmo PR quando houver mudança de comportamento. Uma alteração apenas editorial não exige modificar testes de aplicação.

Mantenha método, caminho e `operationId` consistentes entre o índice e o OpenAPI. O OpenAPI concentra os schemas; as páginas explicam o comportamento e repetem somente os detalhes necessários à leitura da operação. As tabelas Markdown são mantidas manualmente, portanto mudanças de campos também precisam ser refletidas nelas.

Documente comportamento implementado separadamente de limitações e propostas. Preserve os identificadores e mensagens literais da API, mesmo quando estiverem em inglês. Requisitos da especificação inicial (`RF-001`–`RF-012`) e do documento referenciado pelo guia do banco (`RF-01`–`RF-32`) têm numerações diferentes.

## Verificação disponível

Execute a partir de `trip.share/`, com Java 17+ e as dependências Maven disponíveis:

```bash
./mvnw -Dtest=TripApiTest,TripServiceTest,TripPlacesApiTest test
./mvnw -Pdatabase-it -Dit.test=TripManagementPostgresIT verify
```

O segundo comando usa Docker/Testcontainers e PostgreSQL para verificar cascatas, triggers e transações. Os testes com H2 não substituem essa cobertura. Esses testes não validam automaticamente o OpenAPI; ao editar a documentação, verifique também a sintaxe, os `$ref`, os exemplos e os links locais.
