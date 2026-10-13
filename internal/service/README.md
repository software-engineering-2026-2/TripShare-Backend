# internal/service

Camada de regras de negócio.

Responsabilidades:

- Orquestrar o uso dos repositórios.
- Validar e aplicar as regras do domínio.
- Expor métodos consumidos pelos handlers.

Não conhece HTTP nem Gin; trabalha apenas com `model` e `repository`.
