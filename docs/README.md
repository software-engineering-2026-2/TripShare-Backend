# Índice da documentação

Use este índice para localizar a documentação relevante antes de planejar ou editar arquivos. A documentação das rotas de viagens está organizada por operação em [api/README.md](api/README.md).

## Documentos deste diretório

| Documento | Conteúdo | Quando consultar |
| --- | --- | --- |
| [Referência das rotas](api/README.md) | Índice das cinco operações de viagens, com uma página por método e caminho, exemplos, regras, erros e referências de implementação. | Antes de integrar, testar ou modificar uma rota de viagens. |
| [Convenções da API](api/conventions.md) | Autenticação, formatos, paginação, erros compartilhados, transações e preparação dos exemplos. | Ao interpretar o comportamento comum das rotas de viagens. |
| [OpenAPI de viagens](openapi.yaml) | Contrato estruturado de criação, edição, prévia de exclusão, exclusão e listagem de lugares do destino. | Ao integrar clientes, conferir schemas ou validar exemplos dessas operações. |
| [Especificação do projeto](especificacao-do-projeto.md) | Contexto do produto, público, escopo proposto, vocabulário, requisitos iniciais (`RF-001`–`RF-012`), critérios de aceitação propostos, regras e questões em aberto. | Antes de discutir escopo ou alterar comportamento do produto. É uma especificação inicial, não um registro de funcionalidades implementadas. |
| [Guia do banco](database.md) | PostgreSQL, comandos de operação, Flyway, migrações, transição Firebase, seeds, integridade, exclusão, privacidade, consultas, testes, diagrama e rastreabilidade de `RF-01`–`RF-32` do documento referenciado no guia. | Antes de alterar persistência, relacionamentos, consultas ou operações do banco. |
| [Dicionário de dados](database-dictionary.md) | Referência das tabelas das migrações V1–V7, com tipos, nulabilidade, valores padrão, constraints e índices. | Para conferir definições de armazenamento e mapeamentos. Mudanças posteriores, como a duração editável e datas opcionais da V8, devem ser consultadas nas migrações. |
| [Autenticação](auth.md) | Fluxo de login com Firebase e Spring Security, validação do token e consulta ao perfil no PostgreSQL. | Para entender a autenticação ou investigar erros de login. |

## Documentação relacionada

| Documento | Conteúdo | Quando consultar |
| --- | --- | --- |
| [README do repositório](../README.md) | Visão geral, pré-requisitos, execução local/Docker, credenciais, testes, scripts e contratos de autenticação e perfis. | Ao configurar o backend ou trabalhar com autenticação e perfis. |
| [Instruções para agentes](../AGENTS.md) | Convenções de trabalho, skills, idioma, edição e verificação. | Antes de trabalhar no repositório. |
| [Ajuda do Spring Boot](../trip.share/HELP.md) | Referências do Spring Initializr, Docker Compose e configuração Maven. | Ao consultar referências do framework e do projeto inicial. |
| [Migrações SQL](../trip.share/src/main/resources/db/migration) | Histórico executável e versionado do esquema. | Para confirmar o comportamento atual do banco, inclusive mudanças posteriores aos guias. |

## Como interpretar

- Comece pelo documento correspondente à tarefa e siga suas referências. Para persistência, leia o guia junto com o dicionário e as migrações aplicáveis.
- Preserve o vínculo entre os identificadores de requisitos e sua fonte. A especificação inicial usa `RF-001`–`RF-012`; o documento referenciado pelo guia do banco usa `RF-01`–`RF-32`. Números semelhantes não significam o mesmo requisito.
- Trate propostas, critérios de aceitação iniciais e questões abertas como pendentes de validação.
- Diferencie objetivo do produto, suporte do banco e comportamento da aplicação. Uma tabela ou constraint não comprova que existe uma API implementada.
- Registre divergências entre código e documentação. O catálogo de rotas descreve o comportamento atual, incluindo limitações conhecidas, sem transformar propostas em requisitos aprovados.

## Manutenção

Atualize este índice ao adicionar, renomear, remover ou mudar substancialmente o escopo de um documento. Mantenha os detalhes nos documentos próprios. Mudanças de comportamento de rotas devem atualizar o OpenAPI, a página da operação e os testes relevantes no mesmo PR.
