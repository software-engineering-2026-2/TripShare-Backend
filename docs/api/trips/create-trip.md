# Criar viagem

`POST /trips` · `operationId: createTrip`

Cria uma viagem para o perfil autenticado e um dia vazio para cada dia de sua duração. Permite planejar com datas fixas ou somente com uma quantidade de dias.

[Índice](../README.md) · [Convenções e erros comuns](../conventions.md) · [OpenAPI: CreateTripRequest e Trip](../../openapi.yaml)

## Acesso e entrada

Exige `Authorization: Bearer <ID_TOKEN_DO_FIREBASE>`, perfil local ativo e `Content-Type: application/json`. Não recebe parâmetros de caminho ou query. O proprietário é obtido do token; não existe campo de entrada `userId`.

| Campo do body | Tipo | Obrigatório | Regra |
| --- | --- | --- | --- |
| `title` | String | Sim | Não vazio nem apenas espaços; salvo com `trim()`. Sem limite de tamanho explícito no DTO de criação. |
| `destinationId` | UUID | Sim | Destino já cadastrado. |
| `startsOn` | Data ou `null` | Condicional | Deve acompanhar `endsOn` se houver datas fixas. |
| `endsOn` | Data ou `null` | Condicional | Deve acompanhar `startsOn` e ser igual ou posterior a ela. |
| `durationDays` | Inteiro ou `null` | Condicional | Positivo quando não há datas; quando há datas, deve coincidir com o intervalo inclusivo, se informado. |

## Regras de negócio

1. Valida o perfil e impede criação por usuário suspenso.
2. Com as duas datas, calcula `durationDays = endsOn - startsOn + 1`. Datas iguais representam um dia. Não há restrição implementada que exija datas futuras.
3. Sem datas, exige `durationDays > 0` e mantém `startsOn` e `endsOn` nulos.
4. Uma única data, duração ausente sem datas ou duração incompatível com as datas é inválida. `null` equivale a ausência para esses campos.
5. Busca o destino existente e cria os registros da viagem e de seus dias na mesma transação.

Não há teto de duração definido no contrato. A operação cria uma linha por dia, portanto duração maior implica mais registros. Nenhum roteiro ou atividade é criado automaticamente.

## Exemplos de chamada

Prepare as [variáveis dos exemplos](../conventions.md#endereço-conteúdo-e-exemplos). Com datas fixas:

```bash
curl -i -X POST "$BASE_URL/trips" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  -H 'Content-Type: application/json' \
  --data "{\"title\":\"Férias no Rio\",\"destinationId\":\"$DESTINATION_ID\",\"startsOn\":\"2027-01-10\",\"endsOn\":\"2027-01-14\"}"
```

Somente com duração (esta chamada cria uma segunda viagem):

```bash
curl -i -X POST "$BASE_URL/trips" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  -H 'Content-Type: application/json' \
  --data "{\"title\":\"Cinco dias no Rio\",\"destinationId\":\"$DESTINATION_ID\",\"durationDays\":5}"
```

## Resposta de sucesso

`201 Created`, com o objeto `Trip`. O controller não define header `Location`.

```json
{
  "id": "40000000-0000-0000-0000-000000000001",
  "userId": "10000000-0000-0000-0000-000000000001",
  "destinationId": "20000000-0000-0000-0000-000000000001",
  "destinationName": "Rio de Janeiro",
  "destinationRegion": "Rio de Janeiro",
  "destinationCountryCode": "BR",
  "title": "Férias no Rio",
  "startsOn": "2027-01-10",
  "endsOn": "2027-01-14",
  "durationDays": 5,
  "createdAt": "2026-11-17T12:00:00Z",
  "updatedAt": "2026-11-17T12:00:00Z"
}
```

| Campo | Significado |
| --- | --- |
| `id` | UUID da viagem criada; use nas demais operações. |
| `userId` | UUID interno do perfil proprietário, não o UID Firebase. |
| `destinationId`, `destinationName` | Identificação e nome do destino cadastrado. |
| `destinationRegion` | Região do destino; pode ser `null`. |
| `destinationCountryCode` | Código do país com duas letras. |
| `title` | Título após remoção de espaços nas extremidades. |
| `startsOn`, `endsOn` | Datas da viagem; ambas `null` no planejamento apenas por duração. |
| `durationDays` | Total de dias, sempre positivo. |
| `createdAt`, `updatedAt` | Instantes de criação e última alteração. |

## Erros

| HTTP | Mensagem literal | Condição |
| --- | --- | --- |
| `400` | `Invalid request body` | Body ausente/inválido, título obrigatório em branco, destino nulo ou valores não desserializáveis. |
| `400` | `End date cannot be before start date` | `endsOn` anterior a `startsOn`. |
| `400` | `Trip must specify a valid date range or a positive duration in days` | Datas incompletas, falta de período/duração, duração não positiva sem datas ou incompatível com o intervalo. |
| `404` | `Destination not found` | UUID válido de destino inexistente. |

Também se aplicam `401` por autenticação, `403` por suspensão, `404` por perfil inexistente e os erros de infraestrutura `500`/`503` descritos nas [convenções](../conventions.md#erros-compartilhados).

Exemplo de período inválido:

```json
{"status":400,"message":"End date cannot be before start date"}
```

## Persistência e repetição

Grava `trips` e `trip_days`, numerados de `1` a `durationDays`. O primeiro dia corresponde a `startsOn` quando há datas. Falha na criação de um dia reverte a criação da viagem. O destino e o perfil não são criados nem alterados. Repetir a requisição cria outra viagem; não há deduplicação por título ou chave de idempotência.

## Implementação e testes

- [TripController.createTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/controller/TripController.java) → [TripService.createTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/service/TripService.java).
- [CreateTripRequest](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/CreateTripRequest.java), [TripResponse](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/TripResponse.java) e [Trip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/models/Trip.java).
- [TripApiTest](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripApiTest.java): métodos `createTrip_*` cobrem datas, duração, autenticação, validações, criação dos dias e rollback em falha.
- [Migração V8](../../../trip.share/src/main/resources/db/migration/V8__allow_trips_without_dates.sql): admite datas nulas e valida coerência da duração no PostgreSQL.
