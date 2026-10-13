# internal/handler

Camada de apresentação HTTP, usando o framework Gin.

Responsabilidades:

- Definir os handlers (controllers) das rotas.
- Fazer binding/validação do JSON de entrada e montar as respostas.
- Traduzir erros das camadas internas para status HTTP.
- Delegar a lógica de negócio para `service`.
