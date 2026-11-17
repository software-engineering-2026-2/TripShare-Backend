# Excluir viagem

`DELETE /trips/{id}` · `operationId: deleteTrip`

Exclui fisicamente a viagem e seus dependentes no banco, mediante confirmação explícita do proprietário.

[Índice](../README.md) · [Convenções e erros comuns](../conventions.md) · [OpenAPI](../../openapi.yaml)

## Acesso e entrada

Exige `Authorization: Bearer <ID_TOKEN_DO_FIREBASE>`, perfil ativo e propriedade da viagem. Não recebe body.

| Parâmetro | Local | Tipo | Regra |
| --- | --- | --- | --- |
| `id` | Caminho | UUID obrigatório | Viagem existente do usuário autenticado. |
| `confirm` | Query | String obrigatória para excluir | Valor literal `true`, em minúsculas e sem espaços. |

O controller permite receber `confirm` ausente, mas o serviço rejeita a exclusão com `400`. `false`, `TRUE`, `1`, vazio ou qualquer outro valor não confirma. A propriedade da viagem é verificada antes da confirmação; por exemplo, outro proprietário recebe `403` mesmo sem `confirm`.

## Regras de negócio

A [prévia](preview-trip-deletion.md) pode ser usada para mostrar o impacto, mas não é um pré-requisito validado pelo backend. Não há token, prazo de validade ou vínculo entre uma prévia e a exclusão. A operação usa o estado atual da viagem e remove todos os seus roteiros, inclusive os que não apareceram na página consultada.

A exclusão não possui desfazer ou restauração por API. Repetir a chamada depois de uma exclusão concluída retorna `404`, sem novas exclusões.

## Exemplo de chamada

Use as [variáveis dos exemplos](../conventions.md#endereço-conteúdo-e-exemplos). Esta chamada efetivamente exclui a viagem indicada:

```bash
curl -i -X DELETE "$BASE_URL/trips/$TRIP_ID?confirm=true" \
  -H "Authorization: Bearer $FIREBASE_ID_TOKEN"
```

## Resposta de sucesso

`204 No Content`, sem JSON ou outro corpo de resposta:

```http
HTTP/1.1 204 No Content
```

## Erros

| HTTP | Mensagem literal | Condição |
| --- | --- | --- |
| `400` | `Trip deletion requires explicit confirmation` | `confirm` ausente ou diferente de `true`. |
| `403` | `You do not own this trip` | Outro proprietário. |
| `404` | `Trip not found` | Viagem inexistente ou já excluída. |
| `500` | `Database operation failed` | Violação de integridade impede a exclusão, inclusive no commit. |

Também se aplicam `401` por autenticação, `403` por suspensão, `404` por perfil inexistente e `503` por indisponibilidade. UUID inválido no caminho gera `400` do framework. Veja os [erros compartilhados](../conventions.md#erros-compartilhados).

Exemplo sem confirmação:

```json
{"status":400,"message":"Trip deletion requires explicit confirmation"}
```

## Efeitos no banco

A remoção de `trips` aciona as cascatas das chaves estrangeiras:

| Dados | Efeito |
| --- | --- |
| `trip_interests`, `trip_days`, `itineraries` da viagem | Excluídos. |
| Atividades e trechos dos roteiros | Excluídos. |
| Curtidas, votos, favoritos, avaliações, comentários e registros de imagens desses roteiros | Excluídos, inclusive interações feitas por outros usuários. |
| Notificações e denúncias relacionadas aos roteiros ou comentários removidos | Excluídas conforme os vínculos. |
| Views públicas e ranking | Deixam de apresentar os roteiros removidos. |
| Cópias dos roteiros em outras viagens | Preservadas; `source_itinerary_id` vira `NULL`, mantendo nome do autor e título de origem como atribuição histórica. |
| Ações de moderação | Preservadas; a referência a uma denúncia removida vira `NULL`. |
| Perfil, destino, catálogo de lugares e viagens independentes | Preservados. |

Tudo ocorre em uma transação. Se a exclusão ou seu commit falhar, os registros removidos pela cascata são restaurados pelo rollback.

Excluir registros de imagens no banco não remove arquivos de armazenamento externo. O serviço atual não executa limpeza de object storage nem envia mensagens ou notificações externas ao excluir a viagem.

## Implementação e testes

- [TripController.deleteTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/controller/TripController.java) → [TripService.deleteTrip](../../../trip.share/src/main/java/ufcg/software/engineering/trip/share/service/TripService.java).
- [V2: planejamento](../../../trip.share/src/main/resources/db/migration/V2__create_travel_planning.sql), [V3: interações e moderação](../../../trip.share/src/main/resources/db/migration/V3__create_social_and_moderation.sql) e [V6: FK adiável de atividades/dias](../../../trip.share/src/main/resources/db/migration/V6__defer_activity_day_foreign_key.sql).
- [TripServiceTest](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripServiceTest.java): confirmação explícita e autorização.
- [TripManagementPostgresIT](../../../trip.share/src/test/java/ufcg/software/engineering/trip/share/trip/TripManagementPostgresIT.java): `confirmationAfterOnlyFirstPageDeletesEntireTripAndPreservesOtherTrip`, `failedCommitRestoresAllCascadedRows` e `previewAndDeletionRequireAnActiveOwner`.
- [Guia do banco: exclusão e privacidade](../../database.md#exclusão-e-privacidade). Esta operação integra o escopo chamado RF-06 no contrato anterior.
