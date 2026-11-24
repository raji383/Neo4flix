# Neo4flix Architecture

Neo4flix is a microservice-based movie discovery platform. The repository
contains an Angular 22 frontend, an HTTPS API gateway, a Eureka service registry,
four domain Spring Boot services, and Neo4j 5 as the shared graph database.

The application supports registration and JWT login, optional email 2FA,
movie search and details, ratings, watchlists, recommendations, profile
management, and account settings.

## System topology

```text
Browser
  |
  +-- HTTP 4200 / HTTPS 8444 --> Angular Frontend
                                   |
                                   | HTTPS API requests
                                   v
                              API Gateway 8443
                                   |
                                   +-- Eureka discovery --> Service Registry 8761
                                   |
                   +---------------+---------------+---------------+
                   v               v               v               v
             User Service    Movie Service   Rating Service   Recommendation
               8083            8080            8081             Service 8082
                   |               |               |               |
                   +---------------+---------------+---------------+
                                                   |
                                                   v
                                           Neo4j 5 Database
                                             7474 / 7687
```

Every service uses Eureka for registration and discovery. The gateway exposes
HTTPS routes and forwards authenticated requests to domain services. The
frontend communicates only with the gateway and never directly with domain
service ports.

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

| Component | Responsibility |
| --- | --- |
| `frontend` | Angular UI for authentication, settings, movies, ratings, recommendations, watchlists, profile, dashboard, and not-found pages. |
| `backend/api-gateway` | HTTPS public entry point, Eureka discovery, CORS, JWT validation, and trusted user identity header propagation. |
| `backend/registry` | Eureka server for service registration and discovery. |
| `backend/user-service` | Registration, login, JWT issuance, profile operations, optional 2FA, email OTP challenges, and email delivery. |
| `backend/movie-service` | Movie CRUD, search and filtering, popular movies, genre data, TMDB seeding with direct TMDB poster URLs, and Cloudinary uploads for user-provided posters. |
| `backend/rating-service` | User ratings, rating history, popularity summaries, and watchlists. |
| `backend/recommendation-service` | Hybrid genre/collaborative recommendation calculations using Neo4j rating relationships. |
| `neo4j` | Shared graph database for users, movies, genres, ratings, and watchlist relationships. |

Each domain service owns a bounded set of controllers and repositories but
shares the same physical Neo4j database. Service ownership is expressed through
application boundaries and API routes, not separate databases.

## Frontend

The Angular application uses standalone components, signals, reactive forms,
HTTP interceptors, route guards, and shared UI components.

| Path | Component | Access |
| --- | --- | --- |
| `/login` | Login with optional login OTP | Guest |
| `/register` | Account registration | Guest |
| `/movies` | Search, filters, popular movies, and watchlist actions | Authenticated |
| `/movies/:id` | Movie details and rating/watchlist actions | Authenticated |
| `/recommendations` | Personalized recommendations | Authenticated |
| `/rating` | Signed-in user's rating history | Authenticated |
| `/profile` | Profile summary and watchlist | Authenticated |
| `/settings` | Profile update and 2FA enable/disable | Authenticated |
| `/dashboard` | Movie administration and statistics | Admin |
| `/` | Redirect to `/movies` | Authenticated |
| `**` | Not-found page | Public |

An HTTP interceptor attaches the stored JWT to outgoing API requests. Route
guards prevent guest users from opening authenticated pages and non-admin users
from opening the dashboard.

The Docker image serves the production Angular build through NGINX. NGINX
redirects HTTP to HTTPS, uses the frontend PEM certificate, and proxies API
requests to the gateway over HTTPS. Compose publishes the HTTP listener on
`FRONTEND_PORT` (default `4200`) and the HTTPS listener on host port `8444`
(container port `443`).
Development mode uses the Angular CLI on port `4200`.

## Backend request and discovery flow

1. The browser sends an HTTPS API request to the gateway.
2. The gateway allows public auth routes or validates the bearer JWT for
   protected routes.
3. The gateway removes untrusted identity headers and sets
   `X-Authenticated-User-Id` and `X-Authenticated-Role` from the JWT claims.
4. Eureka resolves the target domain service and the gateway forwards the
   request through Spring Cloud Gateway.
5. The domain controller validates input and applies the service's business
   logic.
6. Neo4j repositories execute Spring Data Neo4j queries or native Cypher and
   return DTOs or domain models.

Public routes include registration, login, login OTP verification, and email
OTP verification. Profile, settings, 2FA enable/disable, movies, ratings,
watchlists, and recommendations require a valid JWT.

## Data architecture

Docker Compose starts one Neo4j 5 Community instance and persists `/data` in
the `neo4j-data` volume. The domain services connect to that database using
Spring Data Neo4j.

Primary node types include:

- `User`: username, email, password hash, role, email verification, and 2FA state.
- `Movie`: title, description, director, release year, image URL, and TMDB metadata.
- `Genre`: a movie category name.
- `EmailVerificationChallenge`: challenge ID, user ID, purpose, OTP hash, expiry,
  attempt count, and consumed state.
- `LoginChallenge`: login OTP hash, expiry, attempt count, and consumed state.

Primary relationships include:

- `RATED`: a user to a movie, with the rating value.
- `WATCHLISTED`: a user to a saved movie.
- `HAS_GENRE`: a movie to its genres.

Cypher is used for rating summaries, user rating history, watchlists, candidate
ratings, genre preferences, popularity, and hybrid recommendation calculations.

## Runtime configuration

Backend applications optionally import `../../.env`, or the file selected by
`ENV_FILE`. The checked-in `.env.example` documents the local values:

| Process | Port setting | Example value |
| --- | --- | ---: |
| Frontend Compose HTTP host | `FRONTEND_PORT` | `4200` |
| Frontend Compose HTTPS host | Docker port mapping | `8444` |
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

## Authentication and 2FA

Registration creates a user without issuing a JWT. Passwords are encoded with
BCrypt and validated against a complexity pattern. Login first validates the
password:

- If 2FA is disabled, the user-service issues a JWT directly.
- If 2FA is enabled, it creates a login challenge, sends a six-digit email OTP,
  and waits for verification before issuing the JWT.

2FA activation is separate from registration:

1. An authenticated user opens Settings and requests Enable 2FA.
2. The user-service creates a purpose-specific email verification challenge.
3. The frontend submits the six-digit code to `/api/auth/2fa/email/verify`.
4. A valid, unexpired, single-use challenge sets `emailVerified` and
   `twoFactorEnabled` on the user.
5. Disabling 2FA clears the flag and cancels pending activation challenges.

The gateway is the JWT trust boundary. It strips client-supplied identity
headers and derives the user ID and role from the signed token.

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

## Test coverage

Backend tests cover gateway JWT/header behavior, user registration/login, email
challenge verification, auth and profile services, movie search and seeding,
rating and watchlist operations, recommendation scoring and service behavior,
and Spring application contexts.

Frontend tests cover route guards, auth service requests and token storage,
movie search and popular requests, rating and watchlist services, profile and
settings behavior, shared OTP validation, component rendering, and not-found
routing.

Automated tests complement, but do not replace, live smoke testing, Neo4j graph
inspection, browser end-to-end testing, security review, and load testing.
