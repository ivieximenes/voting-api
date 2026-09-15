# API de Votação em Pautas

API REST em Java 21 + Spring Boot 3 para cadastro de pautas, abertura de sessões de votação, registro de votos dos associados e apuração de resultado.

---

## Sobre o desafio

No cooperativismo, cada associado possui um voto e as decisões são tomadas em assembleias, por votação. Este projeto é uma implementação back-end para gerenciar essas sessões de votação, conforme desafio técnico proposto para vaga de Desenvolvedor Back-End Java.

### Requisitos atendidos

1. **Cadastrar uma nova pauta**
2. **Abrir uma sessão de votação em uma pauta** (tempo determinado na abertura, 1 minuto por default)
3. **Receber votos dos associados** (Sim/Não; cada associado identificado por id único vota apenas uma vez por pauta)
4. **Contabilizar os votos e dar o resultado** da votação na pauta

**Tarefas bônus entregues:**

- Bônus 1— Integração com sistema externo de validação de CPF
- Bônus 2— Performance (teste de carga com k6)
- Bônus 3— Versionamento da API

---

## Stack

| Camada | Tecnologia |
| ------ | ---------- |
| Linguagem | Java 21 |
| Framework | Spring Boot 3 |
| Banco | PostgreSQL + Flyway |
| Testes | JUnit + Testcontainers |
| Cobertura | JaCoCo |
| Carga | k6 |
| Docs | OpenAPI / Swagger UI |
| Container | Docker + Docker Compose |
| CI | GitHub Actions |

---

## Como executar

Pré-requisitos: Docker e Docker Compose, ou Java 21 + Maven 3.9+ e PostgreSQL.

### Opção A— via Docker Compose (recomendado)

Sobe o Postgres e a API juntos, já conectados entre si:

```bash
docker-compose up --build
```

A **API** fica em `http://localhost:8080` e o **Postgres** em `localhost:5432`.

Credenciais do banco (`db=voting`, `user=voting`, `password=voting`) estão em `docker-compose.yml`.

Os dados do **Postgres** ficam num volume Docker nomeado (`voting-db-data`) e sobrevivem a um restart tanto da API quanto do banco. Só são apagados com `docker-compose down -v`.

### Opção B— Postgres em Docker, API local

Sobe só o banco em container e roda a API direto pelo Maven, evitando reconstruir a imagem a cada mudança de código:

```bash
docker-compose up -d db
mvn spring-boot:run
```

### Opção C— Sem Docker

É necessário um PostgreSQL. As configurações podem ser editadas no arquivo `application.yml` ou via variáveis de ambiente:

```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=voting DB_USER=voting DB_PASSWORD=voting
```

```bash
mvn spring-boot:run
```

---

## Exemplo de uso

Fluxo completo do desafio, do cadastro da pauta até a apuração do resultado.

### 1. Criar uma pauta

```bash
curl -X POST http://localhost:8080/api/v1/topics \
  -H 'Content-Type: application/json' \
  -d '{"title":"Aumento de 3% no imposto de internet"}'
```

Resposta: `201 Created` com o `Location` da pauta criada.

### 2. Abrir a sessão de votação

```bash
curl -X POST http://localhost:8080/api/v1/topics/{topicId}/sessions \
  -H 'Content-Type: application/json' \
  -d '{"durationSeconds": 60}'
```

Se `durationSeconds` não for informado, a sessão fica aberta por **1 minuto** (default do desafio).

### 3. Votar

```bash
curl -X POST http://localhost:8080/api/v1/topics/{topicId}/votes \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"19839091069","option":"YES"}'
```

Opções de voto: `YES` ou `NO`. Cada associado pode votar **apenas uma vez por pauta**.

### 4. Consultar o resultado

```bash
curl http://localhost:8080/api/v1/topics/{topicId}/result
```

Após o tempo da sessão encerrar, novos votos são rejeitados.

---

## Documentação da API

Com a aplicação em execução, a documentação interativa (Swagger UI) fica disponível em:

```text
http://localhost:8080/swagger-ui.html
```

A especificação OpenAPI em JSON fica em:

```text
http://localhost:8080/v3/api-docs
```

---

## Telas do app mobile

Além dos endpoints REST convencionais (`/api/v1/topics`, `/api/v1/topics/{id}/sessions`, `/api/v1/topics/{id}/votes`), a API expõe um segundo grupo de endpoints, em `/api/v1/screens`, que devolve as respostas no formato de tela FORMULARIO/SELECAO descrito no desafio:

| Endpoint | Tela | Uso |
| -------- | ---- | --- |
| `GET /api/v1/screens/topics/new` | FORMULARIO | Campos para cadastrar uma nova pauta; `botaoOk` aponta para `POST /api/v1/topics` |
| `GET /api/v1/screens/topics` | SELECAO | Lista de pautas; cada item aponta para a tela de votação (se a sessão estiver aberta) ou para o resultado (se estiver fechada ou não existir sessão) |
| `GET /api/v1/screens/topics/{topicId}/vote` | SELECAO | Opções Sim/Não; cada item aponta para `POST /api/v1/topics/{topicId}/votes` com a opção já no `body` |

O `memberId` do associado não é incluído no `body` das telas, ele identifica o usuário autenticado no app e é adicionado pelo cliente ao montar a requisição, já que autenticação está fora do escopo do desafio.

O domínio usado nas URLs das telas é configurável via `sicredi.app.base-url` (ou a variável de ambiente `APP_BASE_URL`), conforme a orientação do desafio de manter as URLs de callback configuráveis para teste em emulador ou dispositivo físico. Ao rodar via `docker-compose up`, essa variável não é definida e assume o default `http://localhost:8080`. Para testar contra a API em outro host (emulador Android, dispositivo físico na rede local, etc.), defina `APP_BASE_URL` com o endereço acessível a partir do cliente antes de subir a aplicação.

Os endpoints REST convencionais continuam disponíveis e são a via usada pelos testes automatizados e pelo Swagger UI. Os endpoints de tela são uma camada adicional sobre eles.

---

## Decisões de design

- **PostgreSQL + Flyway**: banco relacional real tanto em execução quanto nos testes (via Testcontainers), com o schema versionado em migrations (`src/main/resources/db/migration`).
- **ProblemDetail (RFC 7807)**: erros de API padronizados usando o suporte nativo do Spring (`spring.mvc.problemdetails.enabled`), sem um `GlobalExceptionHandler` customizado.
- **Uma sessão por pauta**: garantido por constraint única em `voting_session.topic_id`, refletindo a regra do desafio de que a votação de uma pauta acontece em uma única sessão.
- **Um voto por associado por pauta**: garantido por constraint única em `vote (topic_id, member_id)`, não só por checagem em código, cobre também requisições concorrentes.
- **Versionamento por URI** (`/api/v1`).

### Considerações de performance

- **Um voto por associado por pauta é garantido por constraint única** (`uk_vote_topic_member`), evitando race condition e retrabalho em concorrência.
- **A apuração usa** `countByTopicIdAndOption` **com índice em** `(topic_id, vote_option)`, evitando full scan em `vote`.
- **`@Transactional(readOnly = true)` nos métodos de leitura** permite otimizações do Hibernate (sem dirty checking).
- **`open-in-view: false`** , sem lazy loading implícito em controllers.
- **Sem `@ManyToOne` entre `Vote` e `Topic`**, menos joins na apuração.
- **Hikari configurado** com pool de 20 conexões para suportar carga concorrente.


### Segurança

Segurança foi abstraída conforme orientação do desafio.

---

## Tarefas bônus

### Bônus 1— Integração com sistema externo de validação de CPF

A validação usa `RestClient` configurado em `RestClientConfig`, com timeout de conexão e leitura configuráveis e um toggle (`sicredi.member-validation.enabled`) para desligar a chamada externa sem precisar de mocks em teste.

A URL default (`sicredi.member-validation.base-url`) aponta para uma aplicação própria publicada na Vercel (`https://user-info-woad.vercel.app`), construída para substituir o serviço `https://user-info.herokuapp.com` citado no desafio. Esse serviço original foi descontinuado pela Heroku e não responde mais.

O resultado (ABLE/UNABLE) é aleatório, como no serviço original.

A URL é configurável via `sicredi.member-validation.base-url` (ou variável de ambiente `MEMBER_VALIDATION_URL`).

### Bônus 2— Performance

Teste de carga com k6 (`load-test.js`): 50 VUs por 2 minutos, votando concorrentemente em uma **única pauta** criada no `setup()`

### Bônus 3— Versionamento da API

Estratégia adotada: **versionamento por URI** (`/api/v1/...`). Preferido a versionamento por header por ser explícito no path, facilitar roteamento, cache e leitura de logs, e por ser o padrão adotado por APIs públicas conhecidas (Stripe, GitHub). Para uma eventual v2, o plano é criar um novo pacote `web/v2` com os novos controllers, mantendo services e domínio compartilhados entre as versões.

---

## Cobertura de testes

O relatório de cobertura é gerado pelo JaCoCo na fase `verify`:

```bash
mvn clean verify
```

O relatório HTML fica em `target/site/jacoco/index.html`.

---

## App de demonstração (pasta `app/`)

A pasta `app/` contém uma página HTML estática que simula o app mobile: consome os endpoints de tela em `/api/v1/screens` e os REST convencionais, e renderiza dinamicamente as telas FORMULARIO/SELECAO retornadas pela API, o HTML é montado a partir do JSON recebido a cada chamada.

Cobre o fluxo completo suportado pela API: listagem de pautas com indicação de status, cadastro de nova pauta, abertura de sessão de votação, votação (com confirmação de CPF) e visualização do resultado.

Para usar, abra `app/index.html` diretamente no navegador com a API em execução em `http://localhost:8080` (endereço configurável em `app.js` via `localStorage`, chave `apiBase`).

---

## CI/CD

Pipeline no **GitHub Actions** (`.github/workflows/ci.yml`), disparada em
`push` e `pull_request`.
