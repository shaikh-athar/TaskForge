# TaskForge Backend

The **TaskForge Backend** is a robust, multi-tenant Spring Boot service that powers the TaskForge project management platform.

It is responsible for data persistence, business logic, authentication & authorization, rate limiting, and integrations with external infrastructure such as Keycloak, Redis, and CockroachDB.

Built with modern Java and cloud-ready components, this backend is designed to be scalable, secure, and production-ready.

---

## Key Features

- Multi-tenant architecture with strict tenant isolation
- OAuth2 / OIDC authentication using Keycloak
- Role-based access control (RBAC)
- Distributed SQL database using CockroachDB
- Rate limiting with Redis + Bucket4j
- Database migrations via Flyway
- OpenAPI 3 / Swagger documentation
- Vertical Slice Architecture for clean modularity
- Production-ready Spring Security setup

---

## Tech Stack

| Technology | Purpose |
| :--- | :--- |
| **Java 21** | Core language |
| **Spring Boot 3.4.12** | Application framework |
| **Spring Data JPA** | ORM / persistence |
| **CockroachDB** | Distributed SQL database (PostgreSQL compatible) |
| **Keycloak** | Identity & access management (OIDC) |
| **Redis** | Distributed caching & rate limiting |
| **Bucket4j** | Advanced rate limiting |
| **Flyway** | Database migrations |
| **Spring Security** | OAuth2 resource server |

---

## Architecture Overview

- The backend acts as an **OAuth2 Resource Server**
- Authentication and authorization are handled by **Keycloak**
- Each request is associated with a tenant and scoped accordingly
- Data is stored in **CockroachDB** using a shared-database, tenant-isolated model
- **Redis** is used for caching and enforcing rate limits
- Features are organized using **Vertical Slice Architecture**

---

## Multi-Tenancy

TaskForge uses a **shared database, tenant-isolated** multi-tenancy model:

- Each authenticated request is associated with a tenant
- Tenant context is resolved from the JWT issued by Keycloak
- All data access is scoped to the active tenant
- Cross-tenant access is prevented at the service and persistence layer

---

## Getting Started

### Quick Start (TL;DR)

```bash
docker-compose up -d
cockroach start-single-node --insecure --background
./mvnw spring-boot:run
````

* Backend: [http://localhost:8081](http://localhost:8081)
* Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

---

## Prerequisites

Before running the application, ensure you have the following installed:

* **Java 21 JDK** (`java -version`)
* **Docker & Docker Compose**
* **CockroachDB** (binary or Docker)

---

## Infrastructure Setup

### Database: CockroachDB

#### Option A: Local Binary (Recommended)

1. Download CockroachDB from the official site.
2. Start a single-node cluster:

   ```bash
   cockroach start-single-node \
     --insecure \
     --listen-addr=localhost:26257 \
     --http-addr=localhost:9090 \
     --background
   ```
3. Create the database:

   ```bash
   cockroach sql --insecure --execute="CREATE DATABASE taskforge;"
   ```

#### Option B: Docker

```bash
docker run -d \
  --name=roach1 \
  -p 26257:26257 \
  -p 9090:8080 \
  cockroachdb/cockroach:latest-v23.1 \
  start-single-node --insecure
```

---

### Identity: Keycloak

Keycloak is used for authentication, authorization, users, and roles.

1. Start Keycloak and Redis:

   ```bash
   docker-compose up -d
   ```

2. Access the Admin Console:

   * URL: [http://localhost:10091](http://localhost:10091)
   * Username: `admin`
   * Password: `admin` (or see `docker-compose.yaml`)

3. Import Realm Configuration:

   * Go to **Create Realm**
   * Import `config/keycloak/realm-export.json`
   * This sets up:

     * `taskforge` realm
     * Clients
     * Roles

4. (Optional) Import users from `users-export.json` or create test users manually.

---

### Cache: Redis

Redis is started automatically via Docker Compose.

* Host: `localhost`
* Port: `6379`
* Used for:

  * Rate limiting
  * Caching

---

## Running the Application

1. Verify configuration in:

   ```text
   src/main/resources/application.yaml
   ```

2. Build and run:

   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

3. The server starts on **port 8081**

---

## API Documentation (Swagger)

Swagger UI is available at:

 **[http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)**

### Authentication in Swagger

1. Obtain an access token from Keycloak
2. Click **Authorize**
3. Enter:

   ```
   Bearer <your-access-token>
   ```

---

## Project Structure

```text
src/main/java/com/taskforge
├── config/          # Security, OpenAPI, & application config
├── features/        # Feature modules (vertical slices)
│   ├── issues/      # Issue management
│   ├── projects/    # Project management
│   ├── tenants/     # Multi-tenancy logic
│   └── ...
├── shared/          # Shared utilities & exceptions
└── TaskForgeApplication.java
```

---

## Security Notes

* All endpoints are secured using OAuth2 Bearer Tokens
* Tokens must be issued by the `taskforge` Keycloak realm
* Authorization checks are enforced at controller and service layers
* Rate limiting is applied per user / tenant

---

## Troubleshooting

* **Database connection refused**
  Ensure CockroachDB is running on port `26257`

* **401 Unauthorized**
  Ensure the Bearer token is valid and issued by:

  ```
  http://localhost:10091/realms/taskforge
  ```

* **Rate limit exceeded**
  Bucket4j limits may be hit — wait or restart Redis

* **Port conflicts**
  Ensure port `8081` is not already in use

---

## Development Guidelines

* Keep features isolated within their module
* Always add Flyway migrations for schema changes
* Document new endpoints in OpenAPI
* Follow standard Java and Spring Boot best practices

---

## Roadmap

* Audit logging
* Webhook support
* Advanced search & filtering
* Notification system
* Observability & metrics

---

## License

This project is proprietary and intended for internal or educational use.

---

Built with ❤️ using Java & Spring Boot
