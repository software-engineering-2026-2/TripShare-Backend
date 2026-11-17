## Descrição

<!-- Explique o problema, o que mudou e o resultado esperado. -->

## Ref / Issue

<!-- Informe a REF e/ou issue relacionada. -->

## Como validar

<!-- Descreva os pré-requisitos, os passos para reproduzir e o resultado esperado.
Para alterações na API, inclua exemplos de requisição e resposta sem dados sensíveis. -->

1. <!-- Ação e resultado esperado. -->
2. <!-- Ação e resultado esperado. -->

## Checklist de validação

<!-- Marque apenas o que foi verificado. Para itens que não se aplicam,
registre a justificativa na seção de evidências e deixe a caixa desmarcada. -->

- [ ] A alteração atende aos critérios de aceitação da US/issue relacionada.
- [ ] O projeto compila e os testes passam com `cd trip.share && ./mvnw verify` (Windows: `cd trip.share && mvnw.cmd verify`).
- [ ] Os testes cobrem o comportamento alterado, incluindo cenários de sucesso e erro.
- [ ] As entradas da API são validadas e as respostas usam os códigos HTTP esperados.
- [ ] As regras de autenticação e autorização foram verificadas nos fluxos alterados.
- [ ] Alterações em contratos da API e possíveis quebras de compatibilidade estão documentadas.
- [ ] Alterações no banco de dados foram validadas, incluindo o impacto nos dados existentes.
- [ ] Não foram incluídos segredos, credenciais ou dados pessoais no código, nos logs ou nas evidências.
- [ ] A documentação e as instruções de configuração foram atualizadas quando necessário.

## Impactos e cuidados na entrega

<!-- Informe mudanças de configuração, variáveis de ambiente, dependências,
passos de implantação e como reverter a alteração, quando aplicável. -->
