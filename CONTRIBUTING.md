# Contributing to Mockify

First off, thank you for considering contributing to Mockify! It's people like you that make Mockify such a great tool. We welcome contributions from everyone, whether it's reporting a bug, suggesting a feature, or writing code.

This document provides guidelines and instructions for contributing to the Mockify backend repository.

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [How Can I Contribute?](#how-can-i-contribute)
    - [Reporting Bugs](#reporting-bugs)
    - [Suggesting Enhancements](#suggesting-enhancements)
    - [Pull Requests](#pull-requests)
3. [Local Development Setup](#local-development-setup)
4. [Coding Style & Guidelines](#coding-style--guidelines)
5. [Commit Message Guidelines](#commit-message-guidelines)
6. [Getting Help](#getting-help)

---

## Code of Conduct

By participating in this project, you are expected to uphold a welcoming and inclusive environment. Please be respectful to all contributors and maintainers. Harassment or abusive behavior will not be tolerated.

---

## How Can I Contribute?

### Reporting Bugs

If you find a bug in the source code or a mistake in the documentation, you can help us by submitting an issue to our GitHub repository. Even better, you can submit a Pull Request with a fix!

When reporting an issue, please include:

- A clear and descriptive title.
- Steps to reproduce the issue.
- Expected behavior vs. actual behavior.
- Relevant logs, screenshots, or code snippets.

### Suggesting Enhancements

We welcome feature requests! If you have an idea to improve Mockify, please submit an issue detailing:

- The problem your feature solves.
- A description of the proposed solution.
- Any alternative solutions you've considered.

### Pull Requests

1. **Fork the repository** and create your branch from `main`.
2. **Create a descriptive branch name** (e.g., `feat/add-api-key-pagination` or `fix/jwt-parsing-error`).
3. If you've added code that should be tested, **add tests** (JUnit/Mockito).
4. Ensure the test suite passes:

   ```bash
   ./mvnw test
   ```

5. Ensure your code lints and follows the formatting guidelines.
6. Open a Pull Request with a clear description of the changes.

---

## Local Development Setup

The Mockify backend is a Spring Boot application built with Maven. We use Docker to manage dependencies like databases and caching layers.

### Prerequisites

- **Java:** JDK 21 or higher.
- **Maven:** Optional, as the project includes the Maven Wrapper (`./mvnw`).
- **Docker & Docker Compose:** For running PostgreSQL, Redis, and other required services locally.
- **Git:** For version control.

### Steps to Run Locally

#### 1. Clone the repository

```bash
git clone https://github.com/sadabazhar/mockify-backend.git
cd mockify-backend
```

#### 2. Set up environment variables

Copy the example environment file:

```bash
cp .env.example .env
```

Update `.env` with the appropriate database credentials, JWT secrets, and OAuth keys if necessary.

#### 3. Start infrastructure using Docker

This will start PostgreSQL, Redis, and any other required services.

```bash
docker compose up -d
```

#### 4. Build and run the application

Run the Spring Boot application using the Maven Wrapper. Flyway will automatically execute database migrations during startup.

**Windows**

```bash
mvnw.cmd spring-boot:run
```

**Linux/macOS**

```bash
./mvnw spring-boot:run
```

#### 5. Verify the application

The API should now be running at:

- `http://localhost:8080`

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

OpenAPI specification:

- `http://localhost:8080/v3/api-docs`

---

## Coding Style & Guidelines

- **Follow standard Java conventions:** Use clear, descriptive class, method, and variable names.
- **Keep it modular:** Write clean, reusable, and maintainable code. Follow the standard Spring Boot architecture:

  ```
  Controller → Service → Repository
  ```

- **Document your code:** Use Javadocs for complex methods, public APIs, and important classes.
- **Testing:** Write unit tests for services and integration tests for endpoints. We aim for high code coverage to ensure reliability.

---

## Commit Message Guidelines

We follow the Conventional Commits specification to keep our Git history clean and readable.

### Format

```text
<type>(<scope>): <subject>
```

### Examples

```text
feat(auth): implement Google OAuth2 login
fix(schema): resolve unique constraint violation on endpoints
docs(readme): update setup instructions
test(mock-record): add tests for auto-generation logic
```

### Allowed Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation-only changes |
| `style` | Formatting or whitespace changes that do not affect code behavior |
| `refactor` | Code changes that neither fix a bug nor add a feature |
| `perf` | Performance improvements |
| `test` | Adding or updating tests |
| `chore` | Build process, tooling, or maintenance tasks |

---

## Getting Help

If you have questions about contributing, setting up the project, or understanding the codebase, we're happy to help.

Before asking for help, please:

* Check the project documentation and existing GitHub issues to see if your question has already been answered.
* Make sure you're using the latest version of the project and have followed the setup instructions in this guide.

If you still need assistance:

* Open a **GitHub Discussion** for general questions and ideas.
* Open a **GitHub Issue** if you've found a bug or have a specific feature request.
* When asking for help, include relevant details such as:

    * Your operating system and Java version.
    * Steps you've already tried.
    * Error messages or stack traces.
    * Screenshots or logs, if applicable.


Providing clear and detailed information helps maintainers and contributors diagnose issues more quickly and provide accurate assistance.

---

#### Thank you once again for your interest in contributing to **Mockify**! Every contribution, no matter how small, helps make the project better.
