# Documentation Index

Use this index to find the documentation relevant to your task before planning or editing files. Most source documents are written in Brazilian Portuguese; their existing filenames are preserved.

## Documents in this directory

| Document | Contents | When to read |
| --- | --- | --- |
| [Project specification](especificacao-do-projeto.md) | Product context and project specification. | Before discussing product scope or changing user-facing behavior. |
| [Database guide](database.md) | PostgreSQL setup and connection configuration, database lifecycle commands, Flyway migrations and recovery, the Firebase transition, reference and demo seeds, integrity rules, deletion and privacy behavior, database tests, an entity-relationship diagram, and a traceability matrix. | Before changing persistence, migrations, relationships, database constraints, queries, or database operations. |
| [Data dictionary](database-dictionary.md) | A table-by-table schema reference for PostgreSQL migrations V1–V7, including column types, nullability, defaults, generated expressions, constraints, and additional indexes. | When inspecting exact storage definitions or checking entity mappings and SQL changes. The dictionary identifies versioned SQL as the source of truth for the schema. |
| [Autenticação](auth.md) | Fluxo de login com Firebase e Spring Security, validação do token e consulta ao perfil no PostgreSQL. | Para entender como o backend autentica requisições ou investigar erros de login. |
| [OpenAPI](openapi.yaml) | Edição de viagem, prévia paginada e exclusão confirmada. | Ao integrar ou testar essas rotas. |

## Related repository documentation

| Document | Contents | When to read |
| --- | --- | --- |
| [Repository README](../README.md) | Project overview, prerequisites, local and Docker startup, Firebase credential configuration, build and test commands, database scripts, repository layout, and documented authentication and user-profile API contracts. | When setting up the backend, running checks, or working on authentication and profile endpoints. |
| [Agent instructions](../AGENTS.md) | Repository working conventions, skill usage, language preferences, editing guidelines, verification expectations, and delivery instructions. | Before performing repository tasks. |
| [Spring Boot help](../trip.share/HELP.md) | Spring Initializr reference links and guides, Docker Compose support notes, and Maven parent configuration notes. | When looking for framework references or generated project setup guidance. |

## Reading and interpreting the documentation

- Start with the document that matches the task, then follow its relevant references. Read the database guide together with the data dictionary when changing persistence.
- Distinguish product goals, database support, and application behavior. A table or constraint does not by itself mean that a corresponding API or user flow is implemented.
- If documents conflict with one another or with the code, report the discrepancy and resolve it before relying on the conflicting detail for a change.

## Maintaining this index

Update this index when documentation is added, renamed, removed, or substantially changes scope. Keep detailed requirements, commands, and schema definitions in their respective documents rather than duplicating them here.
