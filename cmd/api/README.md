# cmd/api

Ponto de entrada da aplicação (`main.go`).

Responsabilidades:

- Carregar configurações (variáveis de ambiente, porta, conexão com banco).
- Inicializar dependências (repositórios, serviços e handlers).
- Registrar as rotas no roteador do Gin.
- Subir o servidor HTTP.

O `main` apenas monta e inicia a aplicação; a lógica fica em `internal/`.
