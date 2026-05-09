# DockerDemoApp

A Spring Boot 4 + Java 21 REST API connected to MySQL 8, fully containerized with Docker and Docker Compose.

---

## What This App Does

- Manages a single `User` entity with a `name` field
- Exposes two REST endpoints:
  - `GET /users` — returns all saved names
  - `POST /users` — adds a new name
- Spring Boot connects to MySQL using environment variables (no hardcoded credentials)
- Both services run as Docker containers and communicate over a private Docker network

---

## Project Structure

```
DockerDemoApp/
├── src/
│   └── main/
│       ├── java/com/example/docker/
│       │   ├── controller/
│       │   │   └── UserController.java     # REST endpoints
│       │   ├── entity/
│       │   │   └── User.java               # User entity (id, name)
│       │   └── repository/
│       │       └── UserRepository.java     # JPA repository
│       └── resources/
│           └── application.yaml            # Config with env variable placeholders
├── Dockerfile                              # Builds the Spring Boot image
├── docker-compose.yml                      # Orchestrates app + mysql containers
├── names.txt                               # Flat file used by the app
├── pom.xml
└── README.md
```

---

## Prerequisites

| Tool | Version | Download |
|---|---|---|
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Java 21 (JDK) | 21+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| Git | Any | https://git-scm.com |

> **Note:** Java and Maven are only needed to build the `.jar` locally. If you use the pre-built image from Docker Hub, you only need Docker.

---

## Configuration

### `application.yaml`

The app reads all sensitive values from environment variables, so no secrets are hardcoded:

```yaml
spring:
  application:
    name: DockerDemoApp
  datasource:
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/${DB_NAME:demo_db}?useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update     # Auto-creates/updates tables on startup
    show-sql: true

server:
  port: ${SERVER_PORT:8080}

app:
  file:
    path: ./names.txt
```

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | MySQL hostname (use `mysql` inside Docker) |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `demo_db` | Database name |
| `SERVER_PORT` | `8080` | Spring Boot server port |

---

## Dockerfile

The `Dockerfile` builds the Spring Boot image from the pre-built `.jar`:

```dockerfile
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/spring-boot1.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**What each line does:**

| Line | Purpose |
|---|---|
| `FROM eclipse-temurin:21-jdk` | Base image with Java 21 JDK (Eclipse Temurin = official OpenJDK) |
| `WORKDIR /app` | Sets working directory inside the container |
| `COPY target/spring-boot1.jar app.jar` | Copies the built JAR into the image |
| `EXPOSE 8080` | Documents the port the app listens on |
| `ENTRYPOINT [...]` | Runs the JAR when the container starts |

---

## Docker Compose

`docker-compose.yml` defines two services that communicate over a shared Docker network:

```yaml
services:

  mysql:
    image: mysql:8
    container_name: mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: demo_db
    ports:
      - "3307:3306"           # Host 3307 → Container 3306 (avoids conflict with local MySQL)
    volumes:
      - mysql-data:/var/lib/mysql   # Persists DB data across container restarts
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pMilo@3005"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 30s

  app:
    image: docker-demo:6.0          # The image you build locally
    ports:
      - "9090:9090"                 # Host 9090 → Container 9090
    environment:
      SERVER_PORT: 9090
      DB_HOST: mysql                # Uses the MySQL service name — Docker DNS resolves this
      DB_PORT: 3306
      DB_NAME: demo_db
      DB_USERNAME: root
      DB_PASSWORD: "root"
    volumes:
      - names-volume:/app           # Persists names.txt across restarts
    depends_on:
      mysql:
        condition: service_healthy  # Waits for MySQL healthcheck to pass before starting

volumes:
  names-volume:
  mysql-data:
```

### How Container Communication Works

```
 ┌─────────────────────────────────────────────────┐
 │              Docker Internal Network             │
 │                                                 │
 │   ┌─────────────────┐    DB_HOST=mysql          │
 │   │   app container │ ─────────────────────►    │
 │   │  (docker-demo)  │                           │
 │   │   port 9090     │   ┌─────────────────┐     │
 │   └─────────────────┘   │ mysql container │     │
 │          │              │   port 3306     │     │
 │          │              └─────────────────┘     │
 └──────────┼──────────────────────┼───────────────┘
            │                      │
         Host 9090              Host 3307
     (your browser/API)    (MySQL Workbench etc.)
```

- Docker Compose creates a **shared virtual network** for all services automatically
- Services talk to each other using their **service name as hostname** (e.g. `DB_HOST=mysql`)
- The `depends_on: condition: service_healthy` ensures MySQL is fully ready before the app starts
- Ports are mapped to the host so you can access them from your browser or DB client

---

## How to Run

### Step 1 — Clone the repository

```bash
git clone https://github.com/<your-username>/DockerDemoApp.git
cd DockerDemoApp
```

### Step 2 — Build the JAR

```bash
./mvnw clean package -DskipTests
```

This produces `target/spring-boot1.jar`.

### Step 3 — Build the Docker image

```bash
docker build -t docker-demo:6.0 .
```

Verify it was created:

```bash
docker images
# REPOSITORY    TAG    IMAGE ID    CREATED    SIZE
# docker-demo   6.0    ...
```

### Step 4 — Start all containers

```bash
docker compose up -d
```

- `-d` runs in detached (background) mode
- Docker will start MySQL first, wait for its healthcheck, then start the app

Check that both containers are running:

```bash
docker compose ps
```

Watch the logs:

```bash
docker compose logs -f app      # Spring Boot logs
docker compose logs -f mysql    # MySQL logs
```

---

## API Usage

Base URL: `http://localhost:9090`

### Add a name

```bash
curl -X POST http://localhost:9090/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice"}'
```

Response:
```json
{ "id": 1, "name": "Alice" }
```

### Get all names

```bash
curl http://localhost:9090/users
```

Response:
```json
[
  { "id": 1, "name": "Alice" },
  { "id": 2, "name": "Bob" }
]
```

---

## Useful Docker Commands

```bash
# Stop all containers (keeps volumes/data)
docker compose down

# Stop and delete all data (volumes too)
docker compose down -v

# Rebuild image and restart everything
docker compose up -d --build

# Open a shell inside the running app container
docker exec -it <container_id> sh

# Connect to MySQL inside its container
docker exec -it mysql mysql -u root -pMilo@3005 demo_db

# View resource usage
docker stats
```

---

## Common Issues & Fixes

| Problem | Cause | Fix |
|---|---|---|
| App starts before MySQL is ready | Race condition | Already handled by `depends_on: condition: service_healthy` |
| `Connection refused` to MySQL | Wrong `DB_HOST` | Must be `mysql` (service name), not `localhost` |
| Port 3306 already in use | Local MySQL running | Compose maps to `3307` on host — use `localhost:3307` for DB clients |
| `Table 'users' doesn't exist` | First run, table not created | `ddl-auto: update` creates it automatically on startup |
| Changes not reflected after edit | Old image cached | Run `docker compose up -d --build` to rebuild |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8 |
| Containerization | Docker + Docker Compose |
| Base Image | Eclipse Temurin 21 JDK |
| Build Tool | Maven |
| IDE | Spring Tools for Eclipse |
