# AGENTS.md

Instruções oficiais para qualquer agente de IA trabalhando no `TripShare-Backend`. Responda e escreva documentação em **português do Brasil**, preservando nomes de comandos, APIs, arquivos e identificadores técnicos.

## 0. Preflight (antes de começar qualquer tarefa)

Antes de planejar ou editar qualquer arquivo, verifique se sua branch está sincronizada com `origin/development`:
1. Rode `git fetch origin`.
2. Verifique `git rev-list --count HEAD..origin/development`.
3. Se o resultado for maior que `0`:
   - Se `git status --porcelain` estiver limpo, avise o usuário e rode `git merge origin/development`.
   - Se houver alterações locais não commitadas, **pare e avise o usuário** que `origin/development` avançou antes de continuar.

## 1. Fontes da Verdade (leia antes de codar)

| Arquivo | Quando ler |
| --- | --- |
| `docs/especificacao-do-projeto.md` | **Sempre** — contém os 15 Requisitos Funcionais do Plano B (`RF-01`–`RF-05`, `RF-08`–`RF-17`). |
| `docs/openapi.yaml` | **Sempre** ao criar, alterar ou revisar qualquer endpoint ou DTO. |
| `docs/auth.md` | Ao mexer em autenticação Firebase, Spring Security ou perfis. |
| `docs/database.md` | Ao mexer em persistência, migrations Flyway, entidades JPA ou consultas. |

## 2. Regras Inegociáveis

- **Contrato Único (`docs/openapi.yaml`):** TODA rota nova ou alterada deve corresponder exatamente a `docs/openapi.yaml`. Se uma mudança aprovada alterar caminho, método, payload, status HTTP ou erro, atualize `docs/openapi.yaml` no mesmo PR.
- **Escopo Fechado (Plano B):** Implemente APENAS o que pertence aos 15 RFs de `docs/especificacao-do-projeto.md`. **NUNCA** implemente funcionalidades de comunidade (publicação pública, feed social, curtidas, comentários, avaliações, votos, ranking, favoritos, seguir usuários, cópia de roteiros, notificações, denúncias ou moderação).
- **Simplicidade e Mudanças Cirúrgicas:** Escreva o mínimo de código necessário para resolver a tarefa. Não refatore código fora do escopo, não crie abstrações especulativas e preserve alterações já feitas pelo usuário.
- **Não assuma — pergunte:** Se houver ambiguidade entre código, contrato e requisitos, pare e reporte antes de decidir sozinho.

## 3. Fluxo de Git

- Crie sua branch a partir de `development` e abra PR direto para `development`.
- Use nomes claros em inglês para novos arquivos e diretórios.
- **NUNCA** execute `git push` sem autorização explícita do usuário.
- **NUNCA** edite migrations Flyway já aplicadas; crie sempre uma nova versão (`V<N>__descricao.sql`).

## 4. Skills Disponíveis (`.agents/skills/`)

Use apenas quando aplicável à tarefa (leia o `SKILL.md` antes):
- `flyway-migrations` — criação e validação de migrations SQL.
- `spring-data-jpa` — entidades, repositórios e consultas JPA.
- `rest-api-conventions` — design de endpoints, status HTTP e DTOs.
- `testing-pyramid` — testes unitários, de API e de integração.

## 5. Verificação Obrigatória

Antes de concluir qualquer alteração de código, rode a partir de `trip.share/`:
- Testes unitários e de API: `./mvnw test`
- Se alterou migrations, schema, constraints ou consultas SQL (requer Docker): `./mvnw verify -Pdatabase-it`

Se algum comando não puder ser executado, informe explicitamente a limitação. Na entrega, resuma as alterações e liste os arquivos modificados.
