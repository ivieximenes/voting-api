# API de Votação em Pautas
API REST em Java 21 + Spring Boot 3 para cadastro de pautas, abertura de sessões de votação, registro de votos dos associados e apuração de resultado.

## Como executar
Pré-requisitos: Docker e Docker Compose, ou Java 21 + Maven 3.9+ e  PostgreSQL.

### Opção A- tudo via Docker Compose (recomendado)
Sobe o Postgres e a API juntos, já conectados entre si:

```bash
docker-compose up --build
```

A **API** fica em `http://localhost:8080` e o **Postgres** em **localhost:5432**
**(db=voting, user=voting, password=voting )** em **docker-compose.yml**. 
Os dados do **Postgres** ficam num volume Docker nomeado **(voting-db-data)** e sobrevivem a um restart tanto da API quanto do banco e só são apagados com **docker-compose down -v**.

### Opção B- Postgres em Docker, API local (ciclo de desenvolvimento mais rápido)
Sobe só o banco em container e roda a API direto pelo Maven evitando reconstruir a imagem a cada mudança de código.

```bash
docker-compose up -d db
mvn spring-boot:run
```

### Opção C- Sem Docker
É necessario um PostgreSQL onde as configurações podem ser editadas no arquivo application.yml

```
DB_HOST=localhost DB_PORT=5432 DB_NAME=voting DB_USER=voting DB_PASSWORD=voting
```
```
mvn spring-boot:run
```

## CONSIDERAÇÕES
- Segurança foi abstraída conforme orientação do desafio.
- **Um voto por associado por pauta é garantido por constraint única** (`uk_vote_topic_member`), evitando race condition e retrabalho em concorrência.
- **A apuração usa `countByTopicIdAndOption`** com índice em `(topic_id, vote_option)`, evitando full scan em `vote`.
- **`@Transactional(readOnly = true)` nos métodos de leitura** permite otimizações do Hibernate (sem dirty checking).
- **`open-in-view: false`** — sem lazy loading implícito em controllers.
- **Sem `@ManyToOne`** entre `Vote` e `Topic` — menos joins na apuração.
- **Hikari configurado** com pool de 20 conexões para suportar carga concorrente.
- **Em cenários de centenas de milhares de votos**, a apuração poderia ser otimizada com:
  - **View materializada** ou **tabela de contagem** atualizada por trigger/job.
  - **Cache** do resultado (Redis ou Caffeine) com invalidação no voto.
  - **Paginação** se a lista de votos for exposta.

## Documentação da API
Com a aplicação em execução, a documentação interativa (Swagger UI) fica disponível em:

```
http://localhost:8080/swagger-ui.html
```

A especificação OpenAPI em JSON fica em `http://localhost:8080/v3/api-docs`.

## Telas do app mobile (Anexo 1)
Além dos endpoints REST convencionais (`/api/v1/topics`, `/api/v1/topics/{id}/sessions`, `/api/v1/topics/{id}/votes`), a API expõe um segundo grupo de endpoints, em `/api/v1/screens`, que devolve as respostas no formato de tela FORMULARIO/SELECAO descrito no Anexo 1 do desafio:

| Endpoint | Tela | Uso |
|---|---|---|
| `GET /api/v1/screens/topics/new` | FORMULARIO | Campos para cadastrar uma nova pauta; `botaoOk` aponta para `POST /api/v1/topics` |
| `GET /api/v1/screens/topics` | SELECAO | Lista de pautas; cada item aponta para a tela de votação (se a sessão estiver aberta) ou para o resultado (se estiver fechada ou não existir sessão) |
| `GET /api/v1/screens/topics/{topicId}/vote` | SELECAO | Opções Sim/Não; cada item aponta para `POST /api/v1/topics/{topicId}/votes` com a opção já no `body` |

O `memberId` do associado não é incluído no `body` das telas — ele identifica o usuário autenticado no app e é adicionado pelo cliente ao montar a requisição, já que autenticação está fora do escopo do desafio.

O domínio usado nas URLs das telas é configurável via `sicredi.app.base-url` (ou a variável de ambiente `APP_BASE_URL`), conforme a orientação do desafio de manter as URLs de callback configuráveis para teste em emulador ou dispositivo físico. Ao rodar via `docker-compose up`, essa variável não é definida e assume o default `http://localhost:8080`; para testar contra a API em outro host (emulador Android, dispositivo físico na rede local, etc.), defina `APP_BASE_URL` com o endereço acessível a partir do cliente antes de subir a aplicação.

Os endpoints REST convencionais continuam disponíveis e são a via usada pelos testes automatizados e pelo Swagger UI; os endpoints de tela são uma camada adicional sobre eles.

## App de demonstração (pasta `app/`)
A pasta `app/` contém uma página HTML estática que simula o app mobile: consome os endpoints de tela em `/api/v1/screens` e os REST convencionais, e renderiza dinamicamente as telas FORMULARIO/SELECAO retornadas pela API — nada de layout fixo por tela, o HTML é montado a partir do JSON recebido a cada chamada.

Cobre o fluxo completo suportado pela API: listagem de pautas com indicação de status, cadastro de nova pauta, abertura de sessão de votação, votação (com confirmação de CPF) e visualização do resultado.

Para usar, abra `app/index.html` diretamente no navegador com a API em execução em `http://localhost:8080` (endereço configurável em `app.js` via `localStorage`, chave `apiBase`).

## Decisões de design
- **PostgreSQL + Flyway**: banco relacional real tanto em execução quanto nos testes (via Testcontainers), com o schema versionado em migrations (`src/main/resources/db/migration`).
- **ProblemDetail (RFC 7807)**: erros de API padronizados usando o suporte nativo do Spring (`spring.mvc.problemdetails.enabled`), sem um `GlobalExceptionHandler` customizado.
- **Uma sessão por pauta**: garantido por constraint única em `voting_session.topic_id`, refletindo a regra do desafio de que a votação de uma pauta acontece em uma única sessão.
- **Um voto por associado por pauta**: garantido por constraint única em `vote (topic_id, member_id)`, não só por checagem em códig, cobre também requisições concorrentes.
- **Versionamento por URI (`/api/v1`)**.

## Tarefas bônus

### Bônus 1 — Integração com sistema externo de validação de CPF
A validação usa RestClient configurado em RestClientConfig, com timeout de conexão e leitura configuráveis e um toggle (sicredi.member-validation.enabled) para desligar a chamada externa sem precisar de mocks em teste.

A URL default (sicredi.member-validation.base-url) aponta para uma aplicação própria publicada na Vercel (https://user-info-woad.vercel.app), construída para substituir o serviço https://user-info.herokuapp.com citado no desafio. Esse serviço original foi descontinuado pela Heroku e não responde mais. A aplicação substituta mantém o mesmo contrato do serviço original:

GET /users/{cpf} → 200 {"status": "ABLE_TO_VOTE"} ou 200 {"status": "UNABLE_TO_VOTE"}

GET /users/{cpf} com CPF inválido → 404

O resultado (ABLE/UNABLE) é aleatório, como no serviço original.

A URL é configurável via sicredi.member-validation.base-url (ou variável de ambiente MEMBER_VALIDATION_URL), então a avaliação pode apontar para outra implementação equivalente se preferir.

### Bônus 2 — Performance

Teste de carga com k6 (`load-test.js`): 50 VUs por 2 minutos, criando pautas, abrindo sessões, votando e apurando. Resultados em ambiente local (Postgres via Docker, API via `mvn spring-boot:run`):

- Throughput: ~X req/s
- p95 de latência: ~Y ms
- Taxa de erro: ~Z%

Os índices `uk_vote_topic_member` e `uk_voting_session_topic`, combinados com `@Transactional` e a ausência de lazy loading na apuração, garantem o comportamento sob carga.

### Bônus 3 — Versionamento da API
Estratégia adotada: **versionamento por URI** (`/api/v1/...`). Preferido a versionamento por header por ser explícito no path, facilitar roteamento, cache e leitura de logs, e por ser o padrão adotado por APIs públicas conhecidas (Stripe, GitHub). Para uma eventual v2, o plano é criar um novo pacote `web/v2` com os novos controllers, mantendo services e domínio compartilhados entre as versões.

## Cobertura de testes
O relatório de cobertura é gerado pelo JaCoCo na fase `verify`:

```bash
mvn clean verify
```

O relatório HTML fica em `target/site/jacoco/index.html`.
