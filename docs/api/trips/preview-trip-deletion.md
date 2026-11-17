# Consultar prévia de exclusão

`GET /trips/{id}/delete-preview` · `operationId: previewTripDeletion`

Lista os roteiros da viagem e alguns de seus vínculos para apresentar o impacto antes da exclusão. Inclui roteiros privados, públicos e ocultados por moderação que pertençam à viagem.

[Índice](../README.md) · [Convenções e erros comuns](../conventions.md) · [OpenAPI: DeletePreview](../../openapi.yaml)

## Acesso e entrada

Exige `Authorization: Bearer <ID_TOKEN_DO_FIREBASE>`, perfil ativo e propriedade da viagem. Não recebe body.

| Parâmetro | Local | Tipo | Regra |
| --- | --- | --- | --- |
| `id` | Caminho | UUID obrigatório | Viagem existente do usuário autenticado. |
| `page` | Query | Inteiro opcional | Começa em zero; padrão `0`; não pode ser negativo. |
| `size` | Query | Inteiro opcional | Padrão `20`; deve ser positivo; acima de `100` é ajustado para `100`. |

## Regras de consulta

- Ordenação fixa por título crescente e, em caso de empate, UUID crescente, conforme a ordenação do banco.
- A página contém roteiros, não atividades. `totalElements` conta todos os roteiros da viagem, inclusive os de outras páginas.
- `favoriteCount` conta vínculos em `itinerary_favorites`; não conta curtidas, votos ou notas.
- `published` indica `visibility = PUBLIC`, mesmo se o roteiro estiver ocultado por moderação.
- `inRanking` indica presença na view `public_itineraries`, que filtra conteúdo público não ocultado e autor ativo. Não é uma posição nem um indicador de estar entre os primeiros N resultados. Por isso um roteiro pode ter `published: true` e `inRanking: false`.
- Página além do total ou viagem sem roteiros retorna `200` com `content: []`, totais e `last: true`.

A prévia não é um inventário completo de todas as tabelas afetadas: não retorna contagens de imagens, comentários, atividades ou denúncias. Consulte os [efeitos da exclusão](delete-trip.md#efeitos-no-banco).

## Exemplo de chamada

Use as [variáveis dos exemplos](../conventions.md#endereço-conteúdo-e-exemplos):

```bash
curl -i "$BASE_URL/trips/$TRIP_ID/delete-preview?page=0&size=20" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN"
```

## Resposta de sucesso

`200 OK`:

```json
{
  "content": [
    {
      "id": "60000000-0000-0000-0000-000000000001",
      "title": "Roteiro de museus",
      "published": true,
      "favoriteCount": 2,
      "inRanking": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

| Campo de `content[]` | Tipo | Significado |
| --- | --- | --- |
| `id` | UUID | Identificador do roteiro afetado. |
| `title` | String | Título do roteiro. |
| `published` | Booleano | Visibilidade pública. |
| `favoriteCount` | Inteiro de 64 bits | Quantidade de favoritos vinculados ao roteiro. |
| `inRanking` | Booleano | Presença na view pública usada pelo ranking. |

Os campos externos são os da [paginação comum](../conventions.md#paginação). Exemplo de viagem sem roteiros:

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0,"last":true}
```

## Erros

| HTTP | Mensagem literal | Condição |
| --- | --- | --- |
| `400` | `Invalid pagination parameters` | `page < 0` ou `size < 1`. |
| `403` | `You do not own this trip` | Outro proprietário. |
| `404` | `Trip not found` | Viagem inexistente ou já excluída. |

Também se aplicam `401` por autenticação, `403` por suspensão, `404` por perfil inexistente e `500`/`503` de infraestrutura. UUID ou inteiros não conversíveis causam `400` do framework, sem garantia do mesmo envelope. Veja os [erros compartilhados](../conventions.md#erros-compartilhados).

```json
{"status":400,"message":"Invalid pagination parameters"}
```

## Efeitos e sequência de uso

A consulta não altera dados, não reserva a viagem para exclusão e não gera token de confirmação. Usa transação somente de leitura com isolamento `REPEATABLE_READ`, mantendo consistência entre a contagem e o conteúdo de uma chamada. Outras páginas e a futura exclusão são chamadas independentes; os dados podem mudar entre elas.

Para confirmar, chame [DELETE com `confirm=true`](delete-trip.md). O backend não exige que a prévia tenha sido consultada nem que todas as páginas tenham sido abertas. A exclusão alcança todos os roteiros atuais da viagem. Para cancelar, basta não executar o DELETE; não existe operação de cancelamento da prévia.

## Implementação e testes

- [TripController.previewDeletion](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/controller/TripController.java) → [TripService.previewDeletion](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/service/TripService.java).
- [TripRepository](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/repository/TripRepository.java): `countItineraries` e `findDeletionImpact`.
- [TripDeletePreview](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/TripDeletePreview.java) e [V4: public_itineraries](../../../trip.share/src/main/resources/db/migration/V4__create_integrity_rules_indexes_and_views.sql).
- [TripManagementPostgresIT](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripManagementPostgresIT.java): `previewPaginatesItinerariesAndCountsEachFavoriteOnce`, `previewValidatesBoundsAndIncludesTotalsOnEmptyPages` e `previewAndDeletionRequireAnActiveOwner`.
- [TripServiceTest](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripServiceTest.java): acesso de outro proprietário. Esta operação integra o escopo chamado RF-06 no contrato anterior.
