# Proposal

## Why

O backend já expõe uma rota provisória para consultar lugares de uma viagem, mas ainda não retorna dados. Viajantes precisam de uma lista paginada dos lugares do destino, em formato conciso para exibição em cards.

## What Changes

- Completar `GET /trips/{id}/places` como endpoint autenticado, autorizado para o proprietário da viagem, listando lugares associados ao destino da viagem.
- Adicionar o modelo JPA e a consulta de repositório necessários para representar e recuperar lugares persistidos com paginação, respeitando o esquema PostgreSQL e o relacionamento com o destino existentes.
- Retornar uma página estável com dados concisos para cards; os campos sugeridos e o contrato de paginação serão apresentados para revisão antes da implementação.
- Adicionar cobertura de API e persistência para paginação, filtro por destino, autorização e resposta dos cards.

## Capabilities

### New Capabilities
- `destination-places`: Consultar, por meio de uma viagem, lugares persistidos de um destino com resposta paginada e resumida para cards.

### Modified Capabilities
- Nenhuma.

## Impact

- Rota da API: `GET /trips/{id}/places` em `TripController`.
- Modelos JPA, repositório e service em `trip.share/src/main/java/ufcg/software/engineering/trip/share/`.
- Testes em `trip.share/src/test/java/`.
- Tabela `places` existente e sua FK `destination_id`; não se prevê migration.
- O stub atual verifica o proprietário, mas referencia `tripId` sem definição e retorna `null`; esta mudança completa esse comportamento em vez de criar um endpoint `/places` sem vínculo com viagem.
