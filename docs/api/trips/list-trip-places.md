# Listar lugares do destino da viagem

`GET /trips/{id}/places` · `operationId: getTripPlaces`

Retorna uma página de lugares disponíveis no catálogo local do destino da viagem. Esses lugares podem ser usados no planejamento; a resposta não representa as atividades já adicionadas aos roteiros.

[Índice](../README.md) · [Convenções e erros comuns](../conventions.md) · [OpenAPI: TripPlaces e Place](../../openapi.yaml)

## Acesso e entrada

Exige `Authorization: Bearer <ID_TOKEN_DO_FIREBASE>`, perfil ativo e propriedade da viagem. Não recebe body.

| Parâmetro | Local | Tipo | Regra |
| --- | --- | --- | --- |
| `id` | Caminho | UUID obrigatório | Viagem existente do usuário autenticado. |
| `page` | Query | Inteiro opcional | Padrão `0`, começando em zero; não pode ser negativo. |
| `size` | Query | Inteiro opcional | Padrão `20`; deve ser positivo; valores acima de `100` são ajustados para `100`. |

## Regras da consulta

1. Obtém o destino da viagem após validar perfil, situação e propriedade.
2. Consulta `places` por esse `destination_id`, com `removed_at IS NULL`.
3. Ordena por `name` crescente e UUID crescente para desempate, conforme a ordenação do banco.
4. Retorna a página e os totais. Viagem sem lugares disponíveis ou página além dos resultados retorna `200` com lista vazia.

Não há busca por texto, categoria, custo, distância ou ordenação personalizável nessa operação. Ela não chama Foursquare nem `ExternalPlaceClient`, não importa novos lugares e não preenche dados ausentes. Alterar o destino da viagem muda o catálogo retornado em chamadas seguintes.

## Exemplo de chamada

Use as [variáveis dos exemplos](../conventions.md#endereço-conteúdo-e-exemplos):

```bash
curl -i "$BASE_URL/trips/$TRIP_ID/places?page=0&size=20" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN"
```

## Resposta de sucesso

`200 OK`:

```json
{
  "content": [
    {
      "id": "30000000-0000-0000-0000-000000000001",
      "name": "Museu de exemplo",
      "address": "Rua Exemplo, 100",
      "neighborhood": "Centro",
      "description": "Museu de história local",
      "latitude": -22.906800,
      "longitude": -43.172900,
      "estimatedVisitMinutes": 90,
      "estimatedCost": 25.50,
      "currency": "BRL"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

| Campo de `content[]` | Tipo | Pode ser `null`? | Significado |
| --- | --- | --- | --- |
| `id` | UUID | Não | Identificador do lugar no catálogo. |
| `name` | String | Não | Nome do lugar. |
| `address` | String | Sim | Endereço disponível no cadastro. |
| `neighborhood` | String | Sim | Bairro. |
| `description` | String | Sim | Descrição. |
| `latitude` | Número | Sim | Latitude em graus decimais. |
| `longitude` | Número | Sim | Longitude em graus decimais. |
| `estimatedVisitMinutes` | Inteiro | Sim | Duração estimada da visita, em minutos. |
| `estimatedCost` | Número | Sim | Custo estimado na moeda informada. |
| `currency` | String | Sim | Código de moeda de três letras, como `BRL`. |

Valores desconhecidos retornam `null`; não devem ser exibidos como custo zero ou duração zero. Números JSON podem aparecer com quantidades diferentes de casas decimais. As constraints do PostgreSQL mantêm coordenadas em par e custo/moeda em par. A resposta não inclui origem externa, URL, categorias, horários ou avaliações do local.

Os demais campos são os da [paginação comum](../conventions.md#paginação). Uma página vazia pode ser:

```json
{"content":[],"page":2,"size":10,"totalElements":0,"totalPages":0,"last":true}
```

## Erros

| HTTP | Mensagem literal | Condição |
| --- | --- | --- |
| `400` | `Invalid pagination parameters` | `page < 0` ou `size < 1`. |
| `403` | `You do not own this trip` | Viagem de outro proprietário. |
| `404` | `Trip not found` | Viagem inexistente. |

Também se aplicam `401` por autenticação, `403` por suspensão, `404` por perfil inexistente e `500`/`503` de infraestrutura. UUID ou inteiros não conversíveis geram `400` do framework; veja os [erros compartilhados](../conventions.md#erros-compartilhados). Falha de provedor externo de lugares não é erro específico dessa rota, pois ela não o consulta.

```json
{"status":403,"message":"You do not own this trip"}
```

## Efeitos e consistência

Executa em transação somente de leitura e não altera viagem, atividades ou catálogo. Não há cache implementado no serviço. O isolamento é o padrão do ambiente; não há garantia de fotografia única entre páginas nem garantia adicional de `REPEATABLE_READ` nesta consulta.

## Implementação e testes

- [TripController.getTripPlaces](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/controller/TripController.java) → [TripService.getPlacesTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/service/TripService.java).
- [PlaceRepository](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/repository/PlaceRepository.java): `findByDestinationIdAndRemovedAtIsNullOrderByNameAscIdAsc`.
- [TripPlacesResponse](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/TripPlacesResponse.java), [PlaceResponse](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/PlaceResponse.java) e [Place](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/models/Place.java).
- [TripPlacesApiTest](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripPlacesApiTest.java): campos dos cartões, nulos, ordenação, paginação, limite de 100, filtro por destino/remoção e autorização.
- [PlaceRepositoryPostgresIT](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/repository/PlaceRepositoryPostgresIT.java): persistência e consultas de lugares no PostgreSQL.
