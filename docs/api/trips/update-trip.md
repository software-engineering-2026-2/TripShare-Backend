# Editar viagem

`PATCH /trips/{id}` · `operationId: updateTrip`

Atualiza título, destino, datas ou duração da viagem do proprietário. Ao encurtar a viagem, permite mover ou remover atividades dos dias excedentes na mesma transação.

[Índice](../README.md) · [Convenções e erros comuns](../conventions.md) · [OpenAPI: UpdateTripRequest, DayResolution e Trip](../../openapi.yaml)

## Acesso e entrada

Exige ID token Firebase, perfil ativo, propriedade da viagem e `Content-Type: application/json`. `id` é o UUID obrigatório no caminho. Não há parâmetros de query. O body é obrigatório, mas `{}` é válido; campos ausentes ou `null` preservam os valores atuais.

| Campo do body | Tipo | Regra quando não nulo |
| --- | --- | --- |
| `title` | String | Até 120 caracteres antes de `trim()`, validada pelo padrão Java `.*\S.*`; não aceita título em branco. |
| `destinationId` | UUID | Deve existir no catálogo de destinos. |
| `startsOn` | Data | Substitui a data inicial; pode ser combinada com o término já existente. |
| `endsOn` | Data | Substitui a data final; pode ser combinada com o início já existente. |
| `durationDays` | Inteiro | Positivo; se enviado com datas, deve corresponder ao intervalo inclusivo resultante. |
| `resolutions` | Lista de objetos | Usada somente ao reduzir a duração, com uma resolução para cada dia conflitante. |

Cada objeto de `resolutions` tem:

| Campo | Tipo | Regra |
| --- | --- | --- |
| `dayNumber` | Inteiro obrigatório | Dia conflitante, numerado a partir de 1; não pode repetir na lista. |
| `action` | String obrigatória | `MOVE` ou `REMOVE`, respeitando maiúsculas. |
| `targetDay` | Inteiro ou `null` | Obrigatório e entre `1` e a nova duração para `MOVE`; ausente ou `null` para `REMOVE`. |

## Regras de período

| Situação | Resultado |
| --- | --- |
| Nenhuma data ou duração enviada | Preserva o período. |
| Viagem com datas; apenas uma data enviada | Combina a nova data com a outra já existente e recalcula a duração. |
| Viagem sem datas; tentativa de definir apenas uma data | `400`; é necessário enviar ambas. |
| Uma ou ambas as datas enviadas, com duração | A duração deve coincidir com o intervalo inclusivo resultante. |
| Viagem com datas; somente duração enviada | Preserva `startsOn` e define `endsOn = startsOn + durationDays - 1`. |
| Viagem sem datas; somente duração enviada | Atualiza a duração e conserva ambas as datas nulas. |
| Datas enviadas como `null` | Não remove as datas existentes. |

O término não pode preceder o início. Ao deslocar datas, as atividades permanecem associadas aos números dos dias; o serviço não as remapeia por data civil. Ao aumentar a duração, cria dias vazios adicionais. Ao reduzir, remove os dias excedentes depois de resolver suas atividades.

Trocar o destino altera a referência da viagem. Não migra nem reescreve os locais das atividades já salvas. A [listagem de lugares](list-trip-places.md) passa a consultar o catálogo do novo destino.

## Redução e conflitos

1. Envie o PATCH com o novo período. Se houver atividades em dias acima da nova duração e nenhuma resolução, recebe `409` com `conflicts`, ordenado por `dayNumber`.
2. Cada conflito informa a quantidade de atividades daquele dia, somando os roteiros da viagem. Atividades sem dia não entram nessa contagem.
3. Reenvie o novo período junto com exatamente uma resolução por dia conflitante. Não inclua dias vazios, repetidos ou que não estejam em conflito; não omita conflitos.
4. `MOVE` transfere todas as atividades do dia de origem para um dia mantido. As atividades continuam nos respectivos roteiros e são posicionadas depois das existentes no destino. A ordem das resoluções influencia a ordem dos lotes movidos para o mesmo dia.
5. `REMOVE` exclui todas as atividades do dia. Se isso deixar um roteiro público sem nenhuma atividade com localização identificada, recebe `409` e toda a alteração é revertida.

Uma lista não vazia de resoluções sem redução da duração retorna `400`. `resolutions: []` equivale a nenhuma resolução. Não há exigência de uma consulta prévia: pode enviar diretamente todas as resoluções válidas, que serão verificadas contra o estado atual.

## Exemplos de chamada

Use as [variáveis dos exemplos](../conventions.md#endereço-conteúdo-e-exemplos). Alteração simples:

```bash
curl -i -X PATCH "$BASE_URL/trips/$TRIP_ID" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"title":"Férias no Rio atualizadas"}'
```

Redução para três dias, supondo uma viagem maior:

```bash
curl -i -X PATCH "$BASE_URL/trips/$TRIP_ID" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"durationDays":3}'
```

Exemplo de resposta `409`:

```json
{
  "status": 409,
  "message": "Cannot shorten trip: activities exist on days outside the new period",
  "conflicts": [
    {"dayNumber": 4, "activityCount": 2},
    {"dayNumber": 5, "activityCount": 1}
  ]
}
```

Para esse conjunto específico de conflitos, mover o dia 4 e remover o dia 5:

```bash
curl -i -X PATCH "$BASE_URL/trips/$TRIP_ID" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"durationDays":3,"resolutions":[{"dayNumber":4,"action":"MOVE","targetDay":2},{"dayNumber":5,"action":"REMOVE"}]}'
```

## Resposta de sucesso

`200 OK`, com todos os campos de `Trip`, não apenas os alterados. O significado dos campos é o mesmo da [resposta de criação](create-trip.md#resposta-de-sucesso).

```json
{
  "id": "40000000-0000-0000-0000-000000000001",
  "userId": "10000000-0000-0000-0000-000000000001",
  "destinationId": "20000000-0000-0000-0000-000000000001",
  "destinationName": "Rio de Janeiro",
  "destinationRegion": "Rio de Janeiro",
  "destinationCountryCode": "BR",
  "title": "Férias no Rio atualizadas",
  "startsOn": "2027-01-10",
  "endsOn": "2027-01-12",
  "durationDays": 3,
  "createdAt": "2026-11-17T12:00:00Z",
  "updatedAt": "2026-11-17T12:10:00Z"
}
```

## Erros

| HTTP | Mensagem literal | Condição |
| --- | --- | --- |
| `400` | `Invalid request body` | Falha de JSON/DTO, título inválido, duração não positiva, ação inválida, dia menor que 1 ou item nulo em `resolutions`. |
| `400` | `End date cannot be before start date` | Término anterior ao início **ou** duração enviada com datas que não corresponde ao intervalo; a implementação reutiliza essa mensagem. |
| `400` | `Trip must specify a valid date range or a positive duration in days` | Viagem sem datas recebe apenas uma data; também usado se um dia necessário à resolução não for encontrado. |
| `400` | `Invalid day resolutions` | Lista incompleta, dia repetido/não conflitante, destino de movimento ausente/fora do período, `targetDay` não nulo em `REMOVE` ou resoluções sem redução. |
| `404` | `Destination not found` | Novo destino inexistente. |
| `409` | `Cannot shorten trip: activities exist on days outside the new period` | Faltam resoluções para a redução; resposta inclui `conflicts`. |
| `409` | `A public itinerary needs an activity with an identified location` | Remoção viola a exigência de atividade localizada em roteiro público; resposta não inclui `conflicts`. |

Também se aplicam os erros comuns `401`, `403`, `404` para perfil/viagem, `500` e `503`. UUID inválido no caminho gera `400` do framework; veja as [convenções](../conventions.md#erros-compartilhados).

## Persistência e concorrência

Atualiza `trips`, cria/remove `trip_days` e pode mover/excluir `itinerary_activities`. Triggers invalidam os trechos derivados afetados (`itinerary_legs`); esta operação não os recalcula. Todas as alterações são atômicas, inclusive as resoluções.

`Trip` usa `@Version` internamente, mas a API não expõe versão ou precondição HTTP. Não há resposta `409` específica documentada para concorrência otimista; as duas respostas de conflito acima são de negócio. Repetir um PATCH com resoluções já aplicadas pode resultar em `400`.

## Implementação e testes

- [TripController.updateTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/controller/TripController.java) → [TripService.updateTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/service/TripService.java), incluindo `resolvePeriod`, `validateResolutions` e `applyResolutions`.
- [UpdateTripRequest](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/UpdateTripRequest.java), [DayResolution](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/DayResolution.java) e [TripDayRepository](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/repository/TripDayRepository.java).
- [TripApiTest](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripApiTest.java): métodos `patchTrip_*` para campos, período, permissão, expansão e conflitos.
- [TripServiceTest](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripServiceTest.java): resolução duplicada rejeitada antes de mover atividades.
- [TripManagementPostgresIT](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripManagementPostgresIT.java): movimento/remoção atômicos e rollback quando falta atividade localizada pública.
- [V4: triggers](../../../trip.share/src/main/resources/db/migration/V4__create_integrity_rules_indexes_and_views.sql) e [V8: duração](../../../trip.share/src/main/resources/db/migration/V8__allow_trips_without_dates.sql). A edição corresponde ao escopo chamado RF-06 no contrato anterior.
