# Migração do frontend para Firebase Session Cookie

## Fluxo

1. O frontend autentica no Firebase e obtém um ID token.
2. Envia o token somente em `POST /auth/login` (ou `/auth/register`) no header
   `Authorization: Bearer <id-token>`, junto ao UID exigido pelo corpo.
3. Faz a chamada com credenciais habilitadas. O backend responde com `Set-Cookie`; o navegador
   armazena o cookie HttpOnly automaticamente.
4. Descartar o ID token usado na chamada. Não guardar tokens no `SessionStorage`, `localStorage` ou
   em qualquer estado persistente acessível ao JavaScript.
5. Para as demais chamadas, não montar o header Authorization. Enviar cookies automaticamente:

```javascript
fetch("https://api.example.com/users/me", {
  method: "PATCH",
  credentials: "include",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ name: "Nome atualizado" })
});
```

Com Axios:

```javascript
axios.patch("https://api.example.com/users/me", { name: "Nome atualizado" }, {
  withCredentials: true
});
```

## Logout

Chame `POST /auth/logout` com `credentials: 'include'` ou `withCredentials: true`. O backend expira o
cookie e revoga os refresh tokens Firebase quando consegue validar a sessão.

## Sessão expirada

Trate `401 Unauthorized` como sessão inválida/expirada. Para renovar:

1. Peça ao Firebase um ID token atualizado, por meio da sessão Firebase ainda ativa.
2. Faça uma nova chamada a `POST /auth/login` com esse token e o UID.
3. Descarte o token novamente após a resposta; o navegador recebe um novo cookie.

Se o Firebase também não conseguir renovar a identidade, solicite autenticação interativa ao usuário.

## SameSite por ambiente

- **Desenvolvimento local HTTP:** `APP_AUTH_COOKIE_SECURE=false` e `SameSite=Lax`.
- **Produção no mesmo site:** HTTPS, profile `prod`, cookie `Secure=true` e `SameSite=Lax` (ou
  `Strict` se compatível com o fluxo).
- **Frontend e API em sites diferentes:** HTTPS em ambos, `SameSite=None` e `Secure=true`.

Os cookies são HttpOnly: `document.cookie` não deve exibi-los.

## Compatibilidade transitória

`APP_AUTH_BEARER_FALLBACK_ENABLED` fica `true` por padrão para a migração. Depois que o frontend estiver
usando cookies em todas as rotas, configure `false` para deixar de aceitar Bearer fora de login e
cadastro.
