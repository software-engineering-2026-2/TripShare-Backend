# Convenções das rotas de viagens

[Índice das rotas](README.md) · [Contrato OpenAPI](../openapi.yaml)

## Endereço, conteúdo e exemplos

A URL local padrão é `http://localhost:8080`, sem prefixo `/api` ou versão no caminho. Use a URL configurada no ambiente quando ela for diferente. Em ambientes publicados, envie credenciais por HTTPS.

`POST` e `PATCH` recebem `Content-Type: application/json`. As respostas documentadas com corpo usam JSON. A exclusão bem-sucedida retorna `204`, sem corpo. Datas civis usam `YYYY-MM-DD`; instantes usam ISO 8601, como `2026-11-17T12:00:00Z`. Identificadores internos são UUIDs; o UID do Firebase é uma string distinta do UUID do perfil.

Para executar os exemplos das páginas, prepare estas variáveis no terminal, substituindo os valores ilustrativos:

```bash
export BASE_URL='http://localhost:8080'
export FIREBASE_ID_TOKEN='SUBSTITUA_PELO_ID_TOKEN'
export DESTINATION_ID='20000000-0000-0000-0000-000000000001'
export TRIP_ID='40000000-0000-0000-0000-000000000001'
```

`DESTINATION_ID` deve existir em `destinations`. Para operações sobre uma viagem existente, `TRIP_ID` deve pertencer ao perfil autenticado. Após criar uma viagem, use o `id` retornado. Os exemplos de resposta são ilustrativos, não registros garantidos de um seed.

## Autenticação e autorização

Todas as cinco operações exigem `Authorization: Bearer <ID_TOKEN_DO_FIREBASE>`. O backend valida o token em cada requisição, sem sessão HTTP. O prefixo Bearer é aceito sem distinção entre maiúsculas e minúsculas. O backend não emite outro token nessas rotas.

O UID validado precisa estar associado a um perfil no PostgreSQL. Perfil inexistente retorna `404`; perfil suspenso retorna `403`. Na criação, esse perfil se torna proprietário. Nas outras operações, o serviço verifica o perfil, sua situação, a existência da viagem e sua propriedade, nessa ordem. Não há exceção para administradores acessarem viagens alheias nessas operações.

O filtro de autenticação executa antes do controller. Portanto, uma chamada sem token pode retornar `401` mesmo que também tenha corpo ou parâmetros inválidos. Após o filtro, conversão de parâmetros e validação do body podem falhar antes das regras de serviço.

Veja o [fluxo de autenticação](../auth.md) para os detalhes do Firebase.

## Valores ausentes, nulos e vazios

Na criação, `title` e `destinationId` não podem estar ausentes ou nulos. Datas e duração seguem as combinações descritas em [criar viagem](trips/create-trip.md).

No PATCH, os campos ausentes ou `null` não substituem os valores existentes. Não é possível apagar datas com `null`. Um objeto `{}` é aceito. `resolutions` ausente, `null` ou `[]` significa que nenhuma resolução foi enviada. Um item `null` dentro dessa lista é inválido.

Strings vazias não equivalem a omissão: título vazio ou apenas com espaços é rejeitado. O título é salvo com `trim()`. A criação não declara limite de tamanho para o título; a edição valida até 120 caracteres antes de aplicar `trim()`.

## Paginação

Aplicável à prévia de exclusão e à listagem de lugares:

| Parâmetro | Tipo | Padrão | Regra |
| --- | --- | --- | --- |
| `page` | Inteiro de 32 bits | `0` | Índice da página, começando em zero; deve ser maior ou igual a zero. |
| `size` | Inteiro de 32 bits | `20` | Deve ser positivo; valores acima de `100` são aceitos e reduzidos para `100`. |

Os parâmetros são opcionais; valores vazios usam o padrão do controller. Valores não conversíveis para inteiro causam `400` pelo framework. O limite de 100 é aplicado ao tamanho efetivo, inclusive ao cálculo do deslocamento (`page × size efetivo`).

| Campo de resposta | Significado |
| --- | --- |
| `content` | Itens da página solicitada. |
| `page` | Índice solicitado. |
| `size` | Tamanho efetivo, já limitado a 100. |
| `totalElements` | Total de itens que atendem ao filtro, não apenas os da página. |
| `totalPages` | Quantidade de páginas; zero quando não há itens. |
| `last` | Indica que não há uma página posterior com resultados. |

Uma página além dos resultados retorna `200`, `content: []` e os totais, com `last: true`. Não há parâmetro de ordenação configurável. Cada rota descreve sua ordenação fixa. Chamadas de páginas diferentes não compartilham uma fotografia imutável dos dados.

## Erros compartilhados

Os erros tratados por `ApiExceptionHandler` e pela segurança usam o formato abaixo. Não existe campo estável `code` nesse contrato; `message` contém a mensagem literal atual em inglês.

```json
{
  "status": 401,
  "message": "Firebase Bearer token is required"
}
```

| HTTP | Mensagem literal | Condição |
| --- | --- | --- |
| `400` | `Invalid request body` | Body ausente quando obrigatório, JSON ilegível ou violação das validações do DTO. |
| `401` | `Firebase Bearer token is required` | Header ausente, esquema incorreto ou token vazio. |
| `401` | `Invalid Firebase token` | Token inválido ou expirado. |
| `403` | `User is suspended` | Perfil suspenso. |
| `403` | `You do not own this trip` | Viagem de outro proprietário; não se aplica à criação. |
| `403` | `Access denied` | Recusa pela camada Spring Security. |
| `404` | `User profile not found` | UID autenticado sem perfil local. |
| `404` | `Trip not found` | Viagem inexistente; não se aplica à criação. |
| `500` | `Database operation failed` | Violação de integridade capturada sem um tratamento de negócio específico. |
| `503` | `Database operation unavailable` | Falha de acesso ao banco capturada pelo handler. |
| `503` | `Firebase authentication unavailable` | Não foi possível validar o token por indisponibilidade da autenticação. |

As respostas `401` incluem `WWW-Authenticate: Bearer`. As páginas das operações acrescentam os erros específicos.

**Limite do formato:** UUID inválido no caminho e parâmetros não conversíveis geram `400`, mas não têm tratamento dedicado nesse handler. O corpo dessas respostas depende do Spring e não deve ser assumido como `{status, message}`. Outros erros gerados pelo framework, como método ou tipo de conteúdo incompatível, também não são padronizados pelo projeto. O OpenAPI deixa o corpo do `400` aberto para representar essa diferença. Não há mapeamento próprio de conflito de bloqueio otimista para `409`.

## Transações e repetição de chamadas

Criação, edição e exclusão executam em transação: alterações no banco são confirmadas juntas ou revertidas em caso de falha. A prévia usa leitura com isolamento `REPEATABLE_READ` dentro de uma única chamada; a listagem de lugares usa transação somente de leitura, com o isolamento padrão.

Não há chave de idempotência, `ETag` ou `If-Match` implementado nesses controllers. Repetir um POST cria outra viagem. Repetir um DELETE após a exclusão retorna `404`. Repetir um PATCH com resoluções de redução já aplicadas pode retornar `400`, pois a viagem já terá a nova duração. Se uma resposta se perder, não presuma que a escrita falhou.

## Fontes da implementação

- [TripController](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/controller/TripController.java) e [TripService](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/service/TripService.java).
- [SecurityConfig](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/config/SecurityConfig.java) e [FirebaseAuthenticationFilter](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/handlers/FirebaseAuthenticationFilter.java).
- [ApiExceptionHandler](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/exceptions/ApiExceptionHandler.java), [ErrorResponse](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/dto/ErrorResponse.java) e [SecurityErrors](../../trip.share/src/main/java/ufcg/software/engineering/trip/share/handlers/SecurityErrors.java).
