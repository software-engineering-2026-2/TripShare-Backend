# Spec Delta

## Purpose

Permite ao viajante consultar, em páginas, os lugares cadastrados para o destino de uma viagem, com dados concisos para apresentação em cards.

## ADDED Requirements

### Requirement: Listar lugares do destino de uma viagem
O sistema SHALL fornecer `GET /trips/{id}/places` com lugares ativos (`removed_at` ausente) associados ao destino da viagem indicada, somente ao proprietário autenticado da viagem.

#### Scenario: Listagem paginada do destino
- **WHEN** o proprietário ativo consulta uma viagem existente com `page` e `size` válidos
- **THEN** a resposta `200` contém somente lugares ativos do destino da viagem na página solicitada, ordenados por nome e identificador para manter ordenação estável
- **AND** cada item contém `id`, `name`, `address`, `neighborhood`, `description`, `latitude`, `longitude`, `estimatedVisitMinutes`, `estimatedCost` e `currency`
- **AND** a resposta contém `page`, `size`, `totalElements`, `totalPages` e `last`

#### Scenario: Página sem resultados
- **WHEN** o proprietário ativo solicita uma página sem lugares correspondentes
- **THEN** o sistema retorna `200`, conteúdo vazio e metadados de paginação consistentes

### Requirement: Proteger a listagem de lugares da viagem
O sistema SHALL autenticar a chamada e limitar o acesso à viagem ao seu proprietário ativo, sem revelar lugares de destinos de outras viagens.

#### Scenario: Viagem inexistente
- **WHEN** o usuário autenticado solicita lugares de uma viagem inexistente
- **THEN** o sistema retorna `404`

#### Scenario: Usuário não é proprietário
- **WHEN** um usuário autenticado tenta consultar os lugares de uma viagem de outro proprietário
- **THEN** o sistema retorna `403` e não retorna os lugares

#### Scenario: Usuário suspenso
- **WHEN** o proprietário autenticado está suspenso
- **THEN** o sistema retorna `403` e não retorna os lugares

### Requirement: Validar parâmetros de paginação da listagem
O sistema SHALL aceitar paginação baseada em índice começando em zero, com tamanho padrão `20` e tamanho máximo `100`.

#### Scenario: Parâmetros ausentes
- **WHEN** o proprietário ativo consulta a rota sem informar `page` e `size`
- **THEN** o sistema retorna a primeira página com até 20 itens

#### Scenario: Parâmetros fora dos limites
- **WHEN** o usuário envia índice negativo ou tamanho menor que um
- **THEN** o sistema retorna `400`

#### Scenario: Tamanho acima do limite
- **WHEN** o usuário solicita tamanho maior que 100
- **THEN** o sistema limita a página a 100 itens
