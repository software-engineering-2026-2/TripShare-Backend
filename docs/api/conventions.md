# Convenções de autenticação da API

## Firebase Session Cookie

`POST /auth/login` e `POST /auth/register` recebem o Firebase ID token recém-obtido pelo cliente em
`Authorization: Bearer <id-token>`. Após validar o token, a igualdade do UID com o campo `uuid` e o
perfil, o backend responde com o corpo existente e `Set-Cookie`.

O cookie padrão é `tripshare_session`, `HttpOnly`, `Path=/`, `SameSite=Lax`, validade de cinco dias e
`Secure=false` no ambiente local HTTP. O profile `prod` usa `Secure=true`. O frontend não pode ler o
cookie nem deve persistir o ID token no `SessionStorage`.

O restante das rotas protegidas autentica usando `FirebaseAuth.verifySessionCookie(cookie, checkRevoked)`.
Se o cookie estiver ausente, `APP_AUTH_BEARER_FALLBACK_ENABLED=true` permite temporariamente o Bearer.
Cookie presente mas inválido não aciona fallback. Desative a compatibilidade antiga com
`APP_AUTH_BEARER_FALLBACK_ENABLED=false` após a migração.

`SameSite=Lax` é o padrão. Para frontend e API em sites diferentes, configure `SameSite=None` junto
com `Secure=true` e HTTPS.

## Status e erros de autenticação

- `401`: token/cookie inválido, expirado ou revogado, ou UID incompatível no login/cadastro.
- `403`: usuário suspenso.
- `404`: perfil autenticado não encontrado.
- `409`: perfil duplicado.
- `503`: Firebase ou banco indisponível.

As respostas de erro mantêm o envelope legado `status` e `message`.
