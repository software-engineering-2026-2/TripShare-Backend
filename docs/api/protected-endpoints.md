# Rotas protegidas

Após login/cadastro, o navegador envia o Firebase Session Cookie automaticamente. As rotas não
exigem que o frontend envie ou leia manualmente um token:

| Método | Rota | Operação |
|---|---|---|
| `GET` | `/users/{id}` | Consulta pública de perfil (a API continua exigindo autenticação) |
| `PATCH` | `/users/me` | Atualiza o perfil autenticado |
| `POST` | `/trips` | Cria uma viagem |
| `PATCH` | `/trips/{id}` | Atualiza uma viagem |
| `GET` | `/trips/{id}/delete-preview` | Prévia paginada de exclusão |
| `DELETE` | `/trips/{id}?confirm=true` | Exclui viagem após confirmação |
| `GET` | `/trips/{id}/places` | Consulta lugares do destino da viagem |

Em chamadas de navegador, habilite envio de credenciais. Enquanto
`APP_AUTH_BEARER_FALLBACK_ENABLED=true`, um Bearer antigo ainda é aceito quando
não há cookie; cookie inválido não permite fallback.

Veja os contratos específicos de viagens no [OpenAPI](../openapi.yaml).
