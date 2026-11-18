# `POST /auth/register`

Cria o perfil local após autenticação da conta no Firebase.

**Autenticação:** Firebase ID token em `Authorization: Bearer <id-token>`. O campo `uuid` do corpo
deve ser exatamente igual ao UID do token verificado.

```json
{
  "uuid": "uid-retornado-pelo-firebase",
  "username": "viajante",
  "name": "Nome do usuário"
}
```

**Sucesso:** `201 Created`, corpo `UserResponse` existente e cabeçalho `Set-Cookie` com Firebase
Session Cookie HttpOnly. O token não é retornado no corpo.

**Erros:** `400` corpo inválido; `401` token inválido ou UID divergente; `403` conta suspensa;
`409` UID, e-mail ou username duplicado; `503` Firebase ou banco indisponível.

O cookie é configurado conforme [`conventions.md`](conventions.md).
