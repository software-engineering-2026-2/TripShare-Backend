# Design

## Context

Consulte `proposal.md` para a motivação e `specs/destination-places/spec.md` para o contrato observável proposto. O banco já contém `places.destination_id` com FK para `destinations`; o modelo JPA atualmente contém `Destination` e `Trip`, mas não possui entidade `Place` nem repositório de lugares. `TripController` contém `GET /trips/{id}/places`; o stub do service verifica o usuário autenticado, mas referencia `tripId` sem definição e retorna `null`.

O projeto usa Java 17, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL/Flyway e identidade Firebase definida pelo filtro de autenticação. Os endpoints de viagem existentes autorizam o proprietário no service. Não há atualmente contrato de resposta paginada nem tipo compartilhado `PageResponse`.

## Goals / Non-Goals

**Goals:**
- Completar a rota existente usando registros persistidos de lugares do destino da viagem.
- Mapear `Place` para a tabela existente e seu relacionamento JPA proprietário obrigatório com `Destination`, sem alterar o esquema.
- Limitar as leituras no banco e retornar uma resposta estável e concisa para cards.

**Non-Goals:**
- Adicionar, atualizar ou excluir lugares; consultar a API do Foursquare neste endpoint; expor atividades de roteiro, horários de funcionamento ou todos os dados de imagens de lugares.
- Adicionar relacionamentos ou modelos de entidade que não sejam necessários para a consulta e a resposta propostas.
- Introduzir migration ou alterar contratos de outros endpoints.

## Decisions

1. **Manter a rota aninhada na viagem.** Resolver primeiro a viagem, verificar usuário ativo e propriedade conforme as convenções atuais do service e consultar lugares por `trip.destination.id`. Uma rota `/places` independente não expressaria o contexto da viagem escolhida e não corresponde ao stub existente.

2. **Mapear somente o lado proprietário da associação.** `Place` mapeará seu `destination_id` não nulo como `@ManyToOne(fetch = LAZY, optional = false)` com `@JoinColumn` correspondente à FK. Não adicionar uma coleção `Destination.places` sem limite: a consulta pode filtrar pela associação proprietária sem carregar todos os lugares de um destino.

3. **Usar paginação no banco e mapear a entidade para o DTO no service.** A consulta do repositório filtrará `destination.id` e `removedAt IS NULL`, ordenará por `name, id` e retornará uma página Spring Data de `Place`; o service converte cada entidade em `PlaceResponse`, e os metadados da página em `TripPlacesResponse`, sem serializar entidades nem expor a serialização de `PageImpl`. A projeção escalar foi considerada, mas descartada em favor do mapeamento explícito no service por simplicidade e por manter o repositório restrito ao modelo.

4. **Contrato de página proposto para revisão.** Os parâmetros são `page` (começa em `0`, padrão `0`) e `size` (padrão `20`, limitado a `100`). O corpo contém `content` e `page`, `size`, `totalElements`, `totalPages` e `last`. Cada card tem `id`, `name`, `address`, `neighborhood`, `description`, `latitude`, `longitude`, `estimatedVisitMinutes`, `estimatedCost` e `currency`. Valores nulos no banco permanecem `null`; moeda não é retornada sem o respectivo custo conhecido. Este formato é uma sugestão, pois o rascunho da API não foi localizado no repositório; deve ser confirmado ou ajustado antes da implementação.

5. **Sem migration.** V1 já define todos os campos escalares usados pelo card proposto, a FK do destino, nulabilidade e marcador de remoção. A validação do Hibernate verificará o mapeamento em relação ao esquema Flyway.

## Risks / Trade-offs

- [Os campos do card ou metadados de paginação propostos podem divergir do rascunho não salvo do usuário] → Revisar e confirmar o contrato antes de implementar; manter DTO e spec alinhados.
- [Mapeamentos JPA podem falhar na validação do Hibernate se tipos/defaults PostgreSQL divergirem] → Seguir o mapeamento existente de `Destination` e verificar com a suíte padrão e o perfil de integração de migrations PostgreSQL.
- [Paginação por número de página pode ficar mais custosa em offsets profundos] → Limitar o tamanho; paginação por cursor não é exigida pela task atual e pode ser considerada se o uso justificar.

## Migration Plan

Não está prevista migration. Adicionar entidade/repositório/service/mapeamento de resposta e testes; depois validar Hibernate e endpoint contra PostgreSQL. O rollback consiste em reverter as alterações da aplicação; o esquema permanece inalterado.
