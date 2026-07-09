# Mockify Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![License](https://img.shields.io/badge/license-UNLICENSED-lightgrey)

> Backend service for **Mockify** — an open-source tool to create and manage mock API endpoints.

---

## 📖 Overview

Mockify Backend is the Spring Boot service that powers Mockify, a platform for spinning up and
managing mock REST API endpoints. It is built as a layered Spring Boot 3 application with
PostgreSQL for persistence, Redis for caching, Flyway for schema migrations, and JWT/OAuth2 for
authentication.

> **Note:** Endpoint-level documentation below is intentionally kept generic. This project ships
> with `springdoc-openapi`, so the authoritative, always-up-to-date list of endpoints is the live
> Swagger UI described in [API Documentation](#-api-documentation) — please treat that as the
> source of truth rather than any static list in this README.

## ✨ Features

Based on the current dependency set, the backend provides (or is architected to provide):

- **RESTful API** built with Spring Web (`spring-boot-starter-web`)
- **Caching / fast-access data** via **Redis**
- **Authentication** combining **JWT** (`jjwt`) and **OAuth2 client** login (`spring-boot-starter-oauth2-client`)
- **Email support** via `spring-boot-starter-mail`
- **Auto-generated interactive API docs** via **springdoc-openapi** (Swagger UI)
- **Health and metrics endpoints** via **Spring Boot Actuator**
- **Local dev auto-wiring** to Docker services via `spring-boot-docker-compose` and `.env` support via `dotenv-java`

> If your fork adds or removes capabilities, please update this list — it should always reflect
> what's actually implemented, not what's planned (see [Future Enhancements](#-future-enhancements) for that).

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Web | Spring Web (MVC, REST) |
| Data Access | Spring Data JPA |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Cache | Redis 7 |
| Auth | JWT (jjwt 0.13.0) + Spring Security OAuth2 Client |
| Mapping | MapStruct 1.5.5 |
| Boilerplate reduction | Lombok |
| Validation | Spring Validation + Commons Validator |
| Email | Spring Boot Starter Mail |
| API Docs | springdoc-openapi-starter-webmvc-ui 2.7.0 |
| Monitoring | Spring Boot Actuator |
| Build Tool | Maven (via Maven Wrapper `mvnw`) |
| Local Infra | Docker Compose (PostgreSQL + Redis) |
| Testing | Spring Boot Test, Spring Security Test |

## 🏗️ Architecture

The application follows a conventional **layered Spring Boot architecture**:

```
Client Request
      │
      ▼
Controller Layer   (REST endpoints, request/response DTOs)
      │
      ▼
Service Layer      (business logic)
      │
      ▼
Repository Layer   (Spring Data JPA repositories)
      │
      ▼
PostgreSQL          (persisted data, schema managed by Flyway migrations)

Redis is used alongside this flow for caching / fast-access data.
Authentication is handled via JWT (self-issued tokens) and/or OAuth2 login,
enforced through Spring Security.
```

Key architectural decisions evident from the build configuration:

- **Flyway-managed schema**: the schema is never hand-edited in production; every change ships as a migration script, so `docker compose up -d` + application start always converges to the same schema.
- **DTOs are mapped, not exposed entities**: MapStruct's presence indicates entities are not returned directly from controllers, keeping the persistence model decoupled from the API contract.
- **Dual authentication strategy**: JWT for stateless API auth and OAuth2 client support for third-party/social login flows.
- **Container-first local development**: `spring-boot-docker-compose` means running the Spring Boot app locally automatically starts/attaches to the services defined in `compose.yml` — no manual `docker compose up` step is strictly required once configured.

## 📁 Folder Structure

```
mockify-backend/
├── .mvn/wrapper/          # Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/          # Application source (controllers, services, repositories, config, security, DTOs)
│   │   └── resources/     # application.yml/properties, Flyway migration scripts (db/migration)
│   └── test/               # Unit and integration tests
├── compose.yml            # Local PostgreSQL + Redis services for development
├── pom.xml                # Maven project definition and dependencies
├── mvnw / mvnw.cmd         # Maven Wrapper scripts (no local Maven install required)
└── README.md
```

> The exact package breakdown inside `src/main/java` (e.g. `controller/`, `service/`, `repository/`,
> `entity/`, `dto/`, `mapper/`, `config/`, `security/`) should be filled in here to match your real
> package names — replace this note once confirmed against the source tree.

## ✅ Prerequisites

Make sure you have the following installed before setting up the project:

- **Java 21** (JDK)
- **Docker** and **Docker Compose** (for local PostgreSQL and Redis)
- **Git**
- Maven is **not** required separately — this project uses the Maven Wrapper (`./mvnw`)

## 🚀 Installation

1. **Fork and clone the repository**

   ```bash
   git clone https://github.com/sadabazhar/mockify-backend.git
   cd mockify-backend
   ```

2. **Create your environment file**

   Create a `.env` file in the project root (see [Environment Variables](#-environment-variables)
   for the full list).

3. **Start local infrastructure**

   ```bash
   docker compose up -d
   ```

   This starts:
    - PostgreSQL 16 on `localhost:5432` (database: `mockify`)
    - Redis 7 on `localhost:6379`

   Flyway will automatically create and migrate the schema the first time the application starts.

## ⚙️ Configuration

Configuration is driven by environment variables (loaded via `dotenv-java` from your `.env` file)
and standard Spring Boot `application.yml`/`application.properties` files under
`src/main/resources`. At minimum, the app needs database connectivity; depending on which features
you exercise (JWT auth, OAuth2 login, email, Redis), you'll also need the corresponding secrets —
see below.

## ▶️ Running the Project

With infrastructure running (see [Installation](#-installation)):

```bash
./mvnw spring-boot:run
```

Or build a jar and run it directly:

```bash
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

By default, Spring Boot applications run on **port 8080** unless overridden via
`server.port` in configuration or an environment variable.

## 📚 API Documentation

This project includes `springdoc-openapi-starter-webmvc-ui`, which auto-generates interactive API
documentation from the controllers at runtime. Once the application is running, open:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html#/` (or `/swagger-ui/index.html`)
- **Raw OpenAPI spec:** `http://localhost:8080/v3/api-docs`

Use Swagger UI as the authoritative, always-current source for available endpoints, request/response
schemas, and authentication requirements — rather than a hand-maintained list in this README, which
can drift out of date.

## 🔑 Environment Variables

| Variable | Description | Required |
|---|---|---|
| `DB_URL` | JDBC connection string, e.g. `jdbc:postgresql://localhost:5432/mockifydb` | ✅ |
| `DB_USER` | Database username | ✅ |
| `DB_PASS` | Database password | ✅ |
| `REDIS_HOST` | Redis host (e.g. `localhost`) | Recommended |
| `REDIS_PORT` | Redis port (e.g. `6379`) | Recommended |
| `JWT_SECRET` | Secret key used to sign/verify JWTs | ✅ if JWT auth is enabled |
| `JWT_EXPIRATION` | Token expiry, in ms or seconds depending on implementation | Recommended |
| `OAUTH2_CLIENT_ID` / `OAUTH2_CLIENT_SECRET` | Credentials for your configured OAuth2 provider(s) | ✅ if OAuth2 login is enabled |
| `MAIL_HOST` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP configuration for outgoing email | ✅ if email features are enabled |

> ⚠️ The variable names above marked "Recommended" or feature-dependent are inferred from the
> dependencies present in `pom.xml` (Redis, JWT, OAuth2, Mail). Please confirm the **exact**
> property names against `src/main/resources/application.yml` and update this table so it's
> 100% accurate — do not assume these names are final without checking the source.

## 🧪 Testing

The project includes `spring-boot-starter-test` and `spring-security-test` as dependencies,
enabling unit and integration testing (JUnit, Mockito, Spring Test, and security-aware test
utilities).

Run the test suite with:

```bash
./mvnw test
```

> Add details here on test coverage tooling, integration test setup (e.g. Testcontainers), and
> any CI test gates once confirmed against the actual test suite.

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository and **clone** your fork locally:

   ```bash
   git clone https://github.com/<your-username>/mockify-backend.git
   cd mockify-backend
   ```

2. **Set up local infrastructure** (see [Installation](#-installation) and
   [Configuration](#%EF%B8%8F-configuration) above).

3. **Create a feature branch** using a descriptive name:

   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Commit and push** your changes:

   ```bash
   git commit -m "feat: describe your change"
   git push origin feature/your-feature-name
   ```

5. **Open a Pull Request** from your fork's branch into `main` on the upstream repository.

6. A maintainer will review your PR and provide feedback. Once approved, it will be merged.

Please keep commit messages clear and scoped, and open an issue first for larger changes so we can
discuss the approach before you invest significant time.

## 🚧 Project Status

Active Development

## 📄 License

This project is licensed under the MIT License.

## 👤 Author

- **sadabazhar** — [GitHub Profile](https://github.com/sadabazhar)

# ⭐ Support

- If you find this project helpful, consider giving it a ⭐ on GitHub.

- It helps others discover the project and motivates future development.