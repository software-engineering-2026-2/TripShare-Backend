# `POST /auth/login`

Localiza o perfil existente associado à conta Firebase.

**Autenticação:** Firebase ID token em `Authorization: Bearer <id-token>`. O campo `uuid` do corpo
deve ser exatamente igual ao UID do token verificado.

```json
{"uuid":"uid-retornado-pelo-firebase"}
```

**Sucesso:** `200 OK`, corpo `UserResponse` existente e cabeçalho `Set-Cookie` com Firebase Session
Cookie HttpOnly. O token não é retornado no corpo.

**Erros:** `400` corpo inválido; `401` token inválido ou UID divergente; `403` perfil suspenso;
`404` perfil ainda não cadastrado; `503` Firebase ou banco indisponível.

Login não cria perfil automaticamente. O cookie é configurado conforme
[`conventions.md`](conventions.md).
