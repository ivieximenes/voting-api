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