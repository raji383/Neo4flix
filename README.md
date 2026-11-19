# Neo4flix

Neo4flix is a movie discovery and recommendation application. It has an
Angular frontend and Spring Boot microservices backed by Neo4j. Users can browse
movies, maintain personal ratings, and receive recommendations based on rating
and genre signals. Administrators manage the catalogue, and an optional TMDB
import can populate it.

## What is implemented

- **Accounts and access:** registration and login return a JWT. The API Gateway
  validates bearer tokens and forwards the authenticated user ID to downstream
  services. The frontend has login, registration, profile, and role-aware
  routes.
- **Movie catalogue:** browse, search, filter by genre and release year, view
  details, and see popular movies. Administrators can add, edit, and delete
  movies; poster uploads use Cloudinary.
- **TMDB import:** an opt-in movie-service startup seeder imports movie details,
  supported genres, director, poster, TMDB ID, and popularity. Imported movies
  are associated with the configured existing admin ID and upserted by TMDB
  ID. TMDB scores and vote counts are not used or shown.
- **Personal ratings:** the rating service creates, updates, deletes, and lists
  a user's own 1–5 ratings. Ratings are represented as person-to-movie
  relationships in Neo4j and remain separate from TMDB metadata. The frontend
  ratings page is still a placeholder.
- **Recommendations:** the recommendation service combines collaborative
  filtering, genre preferences, and the app's own movie ratings to rank
  suggestions. It returns up to ten movies.
- **Admin dashboard:** provides movie catalogue management and catalogue
  statistics.

The application is under active development; see the frontend and service
sources for the current behavior of individual screens and APIs.

## Architecture

```text
Angular frontend
      |
      v
API Gateway (:8443) ---- JWT validation / service routing
      |
      +---- User Service           (:8083)
      +---- Movie Service          (:8080)
      +---- Rating Service         (:8081)
      +---- Recommendation Service (:8082)
      |
      +---- Eureka Registry        (:8761)

All domain services use the shared Neo4j database (:7687).
```

| Component | Responsibility |
| --- | --- |
| `frontend/` | Angular single-page application |
| `backend/api-gateway/` | Public API entry point, JWT validation, and routing |
| `backend/registry/` | Eureka service discovery |
| `backend/user-service/` | Registration, login, profiles, and optional admin bootstrap |
| `backend/movie-service/` | Movie and genre APIs, Cloudinary uploads, and optional TMDB import |
| `backend/rating-service/` | Person-linked movie ratings |
| `backend/recommendation-service/` | Personalized movie recommendations |
| Neo4j | Shared graph storage |

## Requirements

- Java 21
- Docker with the Compose plugin (for local Neo4j)
- Node.js and npm (the frontend lockfile is checked in)
- TMDB API read access token and Cloudinary credentials if importing movies

## Local setup

1. Copy the example environment file and edit it:

   ```sh
   cp .env.example .env
   ```

2. Set `NEO4J_PASSWORD` and `JWT_SECRET`. `JWT_SECRET` must be a Base64-encoded
   key of at least 256 bits; for local development it can be generated with:

   ```sh
   openssl rand -base64 32
   ```

   Keep `.env` private and never commit real credentials.

3. Start Neo4j:

   ```sh
   docker compose up -d neo4j
   ```

   Neo4j Browser is available at `http://localhost:7474`; the Bolt endpoint is
   `bolt://localhost:7687`.

4. Start the backend services from the repository root:

   ```sh
   ./run-services.sh start all
   ./run-services.sh status
   ```

   The launcher starts the six Spring Boot services through their Maven
   wrappers. It does **not** start Neo4j or the frontend. Logs and PID files
   are written under `docs/logs/`.

5. Install and start the frontend:

   ```sh
   cd frontend
   npm ci
   npm start
   ```

   Open `http://localhost:4200`. The development frontend uses the API Gateway
   at `http://localhost:8443`.

To stop backend services, run `./run-services.sh stop all` from the repository
root. Individual services can be started, stopped, or checked with the same
script, for example `./run-services.sh start movie-service`.

> The current Compose configuration starts Neo4j; the other service
> definitions are commented out. Use the launcher above to run the backend
> services locally.

## Environment configuration

The root `.env.example` lists the main local settings. Important values include:

| Variable | Purpose |
| --- | --- |
| `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` | Shared Neo4j connection |
| `JWT_SECRET`, `JWT_EXPIRATION` | JWT signing key and token lifetime in milliseconds |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Movie poster uploads |
| `TMDB_ACCESS_TOKEN` | TMDB API access for optional movie import |
| `MOVIE_SEED_ENABLED` | Enables/disables movie import; defaults to `false` |
| `MOVIE_SEED_COUNT` | Number of movies to process on an enabled startup |
| `MOVIE_SEED_OWNER` | ID of the existing admin account that owns imported movies; defaults to `ADMIN_USER_ID` |

The admin account can be created separately. If it already exists, keep admin
bootstrap disabled and set `MOVIE_SEED_OWNER` (or `ADMIN_USER_ID`) to that
account's actual user ID. Admin bootstrap is optional and disabled by default;
if you enable it, also configure `ADMIN_USERNAME`, `ADMIN_EMAIL`, and a strong
`ADMIN_PASSWORD`.

### Optional TMDB import

1. Configure a valid `TMDB_ACCESS_TOKEN`, Cloudinary credentials, and the ID of
   the existing admin account.
2. Set `MOVIE_SEED_ENABLED=true` and choose `MOVIE_SEED_COUNT`.
3. Start or restart the movie service. It imports the configured number of
   qualifying movies and updates existing imports by TMDB ID.
4. Set `MOVIE_SEED_ENABLED=false` when you do not want the import to run at
   subsequent startups.

TMDB popularity is used to order imported movies in the Popular section. If no
TMDB-imported movies are available, Popular falls back to movies ranked by
Neo4flix user ratings. TMDB vote averages and vote counts are not part of the
Neo4flix rating system.

## API overview

Call the APIs through `http://localhost:8443`. Protected requests should include
the JWT returned by login:

```http
Authorization: Bearer <jwt>
```

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Log in and receive a JWT |
| `GET`, `PUT` | `/api/users/me` | Read or update the current profile |
| `GET` | `/api/movies` | Search and page through movies |
| `GET` | `/api/movies/popular` | List popular movies |
| `GET` | `/api/movies/{id}` | Get movie details |
| `POST` | `/api/movies/add` | Add a movie (admin) |
| `PUT`, `DELETE` | `/api/movies/{id}` | Update or delete a movie (admin) |
| `GET` | `/api/genres` | List available genres |
| `POST`, `PUT`, `DELETE` | `/api/ratings/{movieId}` | Create, update, or delete the current user's rating |
| `GET` | `/api/ratings/me` | List the current user's rated movies |
| `GET` | `/api/recommendations` | Get personalized recommendations |

Movie browsing accepts `q`, `page`, `size`, `genre` (repeatable), and
`releaseYear` query parameters.

## Development and tests

Build the frontend:

```sh
cd frontend
npm run build -- --configuration development
```

Run frontend tests:

```sh
cd frontend
npm test
```

Each backend service is an independent Maven project. Run its tests from the
service directory, for example:

```sh
cd backend/movie-service
./mvnw test
```

## TMDB attribution

This product uses the TMDB API but is not endorsed or certified by TMDB.