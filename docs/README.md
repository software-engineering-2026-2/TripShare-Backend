# Documentation Index

Use this index to find the documentation relevant to your task before planning or editing files. Most source documents are written in Brazilian Portuguese; their existing filenames are preserved.

## Documents in this directory

| Document | Contents | When to read |
| --- | --- | --- |
| [Project specification](especificacao-do-projeto.md) | Product context, problems addressed, target users, proposed scope, domain terminology, initial functional requirements (`RF-001`–`RF-012`), proposed acceptance criteria, business rules, quality objectives, and open questions. | Before discussing product scope or changing user-facing behavior. This is an initial specification, not a record of implemented features. |
| [Database guide](database.md) | PostgreSQL setup and connection configuration, database lifecycle commands, Flyway migrations and recovery, the Firebase transition, reference and demo seeds, integrity rules, deletion and privacy behavior, SQL query examples, database tests, an entity-relationship diagram, and a traceability matrix for `RF-01`–`RF-32` from the requirements document linked in the guide. | Before changing persistence, migrations, relationships, database constraints, queries, or database operations. |
| [Data dictionary](database-dictionary.md) | A table-by-table schema reference for PostgreSQL migrations V1–V7, including column types, nullability, defaults, generated expressions, constraints, and additional indexes. | When inspecting exact storage definitions or checking entity mappings and SQL changes. The dictionary identifies versioned SQL as the source of truth for the schema. |

## Related repository documentation

| Document | Contents | When to read |
| --- | --- | --- |
| [Repository README](../README.md) | Project overview, prerequisites, local and Docker startup, Firebase credential configuration, build and test commands, database scripts, repository layout, and documented authentication and user-profile API contracts. | When setting up the backend, running checks, or working on authentication and profile endpoints. |
| [Agent instructions](../AGENTS.md) | Repository working conventions, skill usage, language preferences, editing guidelines, verification expectations, and delivery instructions. | Before performing repository tasks. |
| [Spring Boot help](../trip.share/HELP.md) | Spring Initializr reference links and guides, Docker Compose support notes, and Maven parent configuration notes. | When looking for framework references or generated project setup guidance. |

## Reading and interpreting the documentation

- Start with the document that matches the task, then follow its relevant references. Read the database guide together with the data dictionary when changing persistence.
- Keep requirement identifiers tied to their source document. The initial specification's `RF-001`–`RF-012` and the database guide's `RF-01`–`RF-32` are separate numbering schemes; do not assume that matching numbers describe the same requirement.
- Treat proposed acceptance criteria and open questions in the initial specification as pending validation. Consult the technical documents for decisions already documented there.
- Distinguish product goals, database support, and application behavior. A table or constraint does not by itself mean that a corresponding API or user flow is implemented.
- If documents conflict with one another or with the code, report the discrepancy and resolve it before relying on the conflicting detail for a change.

## Maintaining this index

Update this index when documentation is added, renamed, removed, or substantially changes scope. Keep detailed requirements, commands, and schema definitions in their respective documents rather than duplicating them here.
