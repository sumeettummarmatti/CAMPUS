# CAMPUS – Project Dependencies

> This is a **Java/Maven** project. All dependencies are managed via `pom.xml` files.
> There is nothing to `pip install` — run `mvn compile` instead.

---

## Runtime Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 17+ (tested on 25) |
| Apache Maven | 3.9+ |
| Docker & Docker Compose | For local PostgreSQL databases |

---

## Backend (Spring Boot 3.2.x)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST APIs (all services) |
| `spring-boot-starter-data-jpa` | JPA / Hibernate ORM |
| `spring-boot-starter-security` | Authentication & authorization |
| `spring-boot-starter-validation` | Request DTO validation |
| `spring-boot-starter-websocket` | Real-time bidding (bidding-service) |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |
| `jjwt-api / impl / jackson` `0.12.5` | JWT token generation & validation |
| `postgresql` | PostgreSQL JDBC driver |
| `h2` | In-memory DB for tests |

---

## Frontend (JavaFX 21)

| Dependency | Version |
|---|---|
| `javafx-controls` | 21.0.2 |
| `javafx-fxml` | 21.0.2 |
| `javafx-graphics` | 21.0.2 |
| `jackson-databind` | JSON serialization |

---

## Build Plugins

| Plugin | Purpose |
|---|---|
| `maven-compiler-plugin` | Java 17 compilation |
| `spring-boot-maven-plugin` | Fat JAR packaging |
| `javafx-maven-plugin` | JavaFX application launcher |
