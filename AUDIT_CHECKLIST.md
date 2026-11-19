# Neo4flix Audit Checklist

Audited on 2026-11-19 against the local repository and Docker stack.

- `[x]` means the item was verified in source and/or at runtime.
- `[ ] PARTIAL` means useful implementation exists, but the criterion is not fully satisfied.
- `[ ] MISSING` means no working implementation was found.
- `[ ] NOT VERIFIED` means the criterion needs evidence that this audit could not establish.

## Functional

- [x] Repository and runtime audit setup completed. The checkout has the dedicated `origin` repository, the Docker services were started, the frontend was fetched over HTTP, and the APIs were exercised with `curl` as a Postman equivalent.
- [ ] **PARTIAL** The application does not work fully cleanly end to end. All backend containers remained healthy and core API flows worked, but the frontend container reports `unhealthy` because its health check resolves `localhost` to IPv6 while Nginx only listens on IPv4 (`docker-compose.yml:37-42`, `frontend/nginx.conf:1-3`). Full browser E2E navigation was not available.

## Data And Design

- [x] The live graph was inspected through Neo4j. It contained 40 `Movie`, 17 `Genre`, and 5 `User` nodes, plus 80 `HAS_GENRE` and 30 `RATED` relationships before temporary audit data was created and removed. Neo4j Browser is exposed at `http://localhost:7474`.
- [ ] **PARTIAL** Movies, users, genres, and ratings have an appropriate basic graph shape: `(User)-[:RATED]->(Movie)-[:HAS_GENRE]->(Genre)`. Movie ownership is only a `createdBy` string, and there are no application-created uniqueness constraints or property indexes.
- [x] Relationship data is appropriate for the implemented model. `RATED` stores the required `value`; `HAS_GENRE` correctly needs no relationship property (`backend/rating-service/src/main/java/com/neo4flix/rating_service/model/Rating.java:11-21`).

Ask the auditee to run this in Neo4j Browser and explain the graph and relationship properties:

```cypher
CALL db.schema.visualization();
MATCH (u:User)-[r:RATED]->(m:Movie)-[:HAS_GENRE]->(g:Genre)
RETURN u, r, m, g
LIMIT 50;
```

## Movie Microservice

- [x] Movie reads and writes work. Runtime checks covered paging, details, popular/search data, and an admin update followed by a successful restore. Source also contains admin create and delete operations (`backend/movie-service/src/main/java/com/neo4flix/movie_service/controller/MovieController.java:34-80`).
- [ ] **MISSING** The Movie microservice does not generate recommendations. That responsibility is implemented in the separate Recommendation microservice.

## User Microservice

- [ ] **PARTIAL** Registration, login, profile read, and profile update worked at runtime. Concurrent registration is incorrect: 10 simultaneous identical requests all returned `201` and created 10 duplicate users because the database has no uniqueness constraints (`backend/user-service/src/main/java/com/neo4flix/user_service/service/AuthService.java:66-83`).
- [x] User movie ratings are stored as `RATED {value}` relationships and were verified by create, read, update, list, and delete operations.

## Rating Microservice

- [ ] **PARTIAL** Normal rating CRUD works and values outside 1-5 are rejected. Under 20 simultaneous creates, one request returned `201`, 18 returned `409`, and one incorrectly returned `404`; only one relationship was created. There are no meaningful rating-service behavior tests (`backend/rating-service/src/test/java/com/neo4flix/rating_service/RatingServiceApplicationTests.java:6-11`).

## Recommendation Microservice

- [x] Spring Data Neo4j is used by the domain services. Repositories use SDN and complex reads use `Neo4jClient` (`backend/recommendation-service/pom.xml:34-38`).
- [ ] **MISSING** Legacy Neo4j-OGM is not used. Modern Spring Data Neo4j annotations perform object mapping instead.
- [ ] **MISSING** Neo4j Graph Data Science/Graph Algorithms Library is not installed or used. The project implements custom Pearson collaborative filtering plus genre and global-rating signals in Java (`backend/recommendation-service/src/main/java/com/neo4flix/recommendation_service/service/SimilarityService.java:22-99`).
- [ ] **NOT VERIFIED** The auditee's personal understanding of Cypher requires an interview. The source demonstrates parameterized queries, aggregation, subqueries, and graph traversal, but also contains correctness and scaling issues.
- [ ] **PARTIAL** Recommendations are generated from ratings and related genre/global signals; a runtime request returned 10 unrated movies. Accuracy is not established because there are no ranking assertions, precision/recall metrics, holdout evaluation, or recommendation behavior tests. The available-movie query also multiplies `ratingCount` by genre count (`backend/recommendation-service/src/main/java/com/neo4flix/recommendation_service/repository/RecommendationRepository.java:40-57`).

## Search Functionality

- [x] Search works by title and director text, genre, and exact release year. Runtime checks found the expected title, director, and combined genre/year results (`frontend/src/app/features/movies/movies.ts:94-130`). Full release dates are not stored.

## Movie Details

- [x] The details API and page expose title, director, description, release year, genres, image, average rating, and rating count (`frontend/src/app/features/movie-details/movie-details.html:13-45`). Runtime data matched the selected movie.

## Rating Movies

- [ ] **PARTIAL** The API workflow works, but the page treats an unrated value of `0` as an existing rating and sends a failing `PUT` before retrying `POST` (`frontend/src/app/shared/components/rating/rating.ts:37-40,63-85`).

## Viewing Recommendations

- [x] Users have a protected recommendations route with loading, error, empty, and recommendation-card states. The API returned recommendation data at runtime (`frontend/src/app/features/recommendations/recommendations.html:11-79`).

## Saving Movies

- [ ] **MISSING** There is no functional watchlist API, route, or page. The component still renders `watchlist works!` (`frontend/src/app/features/watchlist/watchlist.html:1`).

## Sharing Recommendations

- [ ] **PARTIAL** Movie details provide a copy-link button, success state, and error state. There is no native/social sharing or non-Clipboard fallback (`frontend/src/app/shared/components/share-movie/share-movie.ts:15-32`).

## User-Friendly Interface

- [ ] **NOT VERIFIED** The interface has responsive layouts, accessible labels, loading/error states, and clear navigation in source, but usability is subjective and no browser E2E or user test was performed. The 404 and watchlist pages remain placeholders.

## Web Pages

- [ ] **PARTIAL** Login, registration, movie home/catalogue, movie details, profile, rating history, recommendations, and admin routes exist (`frontend/src/app/app.routes.ts:16-33`). API flows and SPA deep links worked, but no browser E2E suite verifies these pages, the first-rating flow has a defect, and the Docker frontend health check fails.

## Security

### Authentication And Authorization

- [ ] **PARTIAL** Signed JWT authentication and gateway role enforcement exist; missing, invalid, and caller-spoofed gateway credentials returned `401`, and a normal user received `403` on an admin movie update. OAuth 2.0 is not implemented. Downstream services trust identity headers directly, and an internal direct request successfully impersonated a user (`backend/user-service/src/main/java/com/neo4flix/user_service/config/SecurityConfig.java:22-24`).
- [ ] **MISSING** Two-factor authentication is not implemented.

### Password Security

- [ ] **PARTIAL** Public registration enforces 8-72 characters with uppercase, lowercase, number, and special-character requirements and stores BCrypt hashes (`backend/user-service/src/main/java/com/neo4flix/user_service/dto/RegisterRequest.java:15-20`). Admin bootstrap accepts any nonblank password, login has no rate limiting/lockout, and database uniqueness is not enforced.

### Data Encryption

- [ ] **MISSING** Production-grade transport security is not configured. The gateway is plain HTTP on port 8443, Neo4j uses plaintext `bolt://`, and the Docker Nginx frontend is HTTP-only. The Angular development certificate is self-signed, has no Subject Alternative Name, and is not a publicly valid certificate. JWTs are stored in `localStorage` and the login response is logged (`frontend/src/app/core/services/auth.service.ts:19-34`).

## Testing

- [ ] **PARTIAL** The audit exercised normal flows, malformed JSON, weak/mismatched passwords, missing/invalid JWTs, forged headers, wrong content types, invalid rating ranges/types, invalid IDs/years, pagination limits, unknown genres, Cypher-like search input, authorization, and concurrent writes. The project itself has no E2E, fuzz, contract, or load suite and very little rating/recommendation coverage.
- [ ] **PARTIAL** Many bad inputs return appropriate `400`, `401`, `403`, `404`, `409`, or `415` responses, and the Cypher-like input did not alter the 40 movies. Error handling is not fully graceful: direct identity-header impersonation succeeds, duplicate registration races create duplicate users, and one concurrent rating request was misreported as `404`.
- [ ] **PARTIAL** A bounded read-only load smoke test completed 100 movie requests at concurrency 20 and 50 recommendation requests at concurrency 10 with all responses returning `200`. This is not a production stress/soak test; the repository has no load tests, graph constraints/indexes, recommendation limits at query time, rate limiting, or resilience configuration.

## Test Evidence

- [x] Frontend production build: passed, with bundle-budget warnings.
- [ ] Frontend unit tests: failed to compile because `rating.spec.ts` imports nonexistent `Rating` and `role-guard.spec.ts` invokes the guard with the wrong type/signature.
- [x] User service: 11/11 tests passed.
- [x] Movie service: 5/5 tests passed.
- [x] Rating service: 1/1 context test passed.
- [x] Recommendation service: 1/1 context test passed.
- [x] Registry: 1/1 context test passed.
- [ ] API gateway: 3/4 tests passed; the JWT forwarding test expects misspelled header `X-Authenticated-UserID` instead of production's `X-Authenticated-User-Id` (`backend/api-gateway/src/test/java/com/neo4flix/api_gateway/ApiGatewayApplicationTests.java:104-106`).
- [x] Frontend production dependency audit: `npm audit --omit=dev` found 0 vulnerabilities.

## Priority TODO

- [ ] Add Neo4j uniqueness constraints/indexes for user IDs, usernames, emails, movie IDs, genre names, and applicable external IDs; make registration atomic.
- [ ] Authenticate service-to-service traffic instead of trusting caller-provided identity headers.
- [ ] Configure HTTPS with a trusted certificate for frontend and gateway, use encrypted Neo4j transport where applicable, and stop logging/storing bearer tokens in XSS-accessible locations.
- [ ] Fix the frontend health check to use `http://127.0.0.1:4000/` or make Nginx listen on IPv6.
- [ ] Fix the frontend and gateway tests, then add real rating/recommendation integration and browser E2E coverage.
- [ ] Fix the first-rating state and bounded retry behavior.
- [ ] Implement the watchlist and decide whether native/social recommendation sharing is required.
- [ ] Add recommendation quality evaluation and either integrate Neo4j GDS if required by the rubric or document why the custom hybrid algorithm is used.
- [ ] Add rate limiting, query-time recommendation bounds, database indexes, observability, and repeatable load/soak tests.
