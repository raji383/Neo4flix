# Neo4flix Architecture

Neo4flix is organised as a set of Spring Boot microservices. Each module is an
independently runnable Maven application using Java 21, Spring Boot 4.1.2, and
Spring Cloud 2025.1.4. At present, the modules contain their application
bootstrap classes and dependency definitions; API endpoints, database models,
and inter-service calls can be added within the responsibilities below.

## Service overview

```text
Client
  |
  v
API Gateway
  |---------------------> User Service -----------+
  |---------------------> Movie Service ----------+--> Neo4j
  |---------------------> Rating Service ---------+
  `---------------------> Recommendation Service -+

All application services <----------------------> Service Registry (Eureka)
```

The diagram describes the intended relationship between modules. The current
`application.yaml` files define service names only; gateway routes, Neo4j
connection details, and Eureka server/client URLs still need configuration.

## Shared platform dependencies

All services use the Spring Boot parent for dependency and plugin version
management, are built with Maven, and target Java 21. The Spring Cloud BOM
(`spring-cloud-dependencies` version `2025.1.4`) keeps Spring Cloud libraries
compatible with one another.

| Dependency | Purpose |
| --- | --- |
| `spring-boot-starter-actuator` | Health, metrics, and operational management endpoints. |
| `spring-cloud-starter-netflix-eureka-client` | Registers a service with Eureka and lets it discover other registered services. |
| `spring-boot-starter-security` | Provides endpoint protection and the foundation for JWT authentication and authorisation. |
| `spring-boot-devtools` | Optional runtime development conveniences, such as restart support; not required in production. |
| `lombok` | Optional compile-time generation of boilerplate Java code. |
| `spring-boot-maven-plugin` | Packages and runs each application as a Spring Boot application. |

## API Gateway (`backend/api-gateway`)

The API Gateway is the public entry point for clients. Its intended job is to
route incoming requests to the appropriate internal service, so clients do not
need to know individual service locations. It can later centralise cross-cutting
concerns such as authentication, CORS, rate limiting, and request logging.

| Dependency | Why it is used here |
| --- | --- |
| `spring-cloud-starter-gateway-server-webflux` | Provides the reactive Spring Cloud Gateway runtime and route/filter support. |
| `spring-boot-starter-security` | Protects the public edge of the system. |
| `spring-cloud-starter-netflix-eureka-client` | Resolves routes through service discovery instead of fixed service addresses. |
| `spring-boot-starter-actuator` | Exposes gateway health and operational data. |
| `reactor-test` (test) | Tests reactive gateway flows. |
| `spring-boot-starter-actuator-test` (test) | Tests Actuator-related behaviour. |

## User Service (`backend/user-service`)

The User Service owns user-focused functionality: user profiles, account data,
and, when implemented, authentication and authorisation rules. Its Neo4j
integration makes it suitable for storing users and their relationships, such
as follows, favourites, or viewing activity.

| Dependency | Why it is used here |
| --- | --- |
| `spring-boot-starter-webmvc` | Builds synchronous REST APIs with Spring MVC. |
| `spring-boot-starter-data-neo4j` | Maps Java domain objects to Neo4j nodes and relationships and provides repository support. |
| `spring-boot-starter-security` | Secures HTTP endpoints and provides authentication/authorisation infrastructure. |
| `jjwt-api` | Provides the Java API used to create and parse JWTs. |
| `jjwt-impl` (runtime) | Supplies JJWT's runtime implementation. |
| `jjwt-jackson` (runtime) | Serialises JWT headers and claims with Jackson. |
| `spring-boot-starter-validation` | Validates request data using Jakarta Bean Validation annotations. |
| Actuator and Eureka client | Provides monitoring and service discovery. |
| `spring-boot-starter-*-test` (test) | Test support for Actuator, Neo4j, Security, Validation, and Spring MVC. |

## Movie Service (`backend/movie-service`)

The Movie Service is intended to own the movie catalogue: titles, descriptions,
genres, people, and relationships between catalogue entities. Neo4j is a good
fit for querying connected data such as movies by genre, cast, or similar
titles.

| Dependency | Why it is used here |
| --- | --- |
| `spring-boot-starter-webmvc` | Exposes movie catalogue REST endpoints. |
| `spring-boot-starter-data-neo4j` | Persists and queries movie graph data in Neo4j. |
| `spring-boot-starter-security` | Protects movie APIs from unauthenticated access. |
| `spring-boot-starter-validation` | Validates catalogue create and update requests. |
| Actuator and Eureka client | Provides monitoring and service discovery. |
| `spring-boot-starter-*-test` (test) | Test support for Actuator, Neo4j, Validation, and MVC. |

## Rating Service (`backend/rating-service`)

The Rating Service is intended to manage ratings or reviews that users give to
movies. Its graph data model can represent relationships such as `USER` rated
`MOVIE`, which can later be consumed by the recommendation system.

| Dependency | Why it is used here |
| --- | --- |
| `spring-boot-starter-webmvc` | Exposes REST endpoints for creating and reading ratings. |
| `spring-boot-starter-data-neo4j` | Stores rating nodes/relationships and queries graph data. |
| `spring-boot-starter-security` | Ensures only authenticated users can access rating APIs. |
| `spring-boot-starter-validation` | Validates rating input, for example a required score range. |
| Actuator and Eureka client | Provides monitoring and service discovery. |
| `spring-boot-starter-*-test` (test) | Test support for Actuator, Neo4j, Validation, and MVC. |

## Recommendation Service (`backend/recommendation-service`)

The Recommendation Service is intended to calculate or serve personalised movie
suggestions. It can use Neo4j graph queries over users, movies, genres, and
ratings to find related content. It may later call the Movie and Rating services
through Eureka-discovered endpoints if those services remain the owners of that
data.

| Dependency | Why it is used here |
| --- | --- |
| `spring-boot-starter-webmvc` | Exposes recommendation REST endpoints. |
| `spring-boot-starter-data-neo4j` | Reads and queries connected graph data for recommendation logic. |
| `spring-boot-starter-security` | Protects personalised recommendation APIs. |
| `spring-boot-starter-validation` | Validates recommendation request parameters. |
| Actuator and Eureka client | Provides monitoring and service discovery. |
| `spring-boot-starter-*-test` (test) | Test support for Actuator, Neo4j, Validation, and MVC. |

## Registry (`backend/registry`)

The Registry is intended to be the central Eureka service registry. Other
services register with it at startup and query it to locate peers without
hard-coded host names or ports.

| Dependency | Why it is used here |
| --- | --- |
| `spring-cloud-starter-netflix-eureka-server` | Runs the Eureka server that accepts registrations and serves discovery data. |
| `spring-boot-starter-actuator` | Exposes health and management endpoints. |
| `spring-boot-starter-actuator-test` (test) | Tests Actuator-related behaviour. |

The application is annotated with `@EnableEurekaServer`. It still needs its
Eureka server configuration (including its port and standalone/server settings)
before other services can register with it.

## Dependency direction

The gateway should be the client-facing entry point. Business services should
register with the registry and use its logical service names (`user-service`,
`movie-service`, `rating-service`, and `recommendation-service`) for service
discovery. Each business service should own its data and API boundary; avoid
letting one service write directly to another service's data store.
