# TripShare Authentication

O TripShare usa o Firebase Authentication para autenticar o usuário e o Spring Security para validar sua identidade a cada requisição. Após login/cadastro, o backend entrega um Firebase Session Cookie em `Set-Cookie`; o cookie é `HttpOnly`, portanto não fica acessível ao JavaScript. Para concluir o login no TripShare, o usuário precisa ter uma conta no Firebase e um perfil cadastrado no PostgreSQL.
O Firebase gerencia as credenciais de acesso e atribui a cada conta um identificador chamado UID. No PostgreSQL, a tabela users armazena o perfil do usuário, incluindo o e-mail. O campo users.auth_id referencia auth.id, e o campo auth.uuid armazena o UID do Firebase.

## Fluxo de login

1. O usuário entra pelo frontend. Ele informa e-mail e senha, e o frontend usa o Firebase Authentication para autenticar a conta. Após o login, o frontend obtém o ID token e o UID do usuário.
2. O frontend chama o backend. Em `POST /auth/login` ou `POST /auth/register`, envia o ID token no cabeçalho Authorization e o UID no campo uuid do corpo da requisição. Após sucesso, o backend devolve um Firebase Session Cookie em `Set-Cookie`; o frontend não deve armazenar o ID token.
```bash
POST /auth/login
Authorization: Bearer <ID_TOKEN_DO_FIREBASE>
Content-Type: application/json

{ "uuid": "<UID_DO_USUARIO_NO_FIREBASE>" }
```

3. O backend valida o token. Antes de a requisição chegar ao controller, o filtro usa o Firebase Admin SDK para verificar assinatura, validade e projeto do ID token. Após a validação, disponibiliza UID e e-mail ao Spring Security. O cookie de sessão só é emitido após validar também o perfil e a igualdade entre o UID validado e o `uuid` enviado.
4. O TripShare localiza o perfil. O serviço confere se o uuid enviado no corpo corresponde ao UID validado. Em seguida, busca o perfil no PostgreSQL e verifica a situação da conta. Se o perfil existir e estiver ativo, o backend mantém o status e o corpo atuais e inclui o cookie em `Set-Cookie`.

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
    | `csrf(AbstractHttpConfigurer::disable)` | Mantém a configuração existente de CSRF da API. |
    | `SessionCreationPolicy.STATELESS` | O Spring não mantém sessão HTTP própria; cada requisição protegida apresenta o Firebase Session Cookie. |
    | `anyRequest().authenticated()` | Exige autenticação nas rotas protegidas. Login/cadastro exigem o ID token Bearer; logout pode ser chamado sem autenticação obrigatória. |
   | `addFilterBefore(...)` | Posiciona o `FirebaseAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`. Assim, o filtro do TripShare pode validar o token e preencher a identidade do usuário antes de o Spring avaliar o acesso à rota. |
   | `exceptionHandling(...)` | Configura respostas JSON para falhas tratadas pelo Spring Security. O `authenticationEntryPoint` responde com `401` quando é necessária autenticação, e o `accessDeniedHandler` responde com `403` quando o acesso é negado. |

### Fluxo
0. Antes da primeira requisição, o Spring prepara o backend. FirebaseConfig.firebaseApp(...) 
lê as credenciais e inicializa o Firebase Admin SDK. FirebaseConfig.firebaseAuth(...) fornece 
o cliente usado para verificar os tokens. Por fim, SecurityConfig.filterChain(...) registra a cadeia de segurança e inclui o
FirebaseAuthenticationFilter. O @Bean faz essa configuração ficar disponível durante a execução da aplicação.
1. Login/cadastro chegam com o ID token no cabeçalho `Authorization`; o cookie ainda não substitui esse token nessas operações. As demais rotas usam o cookie de sessão. Exemplo:
```bash
    POST /auth/login
Authorization: Bearer <ID_TOKEN_DO_FIREBASE>
Content-Type: application/json

{ "uuid": "<UID_DO_USUARIO_NO_FIREBASE>" }
```
2. `FirebaseAuthenticationFilter` valida Bearer no login/cadastro. Nas demais rotas, tenta primeiro o cookie e valida com `FirebaseAuth.verifySessionCookie(cookie, checkRevoked)`. O fallback Bearer só ocorre quando o cookie está ausente e `app.auth.bearer-fallback-enabled=true`.
3. O filtro registra `AuthenticatedUser` no contexto do Spring Security, mantendo o mesmo principal usado pelos controllers. Cookie inválido/expirado/revogado resulta em 401; indisponibilidade do Firebase resulta em 503. O valor do cookie e o ID token não são registrados em logs.
4. O filtro registra quem fez a requisição. De volta a doFilterInternal(...), o filtro cria um contexto de segurança e coloca nele o AuthenticatedUser retornado pelo serviço: O construtor de três argumentos cria uma autenticação marcada como válida. identity identifica o usuário, e List.of() indica que o filtro não atribuiu permissões específicas a ele. setContext(...) disponibiliza essa identidade para o restante da requisição. chain.doFilter(...) permite que o processamento continue.
5. O Spring Security aplica a regra .authenticated(). A regra configurada em SecurityConfig.filterChain(...) consulta a autenticação registrada no passo anterior. Como o contexto contém um usuário autenticado, o Spring permite que a chamada siga para o endpoint. Até aqui, o fluxo é comum às rotas protegidas
6. Controllers continuam recebendo `@AuthenticationPrincipal AuthenticatedUser`; os serviços de perfil e viagem preservam o contrato.
7. `AuthController` mantém status e corpo atuais de login/cadastro e solicita ao Firebase a criação do Session Cookie após as validações.
8. AuthService.login(...) confere o UID e busca o perfil no PostgreSQL. Primeiro, requireSameUid(request.uuid(), identity) compara o UID informado no corpo com o UID extraído do token. Se forem diferentes, o serviço lança AuthException com o motivo UID_MISMATCH. Se coincidirem, UserRepository.findByAuthUuid(identity.uid()) busca o perfil associado ao UID autenticado. O serviço também verifica se a conta está suspensa. ApiExceptionHandler.authentication(...) converte essas falhas em respostas HTTP:
   | Situação | Resposta |
   |---|---|
   | O UID do corpo difere do UID do token | `401 Unauthorized` |
   | O UID é válido, mas o perfil ainda não existe no PostgreSQL | `404 Not Found` |
   | O perfil existe e está suspenso | `403 Forbidden` |
9. `UserResponse.from(user)` monta a resposta JSON, que permanece igual; a sessão é transportada separadamente em `Set-Cookie`. `POST /auth/logout` expira o cookie e tenta revogar refresh tokens. Desligue o fallback Bearer após a migração do frontend.
