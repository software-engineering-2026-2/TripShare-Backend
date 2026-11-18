# `POST /auth/logout`

Encerra a sessão no navegador e solicita ao Firebase a revogação dos refresh tokens do UID associado
ao cookie válido.

O cookie é opcional. Cookie ausente ou inválido também resulta em `204 No Content`. A resposta envia
um `Set-Cookie` com o mesmo nome, domínio, `Path=/`, `HttpOnly`, `Secure` e `SameSite`, mas com
`Max-Age=0`.

Envie a requisição com credenciais (`credentials: 'include'` no Fetch ou `withCredentials: true` no
Axios).

Consulte [`conventions.md`](conventions.md) para os atributos configuráveis do cookie.
