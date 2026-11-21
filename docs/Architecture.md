# Neo4flix Architecture

Neo4flix is an early-stage, microservice-oriented application. The repository
contains an Angular client, six independently runnable Spring Boot applications,
and a shared Neo4j database. Most domain behavior is not implemented yet, so
this document distinguishes the current system from the intended request flow.

## System topology

```text
Browser
  |
  v
Angular frontend
  :
  : intended HTTP API traffic (not implemented)
  v
API Gateway
  :
  : intended service routes (not configured)
  +---------------------> User Service -----------+
  +---------------------> Movie Service ----------+
  +---------------------> Rating Service ---------+--> shared Neo4j database
  `---------------------> Recommendation Service -+

Domain services -------- Eureka registration ----> Service Registry
API Gateway ------ Eureka registration/discovery -> Service Registry
```

The diagram labels target request paths explicitly. The domain services have
Eureka and Neo4j connection settings, while the gateway has a configured Eureka
client. There is currently no complete browser-to-database request path: the
frontend makes no API calls, the gateway has no routes, and the domain services
expose no controllers.

## Technology stack

| Layer | Technology |
| --- | --- |
| Client | Angular 22, TypeScript 6, RxJS, Angular SSR, Express 5 |
| Edge | Spring Cloud Gateway WebFlux |
| Services | Java 21, Spring Boot 4.1.2, Spring MVC, Maven |
| Discovery | Spring Cloud Netflix Eureka, Spring Cloud 2025.1.4 |
| Data | Spring Data Neo4j and Neo4j 5 Community |
| Tests | Vitest through the Angular builder and Spring Boot test starters |

Each backend module has its own Maven wrapper and build. There is no root Maven
aggregator, shared backend library, or shared application configuration module.

## Components

| Component | Intended responsibility | Current implementation |
| --- | --- | --- |
| `frontend` | Browser UI and calls to the public API | Angular application served as static assets by NGINX in the container; NGINX provides HTTPS and proxies `/api/` to the gateway. |
| `backend/api-gateway` | Public entry point, routing, and edge concerns | Reactive gateway bootstrap configured as a Eureka client with Security and Actuator dependencies. No routes, filters, CORS policy, or custom security configuration exist. |
| `backend/registry` | Service registration and discovery | Standalone Eureka server enabled with `@EnableEurekaServer`; client registration and registry fetching are disabled. |
| `backend/user-service` | Accounts, profiles, authentication, and authorization | Spring MVC, Security, Validation, Spring Data Neo4j, Eureka, and JJWT dependencies; no domain or API code. |
| `backend/movie-service` | Movie catalogue and catalogue relationships | Spring MVC, Security, Validation, Spring Data Neo4j, and Eureka dependencies; no domain or API code. |
| `backend/rating-service` | Ratings and reviews | Spring MVC, Security, Validation, Spring Data Neo4j, and Eureka dependencies; no domain or API code. |
| `backend/recommendation-service` | Personalized movie suggestions | Spring MVC, Security, Validation, Spring Data Neo4j, and Eureka dependencies; no recommendation or API code. |
| `neo4j` | Persistent graph storage | One Neo4j 5 Community container with a named Docker volume. |

The four domain services' Java source currently contains only their Spring Boot
application classes and context-load tests. No controllers, DTOs, entities,
repositories, Cypher queries, inter-service clients, or background jobs have
been added.

## Frontend

The Angular application is under `frontend/` and uses standalone components.
The active browser routes are:

| Path | Component |
| --- | --- |
| `/` | Redirects to `/movies` |
| `/movies` | `Movies` |
| `/login` | `Login` |
| Any other path | `Notfound` |

The repository also contains generated components for registration, ratings,
recommendations, watchlists, profiles, and layout elements, but they are not
connected to the router or root layout. The similarly named files under
`core/` and `shared/` are generated components rather than implemented guards,
interceptors, services, models, or pipes.

The Docker image serves the production Angular build through NGINX. NGINX
redirects HTTP to HTTPS, uses the frontend PEM certificate, and proxies API
requests to the gateway over HTTPS. Compose publishes the HTTP listener on
`FRONTEND_PORT` (default `4200`) and the HTTPS listener on port `443`.
Development mode uses the Angular CLI on port `4200`.

## Backend request and discovery flow

The intended request flow is:

1. The browser sends API requests to the API Gateway.
2. The gateway resolves a domain service through Eureka and forwards the
   request.
3. The domain service applies its business rules and accesses Neo4j.
4. A service calls another service through an explicit HTTP client when it
   needs behavior owned by that service.

The gateway and four domain services register against `EUREKA_URL`, which
defaults to `http://localhost:8761/eureka` for host-based development. Compose
overrides it with `http://registry:8761/eureka`. The gateway does not enable
discovery-based routes or define explicit routes. There are also no WebClient,
RestClient, or Feign clients for service-to-service communication.

Service discovery does not itself expose an API. Gateway routes and service
controllers must both exist before the target request flow is available.

## Data architecture

Docker Compose starts one Neo4j 5 Community instance and persists `/data` in
the `neo4j-data` volume. The User, Movie, Rating, and Recommendation services
all connect to the same URI and to the same database named `neo4j`.

Logical ownership is intended to follow the service boundaries: only the
owning service should mutate its data, and other services should use its API.
Because all services are configured against one physical database, this
boundary is currently a convention rather than enforced isolation.

No graph schema is defined yet. In particular, the repository contains no node
or relationship mappings, constraints, indexes, migrations, seed data, or
Graph Data Science configuration. Labels and relationships such as users,
movies, genres, ratings, and watchlists are design candidates, not current
schema.

## Runtime configuration

Backend applications optionally import `../../.env`, or the file selected by
`ENV_FILE`. The checked-in `.env.example` documents the local values:

| Process | Port setting | Example value |
| --- | --- | ---: |
| Frontend Compose HTTP host | `FRONTEND_PORT` | `4200` |
| Frontend Compose HTTPS host | Docker port mapping | `443` |
| API Gateway | `API_GATEWAY_PORT` | `8443` |
| Movie Service | `MOVIE_SERVICE_PORT` | `8080` |
| Rating Service | `RATING_SERVICE_PORT` | `8081` |
| Recommendation Service | `RECOMMENDATION_SERVICE_PORT` | `8082` |
| User Service | `USER_SERVICE_PORT` | `8083` |
| Service Registry | `REGISTRY_PORT` | `8761` |
| Angular development server | Angular CLI default | `4200` |
| Angular Express server | `PORT` | `4000` |
| Neo4j Browser | Docker port mapping | `7474` |
| Neo4j Bolt | Docker port mapping | `7687` |

The backend port placeholders do not have application-level defaults, so each
must be supplied through the environment, an environment file, or Compose. The
API Gateway serves HTTPS on port `8443` using its PKCS#12 keystore; Compose
provides its password through `API_GATEWAY_SSL_PASS`.

The domain services use `NEO4J_URI`, `NEO4J_USERNAME`, and `NEO4J_PASSWORD`.
Compose fixes the bundled database user to `neo4j` and provides a development
password fallback. `NEO4J_AUTH` only initializes credentials for a new data
volume; changing `NEO4J_PASSWORD` later does not rotate an existing database.
For disposable local data, `docker compose down --volumes` resets the database.

## Security

Security dependencies are present, but application authentication and
authorization are not implemented. The User Service includes JJWT libraries,
and the gateway reads `JWT_SECRET` and `JWT_EXPIRATION`, but there is no login
API, password handling, token issuer, JWT filter, security filter chain, route
authorization, or frontend token handling. Until custom configuration is
added, Spring Security's framework defaults apply.

Neo4j authentication is separate from application user authentication. Local
Compose defaults are for loopback-only development and must not be treated as
production secrets. Override them before using a non-local deployment.

## Deployment and operations

`docker-compose.yml` runs the frontend, registry, gateway, four domain services,
and Neo4j on one Compose network. Compose uses `registry` and `neo4j` as the
internal DNS names, waits for dependency health checks, and publishes each
process on its configurable loopback-only host port.

The backend services share a multi-stage Dockerfile selected by a `SERVICE`
build argument. Maven creates each executable jar, and the runtime image runs it
as a non-root user on Java 21. The frontend image builds the Angular application
with npm and runs the generated Express server as the Node user. The backend
applications can be managed individually or together with `run-services.sh`:

```sh
./run-services.sh start all
./run-services.sh start movie-service
./run-services.sh stop movie-service
./run-services.sh stop all
./run-services.sh status
```

The script runs the six Spring Boot applications directly through their Maven
wrappers. It does not start Neo4j or the frontend. Each backend writes to its
own append-only `docs/logs/<service>.log`; PID files are stored under
`docs/logs/.pids/`. Provide the required values in the root `.env` file.
Services that use Neo4j still require a separately running database.
The launcher requires Bash, Java 21, and `setsid` on Linux. Generated logs and
PID files are local runtime output and should not be committed.

Compose health checks provide local startup ordering. All backend applications
include Actuator, but no centralized logging, distributed tracing, metrics
export, or alerting is configured. The repository has no reverse proxy,
Kubernetes configuration, infrastructure-as-code, or CI/CD workflow.

## Test boundaries

Backend coverage consists of one Spring context-load test per application.
Frontend specs are generated component-creation tests. There are currently no
API, security, graph, contract, integration, recommendation, or browser end-to-
end tests.
