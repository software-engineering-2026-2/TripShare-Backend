# internal/model

Camada de domínio: entidades e structs centrais do sistema (ex.: `Trip`, `User`).

Responsabilidades:

- Definir os tipos de dados e suas relações.
- Servir de contrato entre as demais camadas.
- Não depender de frameworks (Gin, banco de dados, etc.).

Também pode conter DTOs de entrada/saída quando necessário.
