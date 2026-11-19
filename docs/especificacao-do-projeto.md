# Requisitos Funcionais — TripShare (Plano B: Planejamento, Carrinho e Roteiro)

> **Escopo definido (Plano B — 15 Requisitos Funcionais):** Foco completo na experiência de planejamento privado de viagens (Autenticação, Perfil, Viagens, Feed Explorar, Carrinho de Atrações, Itens Não Alocados, Restrições de Horário, Roteiro Inteligente e Manual, Timeline por Turnos e Mapa).
>
> Os códigos originais (`RF-01` a `RF-05` e `RF-08` a `RF-17`) foram mantidos para preservar a rastreabilidade com as branches, commits, PRs e testes já criados nos repositórios `TripShare-Backend` e `TripShare-Frontend`.

## Quadro Resumo dos Requisitos Funcionais (Plano B)

| Código | Nome | Prioridade |
| :--- | :--- | :--- |
| **RF-01** | Cadastrar usuário | Essencial |
| **RF-02** | Autenticar usuário e encerrar sessão | Essencial |
| **RF-03** | Visualizar e editar perfil do usuário | Importante |
| **RF-04** | Cadastrar e consultar viagens | Essencial |
| **RF-05** | Editar ou excluir viagem | Essencial |
| **RF-08** | Consultar detalhes de locais e atividades | Essencial |
| **RF-09** | Visualizar Feed Explorar | Essencial |
| **RF-10** | Adicionar atração ao carrinho | Essencial |
| **RF-11** | Gerenciar itens não alocados | Essencial |
| **RF-12** | Definir restrições de horário | Importante |
| **RF-13** | Gerar roteiro inteligente (viagem ou dia) | Essencial |
| **RF-14** | Gerar roteiro manualmente | Essencial |
| **RF-15** | Gerenciar e salvar atividades do roteiro | Essencial |
| **RF-16** | Visualizar roteiro em timeline e alternar modos de visualização | Essencial |
| **RF-17** | Visualizar roteiro no mapa | Essencial |

---

## RF-01 — Cadastrar usuário

- **Descrição:** O sistema deve permitir o cadastro de viajantes por dois métodos, ambos gerenciados pelo Firebase Authentication: conta Google ou e-mail e senha. Em ambos, são solicitados apenas os dados mínimos: nome de exibição e nome de usuário (único). O e-mail identifica uma única conta, independentemente do método utilizado.
- **Atores:** Visitante.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/SEG-02, RNF/SEG-04, RNF/PER-06, RNF/LEG-01, RNF/LEG-04, RNF/LEG-06, RNF/LEG-07, RNF/LEG-12, RNF/LEG-15
- **Entradas e pré-condições:** Formulário de cadastro acessível; usuário ainda não autenticado.
- **Saídas e pós-condições:** Conta criada, vinculada ao identificador do Firebase e disponível para autenticação; aceite dos termos registrado com data e versão; a senha, quando houver, é armazenada apenas pelo Firebase e nunca exibida.

### Fluxos de eventos

**Fluxo principal:**
1. O visitante acessa a opção de cadastro e escolhe "Continuar com Google" ou "Cadastrar com e-mail e senha".
2. No Google, o visitante se autentica no provedor; no e-mail e senha, informa e-mail, senha e confirmação de senha, e o Firebase cria a credencial.
3. O sistema solicita nome de exibição, nome de usuário e o aceite dos Termos de Uso e da Política de Privacidade.
4. O sistema valida os dados, registra a conta e apresenta a confirmação do cadastro.

**Fluxo alternativo 1:**
No passo 2 ou 4, dados inválidos (e-mail mal formatado, senha abaixo dos requisitos mínimos, confirmação diferente da senha, campo vazio ou acima do limite): informar os campos a corrigir, sem criar a conta.

**Fluxo alternativo 2:**
No passo 2, e-mail já cadastrado por qualquer um dos métodos: informar que a conta já existe e orientar o acesso pelo método utilizado originalmente (RF-02), sem criar conta duplicada.

**Fluxo alternativo 3:**
No passo 4, nome de usuário já em uso ou termos não aceitos: solicitar outro nome de usuário ou o aceite, sem criar a conta.

---

## RF-02 — Autenticar usuário e encerrar sessão

- **Descrição:** O sistema deve permitir entrar com conta Google ou com e-mail e senha, ambos por meio do Firebase Authentication, recuperar a senha de contas criadas com e-mail e encerrar a sessão atual. As funcionalidades de criação e interação devem exigir uma sessão válida.
- **Atores:** Visitante e usuário autenticado.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-02, RNF/SEG-04, RNF/PER-06, RNF/LEG-01, RNF/LEG-07, RNF/LEG-10, RNF/LEG-15
- **Entradas e pré-condições:** Conta cadastrada para entrar (RF-01); sessão ativa para sair.
- **Saídas e pós-condições:** Sessão iniciada após autenticação ou invalidada após o encerramento; e-mail de redefinição de senha enviado quando solicitado.

### Fluxos de eventos

**Fluxo principal:**
1. O visitante escolhe "Entrar com Google" ou informa e-mail e senha.
2. O Firebase autentica as credenciais e o sistema verifica o cadastro da conta.
3. O sistema inicia a sessão e apresenta a área do usuário.
4. Ao selecionar Sair, o usuário tem a sessão atual invalidada.

**Fluxo alternativo 1:**
No passo 2, e-mail ou senha incorretos: exibir mensagem genérica de falha de acesso, sem indicar qual dado está incorreto e sem iniciar sessão.

**Fluxo alternativo 2:**
No passo 1 ou 2, login com Google cancelado ou falha de comunicação: não iniciar sessão e permitir nova tentativa. Para ações protegidas, sessão expirada exige nova autenticação.

**Fluxo alternativo 3:**
No passo 2, usuário autenticado no Firebase sem cadastro concluído no sistema: redirecionar para a conclusão do cadastro (RF-01, passo 3).

**Fluxo alternativo 4:**
No passo 1, ao selecionar "Esqueci minha senha": o sistema solicita o e-mail e o Firebase envia o link de redefinição, exibindo a mesma mensagem de confirmação esteja o e-mail cadastrado ou não. Contas criadas apenas com Google redefinem o acesso pelo próprio Google.

---

## RF-03 — Visualizar e editar perfil do usuário

- **Descrição:** O sistema deve permitir visualizar os dados do perfil e alterar nome de exibição, biografia e cidade de origem. O e-mail não deve aparecer publicamente no perfil.
- **Atores:** Usuário autenticado.
- **Prioridade:** Importante
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/SEG-04, RNF/PER-06, RNF/LEG-01, RNF/LEG-05, RNF/LEG-06, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Usuário autenticado para editar o próprio perfil; perfil existente para consulta.
- **Saídas e pós-condições:** Perfil atualizado; somente informações públicas exibidas a terceiros.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre a página de perfil e visualiza seus dados cadastrados.
2. O proprietário seleciona Editar e altera nome de exibição, biografia ou cidade de origem.
3. O sistema valida os campos e salva as alterações.
4. O sistema atualiza o perfil sem expor e-mail ou dados sensíveis.

**Fluxo alternativo 1:**
No passo 3, nome vazio ou campo acima do limite informado: rejeitar e indicar a correção.

**Fluxo alternativo 2:**
No passo 2, tentativa de editar perfil alheio: negar a operação.

---

## RF-04 — Cadastrar e consultar viagens

- **Descrição:** O sistema deve permitir cadastrar uma viagem privada com título, destino e datas de início e término, calcular sua duração em dias e consultar as viagens da própria conta. A edição e exclusão seguem RF-05.
- **Atores:** Usuário autenticado.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/SEG-04, RNF/PER-06, RNF/LEG-01, RNF/LEG-05, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Usuário autenticado; destino e período informados.
- **Saídas e pós-condições:** Viagem privada registrada e vinculada ao usuário, pronta para receber roteiros.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário acessa Minhas viagens e seleciona Nova viagem.
2. Informa título, destino e datas de início e término.
3. O sistema identifica o destino e calcula os dias, incluindo as datas inicial e final.
4. O sistema valida os dados, registra a viagem e a apresenta na lista pessoal.

**Fluxo alternativo 1:**
No passo 3, destino ambíguo: apresentar opções de cidade, região e país para escolha.

**Fluxo alternativo 2:**
No passo 4, término anterior ao início ou destino não identificado: solicitar correção, sem salvar.

---

## RF-05 — Editar ou excluir viagem

- **Descrição:** O sistema deve permitir ao proprietário editar uma viagem ou excluí-la após confirmação. A exclusão deve remover seus roteiros e itens do carrinho vinculados; a mensagem de confirmação deve informar esse efeito.
- **Atores:** Usuário autenticado proprietário.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/SEG-04, RNF/PER-06, RNF/LEG-01, RNF/LEG-02, RNF/LEG-05, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Viagem existente e pertencente ao usuário.
- **Saídas e pós-condições:** Viagem atualizada ou excluída; na exclusão, seus roteiros e itens do carrinho deixam de estar disponíveis. Viagens de outras contas permanecem inacessíveis.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre uma viagem e escolhe Editar ou Excluir.
2. Na edição, altera os dados e solicita salvar.
3. O sistema valida o período e informa conflitos com dias já planejados antes de concluir.
4. Na exclusão, o sistema informa os roteiros afetados e executa a remoção somente após confirmação.

**Fluxo alternativo 1:**
No passo 3, redução do período com atividades fora do intervalo: exigir realocação ou remoção antes de salvar.

**Fluxo alternativo 2:**
No passo 1, usuário sem propriedade: negar a operação. No passo 4, cancelamento: preservar a viagem e seus roteiros.

---

## RF-08 — Consultar detalhes de locais e atividades

- **Descrição:** O sistema deve apresentar, ao selecionar um card do Feed Explorar ou um item da viagem, nome, categoria, descrição e localização de um local; endereço, coordenadas, horários de funcionamento, duração estimada da visita e custos são exibidos quando disponíveis. Dados ausentes devem ser identificados, com origem das informações.
- **Atores:** Visitante e usuário autenticado; serviço de locais, quando utilizado.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/PER-06, RNF/PER-09, RNF/LEG-03, RNF/LEG-13, RNF/LEG-15, RNF/ECO-01
- **Entradas e pré-condições:** Local selecionado em um card do feed, no mapa ou em um roteiro acessível.
- **Saídas e pós-condições:** Detalhes disponíveis exibidos, sem prometer horários, preços ou disponibilidade não confirmados.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário seleciona um local em uma área acessível da plataforma.
2. O sistema recupera seus dados e identifica informações ausentes.
3. O sistema apresenta os detalhes e a localização em mapa, quando disponível.
4. O usuário pode retornar à lista ou, autenticado, adicionar o local ao carrinho, conforme RF-10.

**Fluxo alternativo 1:**
No passo 2, local removido do catálogo: informar indisponibilidade; atividades já salvas conservam nome e observações, sinalizando dados não verificados.

**Fluxo alternativo 2:**
No passo 3, ausência de descrição, coordenadas, horários ou custos: indicar Não informado; falha no mapa mantém os detalhes textuais.

---

## RF-09 — Visualizar Feed Explorar

- **Descrição:** O sistema deve apresentar, para o destino da viagem, um feed de cards de locais e atividades (atrações, museus e refeições) com foto, categoria, bairro e horário de funcionamento, permitindo filtragem por categoria. Cada card permite consultar detalhes (RF-08) ou adicionar o local ao carrinho (RF-10).
- **Atores:** Usuário autenticado; serviço de locais, quando utilizado.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/PER-06, RNF/PER-09, RNF/LEG-03, RNF/LEG-13, RNF/LEG-15
- **Entradas e pré-condições:** Viagem cadastrada e selecionada; catálogo de locais do destino acessível.
- **Saídas e pós-condições:** Cards do destino exibidos, com indicação dos locais já adicionados, para consulta em RF-08 ou inclusão no carrinho em RF-10.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário acessa Explorar com uma viagem selecionada.
2. O sistema consulta o catálogo do destino e aplica o filtro de categoria selecionado, quando houver.
3. O sistema exibe os cards com foto, categoria, bairro e horário, indicando os locais já adicionados.
4. O usuário consulta os detalhes de um card (RF-08) ou o adiciona ao carrinho (RF-10).

**Fluxo alternativo 1:**
No passo 1, sem viagem selecionada: solicitar a criação ou a escolha de uma viagem (RF-04). No passo 3, nenhum local compatível com o filtro selecionado: oferecer limpar o filtro.

**Fluxo alternativo 2:**
No passo 2, fonte externa indisponível: usar dados disponíveis ou informar a falha; não inventar locais, horários ou preços.

---

## RF-10 — Adicionar atração ao carrinho

- **Descrição:** O sistema deve permitir adicionar um local ao carrinho da viagem, informando opcionalmente o dia e o horário de início, e confirmando a duração estimada da visita (sugerida pelo catálogo). Sem dia, o item entra no carrinho global da viagem; com dia, no carrinho do dia; com dia e horário, entra diretamente no roteiro do dia.
- **Atores:** Usuário autenticado proprietário.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/PER-06, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Viagem própria; local exibido no Feed Explorar ou na tela de detalhes.
- **Saídas e pós-condições:** Item registrado no carrinho (global ou do dia) ou no roteiro do dia, com o contador do carrinho atualizado e o local marcado como adicionado.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário seleciona adicionar em um local (card do Feed Explorar ou tela de detalhes).
2. Escolhe um dia ou “Qualquer dia”, informa o horário (opcional) e confirma a duração estimada.
3. O sistema valida o dia e a duração.
4. O sistema registra o item e atualiza o contador do carrinho.

**Fluxo alternativo 1:**
No passo 3, duração inválida ou dia fora da viagem: solicitar correção sem adicionar o item.

**Fluxo alternativo 2:**
No passo 4, local já adicionado à viagem: sinalizar que ele já foi adicionado e não criar item duplicado sem confirmação.

---

## RF-11 — Gerenciar itens não alocados

- **Descrição:** O sistema deve exibir os itens não alocados em duas listas: a global da viagem (itens sem dia preferido) e a de cada dia (itens com dia, mas sem horário), mostrando duração e restrições e um contador visual no cabeçalho. Deve permitir atribuir um item global a um dia, editar sua duração e restrição (RF-12) e removê-lo.
- **Atores:** Usuário autenticado proprietário.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/PER-06, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Viagem própria com ao menos um item no carrinho.
- **Saídas e pós-condições:** Listas e contador atualizados; itens atribuídos a um dia disponíveis para a geração inteligente (RF-13) ou alocação manual no roteiro do dia (RF-14 e RF-15).

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre a viagem em Minhas viagens e acessa os itens não alocados.
2. O sistema exibe os itens globais, os itens de cada dia e o contador total.
3. O usuário atribui um item global a um dia, edita sua duração ou o remove.
4. O sistema atualiza as listas e o contador.

**Fluxo alternativo 1:**
No passo 2, carrinho vazio: informar que não há itens e orientar o acesso ao Feed Explorar (RF-09).

**Fluxo alternativo 2:**
No passo 3, duração inválida: rejeitar a alteração e manter o valor anterior.

---

## RF-12 — Definir restrições de horário

- **Descrição:** O sistema deve permitir definir, para cada item do carrinho, uma restrição de horário: livre, início fixo em um horário determinado ou janela de visita entre dois horários, além do dia preferencial. As restrições são respeitadas na geração inteligente (RF-13) e auxiliam a montagem e validação manual do roteiro do dia (RF-14 e RF-15).
- **Atores:** Usuário autenticado proprietário.
- **Prioridade:** Importante
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/PER-06, RNF/LEG-13, RNF/LEG-15
- **Entradas e pré-condições:** Item existente no carrinho; horário válido informado.
- **Saídas e pós-condições:** Restrição salva no item; item sem restrição permanece livre.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre um item do carrinho.
2. Escolhe o tipo de restrição e informa o horário de início ou os limites da janela.
3. O sistema valida os horários e a compatibilidade com a duração da visita.
4. O sistema salva a restrição e a indica no item.

**Fluxo alternativo 1:**
No passo 3, janela menor que a duração da visita ou horário inválido: solicitar correção sem salvar.

**Fluxo alternativo 2:**
No passo 3, horário fora do funcionamento conhecido do local: avisar o usuário e permitir manter a restrição.

---

## RF-13 — Gerar roteiro inteligente (viagem ou dia)

- **Descrição:** O sistema deve gerar o roteiro de um dia, ou distribuir os itens do carrinho global entre os dias e gerar o roteiro de cada um, ordenando as atividades por proximidade e respeitando as restrições de horário (RF-12) e os horários de saída e retorno do dia. Visitas e deslocamentos estimados que excedam o horário de retorno devem ser sinalizados, sem sobrepor atividades; estimativas ausentes exigem revisão.
- **Atores:** Usuário autenticado proprietário; serviços de locais e rotas, quando utilizados.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-03, RNF/PER-06, RNF/PER-08, RNF/PER-09, RNF/LEG-13, RNF/LEG-14, RNF/LEG-15, RNF/ECO-01
- **Entradas e pré-condições:** Viagem própria com itens no carrinho (global ou do dia); horários de saída e retorno definidos.
- **Saídas e pós-condições:** Roteiros dos dias gerados e salvos como rascunho privado, com sequência, horários e durações; violações de restrição e pendências de estimativas identificadas.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário solicita Distribuir toda a viagem ou Gerar roteiro do dia; se houver roteiro a substituir, confirma a substituição.
2. Na distribuição, o sistema agrupa os itens globais por proximidade e os atribui aos dias com menos itens.
3. Em cada dia, o sistema ordena as atividades respeitando as restrições e estima deslocamentos e horários.
4. O sistema salva os roteiros e apresenta os totais diários e os alertas para revisão.

**Fluxo alternativo 1:**
No passo 3, restrições incompatíveis ou tempo insuficiente no dia: gerar o roteiro mantendo a atividade e sinalizando o alerta, sem excluir itens nem exceder silenciosamente o horário de retorno.

**Fluxo alternativo 2:**
No passo 3, deslocamento ou duração desconhecidos: identificar a estimativa como pendente e não afirmar que o dia foi validado. Falha que impeça a geração preserva o roteiro anterior.

---

## RF-14 — Gerar roteiro manualmente

- **Descrição:** O sistema deve permitir montar o roteiro de um dia sem geração automática, definindo manualmente o horário de início e a ordem de cada atração selecionada. Cada dia da viagem possui um roteiro, criado vazio junto com a viagem; as atividades são incluídas e organizadas conforme RF-15.
- **Atores:** Usuário autenticado proprietário.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/PER-06, RNF/PER-08, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Viagem cadastrada com ao menos um dia; usuário com permissão de edição.
- **Saídas e pós-condições:** Roteiro do dia com horários definidos manualmente, salvo como privado e disponível para gerenciamento e visualização (RF-15, RF-16 e RF-17).

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre um dia da viagem e escolhe montar o roteiro manualmente.
2. Seleciona as atrações do dia e informa o horário de início de cada uma.
3. O sistema valida os horários, calcula o término estimado a partir da duração e indica eventuais conflitos de sobreposição.
4. O sistema salva o roteiro do dia e o apresenta; o usuário pode ajustar as atividades conforme RF-15.

**Fluxo alternativo 1:**
No passo 3, horário inválido ou sobreposição entre atividades: solicitar correção ou salvar somente após ciência do aviso.

**Fluxo alternativo 2:**
No passo 1, viagem inexistente: direcionar ao cadastro de RF-04; cancelamento preserva os registros existentes.

---

## RF-15 — Gerenciar e salvar atividades do roteiro

- **Descrição:** O sistema deve permitir ao proprietário adicionar, substituir, editar, remover e reorganizar atividades entre os dias, com horários, duração e observações, inclusive devolvendo atividades aos não alocados (RF-11), e salvar explicitamente as alterações. Deve permitir limpar o roteiro de um dia após confirmação.
- **Atores:** Usuário autenticado proprietário.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PROC-12, RNF/SEG-01, RNF/SEG-04, RNF/PER-06, RNF/LEG-07, RNF/LEG-15
- **Entradas e pré-condições:** Roteiro próprio existente e viagem válida; atividades devem ocupar dias pertencentes à viagem.
- **Saídas e pós-condições:** Roteiro salvo com as alterações confirmadas ou limpo após confirmação; cancelamento preserva a versão anteriormente salva.

### Fluxos de eventos

**Fluxo principal:**
1. O proprietário abre o roteiro de um dia em edição e adiciona locais do catálogo/carrinho ou atividades manuais com nome e localização, quando conhecida.
2. Edita horários, durações e observações; remove, substitui ou move atividades entre dias, posições e a lista de não alocados (RF-11).
3. O sistema valida dias, horários e durações e indica sobreposições ou excesso do intervalo disponível.
4. Ao confirmar Salvar, o sistema persiste e reapresenta o roteiro. Limpar o dia exige confirmação.

**Fluxo alternativo 1:**
No passo 3, dia fora da viagem, nome vazio ou duração inválida: impedir salvamento até correção. Local manual sem coordenadas fica com localização pendente.

**Fluxo alternativo 2:**
No passo 3, sobreposição ou excesso de tempo: salvar somente após ciência do aviso. No passo 4, falha de gravação ou cancelamento preserva a versão anteriormente salva.

---

## RF-16 — Visualizar roteiro em timeline e alternar modos de visualização

- **Descrição:** O sistema deve exibir o roteiro de cada dia em uma linha do tempo cronológica, com os cards agrupados e coloridos por turno (Manhã, Tarde, Noite e Madrugada), indicando horários e durações. Deve permitir alternar entre os modos Planejamento (itens não alocados e timeline), Dividido (timeline e mapa lado a lado) e Mapa (RF-17).
- **Atores:** Usuário com acesso ao roteiro.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-02, RNF/PROC-06, RNF/PER-06, RNF/PER-07, RNF/LEG-15
- **Entradas e pré-condições:** Viagem com ao menos um dia e roteiro gerado ou montado.
- **Saídas e pós-condições:** Timeline do dia exibida e modo de visualização selecionado, mantendo o mesmo dia.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre um dia da viagem.
2. O sistema exibe a timeline com as atividades ordenadas e agrupadas por turno, em cores distintas.
3. O usuário seleciona o modo Planejamento, Dividido ou Mapa.
4. O sistema atualiza a tela mantendo o dia e destaca, nos dois painéis, a atividade selecionada.

**Fluxo alternativo 1:**
No passo 2, dia sem atividades alocadas: informar que o dia ainda não tem roteiro e orientar a geração inteligente (RF-13) ou a montagem manual (RF-14).

**Fluxo alternativo 2:**
No passo 3, mapa indisponível nos modos Dividido ou Mapa: manter a timeline e permitir nova tentativa.

---

## RF-17 — Visualizar roteiro no mapa

- **Descrição:** O sistema deve apresentar no mapa os pontos de um roteiro, numerados conforme a ordem de visita e coloridos conforme o turno (Manhã, Tarde, Noite ou Madrugada). No zoom detalhado, exibe os pinos numerados e as linhas de rota entre eles; no zoom afastado, remove as rotas e exibe apenas os pinos, mostrando a distribuição dos pontos na cidade. O usuário deve poder selecionar um ponto para consultar seus detalhes.
- **Atores:** Usuário com acesso ao roteiro; serviço de mapas.
- **Prioridade:** Essencial
- **Requisitos Não Funcionais Associados:** RNF/PROC-01, RNF/PROC-02, RNF/PROC-06, RNF/PROC-10, RNF/PER-06, RNF/PER-07, RNF/LEG-03, RNF/LEG-15, RNF/ECO-01
- **Entradas e pré-condições:** Roteiro próprio acessível; atividades com coordenadas para exibição.
- **Saídas e pós-condições:** Mapa e lista do roteiro apresentados de forma correspondente.

### Fluxos de eventos

**Fluxo principal:**
1. O usuário abre a visualização de mapa do roteiro.
2. O sistema apresenta os pontos localizados e a sequência planejada.
3. O usuário alterna entre o zoom detalhado e o zoom afastado ou seleciona um marcador.
4. O sistema ajusta a exibição de pinos e rotas ou destaca a atividade correspondente e exibe seus detalhes.

**Fluxo alternativo 1:**
No passo 2, ponto sem coordenadas: mantê-lo na lista e informar que não foi localizado.

**Fluxo alternativo 2:**
No passo 2, mapa indisponível: manter a consulta textual do roteiro e permitir nova tentativa.

---
