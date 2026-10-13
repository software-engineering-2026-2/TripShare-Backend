# internal/repository

Camada de acesso a dados (persistência).

Responsabilidades:

- Definir interfaces de repositório usadas pelos serviços.
- Implementar consultas e escritas no banco de dados.
- Isolar detalhes de banco (SQL, ORM) do restante da aplicação.

Retorna e recebe `model`, sem conter regras de negócio nem código HTTP.
