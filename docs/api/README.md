# Documentação da API

## Contratos

- [Convenções de autenticação e cookie](conventions.md)
- [Cadastro de perfil](register.md)
- [Login](login.md)
- [Logout](logout.md)
- [Rotas protegidas](protected-endpoints.md)
- [Migração do frontend para cookie](frontend-cookie-migration.md)
- [OpenAPI](../openapi.yaml)

O formato atual de erros é `{"status": <código>, "message": "<mensagem>"}`. Cadastro e login
mantêm seus corpos e status atuais; a sessão é adicionada pelo cabeçalho `Set-Cookie`.
