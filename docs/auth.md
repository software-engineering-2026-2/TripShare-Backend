# TripShare Authentication

O TripShare usa o Firebase Authentication para autenticar o usuário e o Spring Security para validar sua identidade a cada requisição. Para concluir o login no TripShare, o usuário precisa ter uma conta no Firebase e um perfil cadastrado no PostgreSQL.
O Firebase gerencia as credenciais de acesso e atribui a cada conta um identificador chamado UID. No PostgreSQL, a tabela users armazena o perfil do usuário, incluindo o e-mail. O campo users.auth_id referencia auth.id, e o campo auth.uuid armazena o UID do Firebase.

## Fluxo de login

1. O usuário entra pelo frontend. Ele informa e-mail e senha, e o frontend usa o Firebase Authentication para autenticar a conta. Após o login, o frontend obtém o ID token e o UID do usuário.
2. O frontend chama o backend. Na requisição POST /auth/login, o frontend envia o ID token no cabeçalho Authorization e o UID no campo uuid do corpo da requisição.
```bash
POST /auth/login
Authorization: Bearer <ID_TOKEN_DO_FIREBASE>
Content-Type: application/json

{ "uuid": "<UID_DO_USUARIO_NO_FIREBASE>" }
```

3. O backend valida o token. Antes de a requisição chegar ao controller, o filtro de autenticação usa o Firebase Admin SDK para verificar a assinatura, a validade e o projeto de destino do token. Após a validação, o filtro disponibiliza o UID e o e-mail presentes no token ao Spring Security.
4. O TripShare localiza o perfil. O serviço de login confere se o uuid enviado no corpo corresponde ao UID do token validado. Em seguida, busca o perfil no PostgreSQL por esse UID e verifica a situação da conta. Se o perfil existir e a conta estiver ativa, o backend responde com 200 OK e os dados do usuário.

```bash
Respostas de erro

Código HTTP	Quando ocorre
401 Unauthorized	O token está ausente, inválido ou expirado, ou o uuid enviado no corpo difere do UID do token.
403 Forbidden	O perfil existe no TripShare, mas a conta está suspensa.
404 Not Found	O token é válido, mas ainda não existe um perfil no TripShare associado ao UID.
503 Service Unavailable	O serviço de autenticação está indisponível e o backend não consegue validar o token.
```

## Fluxo de Autenticação no código

### Config
1. FirebaseProperties.java — localização das credenciais
   Essa classe representa a configuração de que o backend precisa para encontrar o arquivo de credenciais do Firebase. Ela é um record com um único campo, keyPath.
```bash
 A anotação @ConfigurationProperties(prefix = "app.firebase") faz o Spring associar a propriedade app.firebase.key-path ao campo Java keyPath. O valor vem de application.properties:
 app.firebase.key-path=${FIREBASE_KEY_PATH:file:../.secrets/firebase.json}
```

Se a variável de ambiente FIREBASE_KEY_PATH estiver definida, o Spring usa seu valor. Caso contrário, usa file:../.secrets/firebase.json. O prefixo file: indica um arquivo no sistema de arquivos, e o caminho relativo é resolvido a partir do diretório de execução do backend. Isso permite usar caminhos diferentes em desenvolvimento e em produção sem alterar o código Java.
As anotações @Validated e @NotBlank exigem que o caminho configurado contenha texto. A abertura do arquivo acontece na etapa seguinte. Essa distinção ajuda a diagnosticar uma falha de inicialização: um valor em branco falha na validação da propriedade, enquanto um caminho preenchido que aponta para um arquivo inexistente falha na leitura.

2. FirebaseConfig.java — inicialização do Firebase Admin SDK
   Essa classe usa o caminho fornecido por FirebaseProperties para inicializar o Firebase Admin SDK. A anotação @Configuration identifica a classe como configuração do Spring, e @EnableConfigurationProperties(FirebaseProperties.class) habilita o carregamento das propriedades descritas acima. Com @Profile("!test"), essa configuração só é ativada quando o perfil test está inativo. Assim, os testes podem fornecer substitutos para as dependências do Firebase.

A classe registra dois beans, objetos que o Spring cria e disponibiliza para injeção em outras classes:

| Bean | Como é criado | Para que serve |
|---|---|---|
| `FirebaseApp` | O método `firebaseApp(...)` abre o arquivo com `ResourceLoader`, carrega as credenciais com `GoogleCredentials.fromStream(...)` e inicializa uma instância chamada `tripshare`. | Mantém a configuração do SDK durante a execução do backend. Ao encerrar a aplicação, o Spring chama `delete()` nessa instância, conforme `@Bean(destroyMethod = "delete")`. |
| `FirebaseAuth` | O método `firebaseAuth(...)` recebe o `FirebaseApp` já inicializado e chama `FirebaseAuth.getInstance(app)`. | Fornece o cliente que o `FirebaseTokenService` usa para verificar os ID tokens recebidos pela API. |

3. SecurityConfig.java — proteção das rotas da API
   Essa classe define o bean SecurityFilterChain, que determina quais regras de segurança o Spring aplica às requisições HTTP. É aqui que o TripShare registra o filtro que funciona como middleware de autenticação.
   | Configuração | Efeito no TripShare |
   |---|---|
   | `csrf(AbstractHttpConfigurer::disable)` | Desativa a proteção CSRF do Spring para essa cadeia. A API recebe a credencial do usuário explicitamente pelo cabeçalho `Authorization`. |
   | `SessionCreationPolicy.STATELESS` | O Spring Security não usa uma sessão HTTP para manter o usuário autenticado entre chamadas. Cada requisição precisa apresentar seu próprio Bearer token. |
   | `anyRequest().authenticated()` | Exige autenticação em todas as rotas, inclusive `POST /auth/login` e `POST /auth/register`. Nesses dois casos, o frontend já deve ter obtido o ID token no Firebase. |
   | `addFilterBefore(...)` | Posiciona o `FirebaseAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`. Assim, o filtro do TripShare pode validar o token e preencher a identidade do usuário antes de o Spring avaliar o acesso à rota. |
   | `exceptionHandling(...)` | Configura respostas JSON para falhas tratadas pelo Spring Security. O `authenticationEntryPoint` responde com `401` quando é necessária autenticação, e o `accessDeniedHandler` responde com `403` quando o acesso é negado. |

### Fluxo
0. Antes da primeira requisição, o Spring prepara o backend. FirebaseConfig.firebaseApp(...) 
lê as credenciais e inicializa o Firebase Admin SDK. FirebaseConfig.firebaseAuth(...) fornece 
o cliente usado para verificar os tokens. Por fim, SecurityConfig.filterChain(...) registra a cadeia de segurança e inclui o
FirebaseAuthenticationFilter. O @Bean faz essa configuração ficar disponível durante a execução da aplicação.
1. A requisição chega ao backend com o token do Firebase. O cliente envia o ID token no cabeçalho Authorization. Para chamar POST /auth/login, a API do TripShare também exige o UID do Firebase no campo uuid do corpo. Uma chamada a esse endpoint tem o seguinte formato:
```bash
    POST /auth/login
Authorization: Bearer <ID_TOKEN_DO_FIREBASE>
Content-Type: application/json

{ "uuid": "<UID_DO_USUARIO_NO_FIREBASE>" }
```
2. FirebaseAuthenticationFilter.doFilterInternal(...) lê o cabeçalho. Antes de a requisição chegar ao controller, o filtro chama request.getHeader("Authorization"), verifica o prefixo Bearer  e extrai o token. Se o cabeçalho estiver ausente, o formato estiver incorreto ou o token estiver vazio.
3. FirebaseTokenService.validate(...) verifica o token. O filtro entrega o token ao serviço, que chama FirebaseAuth.verifyIdToken(...). Após a verificação, o serviço extrai token.getUid() e token.getEmail() e devolve new AuthenticatedUser(uid, email). Um token inválido ou expirado resulta em 401 Unauthorized. Se o serviço de autenticação estiver indisponível, o filtro devolve 503 Service Unavailable.
4. O filtro registra quem fez a requisição. De volta a doFilterInternal(...), o filtro cria um contexto de segurança e coloca nele o AuthenticatedUser retornado pelo serviço: O construtor de três argumentos cria uma autenticação marcada como válida. identity identifica o usuário, e List.of() indica que o filtro não atribuiu permissões específicas a ele. setContext(...) disponibiliza essa identidade para o restante da requisição. chain.doFilter(...) permite que o processamento continue.
5. O Spring Security aplica a regra .authenticated(). A regra configurada em SecurityConfig.filterChain(...) consulta a autenticação registrada no passo anterior. Como o contexto contém um usuário autenticado, o Spring permite que a chamada siga para o endpoint. Até aqui, o fluxo é comum às rotas protegidas
6. No controller que precisa de autenticação, deve receber o corpo com a identidade @AuthenticationPrincipal AuthenticatedUser identity recupera a identidade registrada pelo filtro no passo 4. Com os dois argumentos prontos, o controller chama authService.login(request, identity).
7. **Os próximos passos são específicos de POST /auth/login.**  AuthController.login(...) recebe o corpo e a identidade. @Valid @RequestBody LoginRequest request lê o JSON e verifica se uuid foi preenchido e tem até 128 caracteres. Um corpo inválido resulta em 400 Bad Request. Já @AuthenticationPrincipal AuthenticatedUser identity recupera a identidade registrada pelo filtro no passo 4. Com os dois argumentos prontos, o controller chama authService.login(request, identity).
8. AuthService.login(...) confere o UID e busca o perfil no PostgreSQL. Primeiro, requireSameUid(request.uuid(), identity) compara o UID informado no corpo com o UID extraído do token. Se forem diferentes, o serviço lança AuthException com o motivo UID_MISMATCH. Se coincidirem, UserRepository.findByAuthUuid(identity.uid()) busca o perfil associado ao UID autenticado. O serviço também verifica se a conta está suspensa. ApiExceptionHandler.authentication(...) converte essas falhas em respostas HTTP:
   | Situação | Resposta |
   |---|---|
   | O UID do corpo difere do UID do token | `401 Unauthorized` |
   | O UID é válido, mas o perfil ainda não existe no PostgreSQL | `404 Not Found` |
   | O perfil existe e está suspenso | `403 Forbidden` |
9. UserResponse.from(user) monta a resposta. Se as verificações passarem, o serviço converte o perfil em UserResponse. O controller devolve esse objeto, e o Spring o envia como JSON com 200 OK. Como a política de sessão é STATELESS, a próxima chamada protegida deve enviar novamente um ID token válido e repete os passos 1 a 5 antes de chegar ao seu próprio controller.